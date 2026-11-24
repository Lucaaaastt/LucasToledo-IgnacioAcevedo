package com.example.prueba2.data.repository

import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EnvioPendiente
import com.example.prueba2.data.local.EstadoReserva
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.local.ReservaConSolicitante
import com.example.prueba2.data.local.Usuario
import com.example.prueba2.data.remote.ErrorReservaRemota
import com.example.prueba2.data.remote.ReservasApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

internal class ReservasCompartidas(
    private val database: AppDatabase,
    private val api: ReservasApi = ReservasApi()
) {
    private val room = ReservaRepository(database)
    private val dao = database.respaldoDao()
    private val respaldo = RespaldoRemoto(database, TIPO_RESERVAS)
    val avisoConexion = MutableStateFlow<String?>(null)

    fun obtenerPorUsuario(usuarioId: Int): Flow<List<Reserva>> = flow {
        val usuario = obtenerUsuario(usuarioId)
        if (usuario.remotoId == null) emitAll(room.obtenerPorUsuario(usuarioId))
        else emitAll(observar(usuario, false).map { lista -> lista.map { it.reserva } })
    }

    fun obtenerPendientes(usuarioId: Int): Flow<List<ReservaConSolicitante>> = flow {
        val usuario = obtenerUsuario(usuarioId)
        require(usuario.rol == "ADMIN") { "Se requiere una cuenta de administrador." }
        if (usuario.remotoId == null) emitAll(room.obtenerPendientes(usuarioId))
        else emitAll(observar(usuario, true))
    }

    private fun observar(usuario: Usuario, administrador: Boolean): Flow<List<ReservaConSolicitante>> {
        val alcance = if (administrador) "reservas-admin" else TIPO_RESERVAS
        val locales = if (administrador) room.obtenerPendientes(usuario.id)
            else room.obtenerPorUsuario(usuario.id).map { lista -> lista.map { ReservaConSolicitante(it, usuario.nombre) } }
        return combine(locales, dao.observarCopia(usuario.id, alcance), dao.observarEnvios(usuario.id, TIPO_RESERVAS),
            actualizar(usuario, administrador, alcance)) { anteriores, copia, envios, _ ->
            val remotas = api.leerListado(usuario, copia.comoJson())
            val nuevas = if (administrador) emptyList() else envios.filter { envio ->
                envio.remotoId == null || remotas.none { it.reserva.id == -envio.remotoId }
            }.map { ReservaConSolicitante(it.aReserva(usuario.id), usuario.nombre) }
            (anteriores + remotas + nuevas).sortedWith(compareBy({ it.reserva.fecha }, { it.reserva.horaInicio }, { it.reserva.id }))
        }
    }

    private fun actualizar(usuario: Usuario, administrador: Boolean, alcance: String): Flow<Unit> = flow {
        emit(Unit)
        while (true) {
            try {
                sincronizar(usuario.id)
                val datos = api.obtenerListado(usuario, administrador)
                api.leerListado(usuario, datos) // Solo reemplazar la copia si la respuesta es válida.
                respaldo.guardarCopia(usuario.id, alcance, datos)
                avisoConexion.value = null
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                avisoConexion.value = if (error.esCaidaDeServicio())
                    "Sin conexión al servidor. Se muestra la última copia guardada; los envíos pendientes esperan conexión."
                else error.message ?: "No se pudo actualizar la copia guardada."
            }
            emit(Unit)
            delay(5_000)
        }
    }

    suspend fun enviarSolicitud(reserva: Reserva): Long {
        val usuario = obtenerUsuario(reserva.usuarioId)
        if (usuario.remotoId == null) return room.enviarSolicitud(reserva)
        require(reserva.id == 0L && reserva.horaTermino.isAfter(reserva.horaInicio)) { "El horario no es válido." }
        require(!LocalDateTime.of(reserva.fecha, reserva.horaInicio).isBefore(LocalDateTime.now(ZoneId.of("America/Santiago")))) {
            "El inicio de la reserva no puede estar en el pasado."
        }
        require(reserva.motivo.trim().length in 1..300) { "El motivo debe tener entre 1 y 300 caracteres." }
        val datos = JSONObject().put("espacioId", reserva.espacioId).put("fecha", reserva.fecha.toString())
            .put("horaInicio", reserva.horaInicio.toString()).put("horaTermino", reserva.horaTermino.toString())
            .put("motivo", reserva.motivo.trim())
        val envio = respaldo.enviarNuevo(usuario, datos) { api.crearDatos(usuario, it) }
        envio.error?.let { throw ErrorReservaRemota(mensaje = it) }
        return envio.idEnPantalla
    }

    suspend fun sincronizar(usuarioId: Int) {
        val usuario = obtenerUsuario(usuarioId)
        if (usuario.remotoId == null || usuario.rol != "SOLICITANTE") return
        respaldo.sincronizar(usuario) { datos -> api.crearDatos(usuario, datos) }
    }

    suspend fun descartarError(usuarioId: Int, idEnPantalla: Long) {
        dao.descartarError(Long.MAX_VALUE - idEnPantalla, usuarioId)
    }

    suspend fun resolverSolicitud(usuarioId: Int, reservaId: Long, estado: EstadoReserva) {
        if (reservaId > 0) room.resolverSolicitud(usuarioId, reservaId, estado)
        else api.decidir(obtenerUsuario(usuarioId), -reservaId, estado)
    }

    private suspend fun obtenerUsuario(id: Int): Usuario =
        checkNotNull(database.usuarioDao().obtenerPorId(id)) { "Vuelve a iniciar sesión." }

    private fun EnvioPendiente.aReserva(usuarioId: Int): Reserva {
        val datos = JSONObject(datos)
        return Reserva(idEnPantalla, datos.getString("espacioId"), usuarioId,
            LocalDate.parse(datos.getString("fecha")), LocalTime.parse(datos.getString("horaInicio")),
            LocalTime.parse(datos.getString("horaTermino")), datos.getString("motivo"),
            pendienteEnvio = remotoId == null && error == null, errorEnvio = error)
    }
}
