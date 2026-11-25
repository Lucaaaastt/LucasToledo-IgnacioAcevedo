package com.example.prueba2.data.repository

import android.content.Context
import com.example.prueba2.data.remote.ErrorReporteRemoto
import com.example.prueba2.data.local.EnvioPendiente
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import org.json.JSONObject
import com.example.prueba2.data.remote.ReportesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import androidx.room.withTransaction
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EstadoReporte
import com.example.prueba2.data.local.ReporteConAutor
import com.example.prueba2.data.local.Usuario
import com.example.prueba2.data.local.ReporteIncidencia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ReporteRepository internal constructor(private val database: AppDatabase, private val api: ReportesApi) {
    constructor(database: AppDatabase, context: Context) : this(database, ReportesApi(context.applicationContext))

    val avisoConexion = MutableStateFlow<String?>(null)
    val actualizando = MutableStateFlow(false)
    private val respaldo = RespaldoRemoto(database, TIPO_REPORTES)
    private val daoRespaldo = database.respaldoDao()

    fun obtenerPorUsuario(usuarioId: Int): Flow<List<ReporteIncidencia>> = flow {
        val usuario = checkNotNull(database.usuarioDao().obtenerPorId(usuarioId)) { "Vuelve a iniciar sesión." }
        if (usuario.remotoId == null) emitAll(database.reporteDao().obtenerPorUsuario(usuarioId))
        else emitAll(observar(usuario, false).map { lista -> lista.map { it.reporte } })
    }

    fun obtenerTodosConAutor(administradorId: Int): Flow<List<ReporteConAutor>> = flow {
        val usuario = checkNotNull(database.usuarioDao().obtenerPorId(administradorId)) { "Vuelve a iniciar sesión." }
        require(usuario.rol == "ADMIN") { "Se requiere una cuenta de administrador para revisar los reportes." }
        if (usuario.remotoId == null) emitAll(database.reporteDao().obtenerTodosConAutor())
        else emitAll(observar(usuario, true))
    }

    private fun observar(usuario: Usuario, administrador: Boolean): Flow<List<ReporteConAutor>> {
        val alcance = if (administrador) "reportes-admin" else TIPO_REPORTES
        val locales = if (administrador) database.reporteDao().obtenerTodosConAutor()
            else database.reporteDao().obtenerPorUsuario(usuario.id).map { lista -> lista.map { ReporteConAutor(it, usuario.nombre) } }
        return combine(locales, daoRespaldo.observarCopia(usuario.id, alcance), daoRespaldo.observarEnvios(usuario.id, TIPO_REPORTES),
            actualizar(usuario, administrador, alcance)) { anteriores, copia, envios, _ ->
            val remotos = api.leerListado(usuario, copia.comoJson())
            val nuevos = if (administrador) emptyList() else envios.filter { envio ->
                envio.remotoId == null || remotos.none { it.reporte.id == -envio.remotoId }
            }.map { ReporteConAutor(it.aReporte(usuario.id), usuario.nombre) }
            (anteriores + remotos + nuevos).sortedWith(compareBy({ it.reporte.estado != EstadoReporte.PENDIENTE }, { -it.reporte.fechaCreacion }))
        }
    }

    private fun actualizar(usuario: Usuario, administrador: Boolean, alcance: String): Flow<Unit> = flow {
        actualizando.value = true
        try {
            emit(Unit)
            sincronizar(usuario.id)
            val datos = api.obtenerListado(usuario, administrador)
            api.leerListado(usuario, datos)
            respaldo.guardarCopia(usuario.id, alcance, datos)
            avisoConexion.value = null
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (error: Exception) {
            avisoConexion.value = error.avisoDeActualizacion()
        } finally {
            actualizando.value = false
        }
        emit(Unit)
        awaitCancellation()
    }

    suspend fun enviarReporte(reporte: ReporteIncidencia): Long {
        require(reporte.id == 0L) { "El reporte debe ser nuevo." }
        require(reporte.usuarioId > 0) { "El reporte requiere un usuario válido." }
        require(reporte.espacioId.isNotBlank()) { "Selecciona el espacio donde ocurrió la incidencia." }

        val descripcionLimpia = reporte.descripcion.trim()
        require(descripcionLimpia.isNotEmpty() && descripcionLimpia.length <= 500) {
            "La descripción debe tener entre 1 y 500 caracteres."
        }

        val fotoLimpia = reporte.fotoUri?.takeIf { it.isNotBlank() }

        val usuario = checkNotNull(database.usuarioDao().obtenerPorId(reporte.usuarioId)) { "Vuelve a iniciar sesión." }
        if (usuario.remotoId != null) {
            val datos = api.preparar(reporte.copy(descripcion = descripcionLimpia, fotoUri = fotoLimpia))
                .put("fechaCreacion", reporte.fechaCreacion)
            val envio = respaldo.enviarNuevo(usuario, datos) { api.crearDatos(usuario, it) }
            envio.error?.let { throw ErrorReporteRemoto(it) }
            return envio.idEnPantalla
        }
        return database.withTransaction {
            checkNotNull(database.usuarioDao().obtenerPorId(reporte.usuarioId)) {
                "El usuario que intenta reportar no existe."
            }
            database.reporteDao().insertarReporte(
                reporte.copy(
                    descripcion = descripcionLimpia,
                    fotoUri = fotoLimpia,
                    estado = EstadoReporte.PENDIENTE,
                    comentarioAdmin = null,
                    fechaCreacion = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun responderReporte(
        administradorId: Int,
        reporteId: Long,
        comentario: String
    ) {
        val comentarioLimpio = comentario.trim()
        require(comentarioLimpio.isNotEmpty() && comentarioLimpio.length <= 500) {
            "La respuesta debe tener entre 1 y 500 caracteres."
        }

        if (reporteId < 0) {
            val usuario = checkNotNull(database.usuarioDao().obtenerPorId(administradorId)) { "Vuelve a iniciar sesión." }
            require(usuario.rol == "ADMIN" && usuario.remotoId != null) { "Se requiere un administrador conectado al servidor." }
            api.responder(usuario, -reporteId, comentarioLimpio)
            return
        }
        database.withTransaction {
            require(database.usuarioDao().obtenerPorId(administradorId)?.rol == "ADMIN") {
                "Se requiere una cuenta de administrador para responder reportes."
            }
            val dao = database.reporteDao()
            checkNotNull(dao.obtenerPorId(reporteId)) { "El reporte ya no existe." }
            val filasActualizadas = dao.responderReporte(
                reporteId = reporteId,
                comentarioAdmin = comentarioLimpio,
                estado = EstadoReporte.REVISADO
            )
            check(filasActualizadas == 1) { "No se pudo actualizar el reporte." }
        }
    }

    suspend fun sincronizar(usuarioId: Int) {
        val usuario = checkNotNull(database.usuarioDao().obtenerPorId(usuarioId)) { "Vuelve a iniciar sesión." }
        if (usuario.remotoId == null || usuario.rol != "SOLICITANTE") return
        respaldo.sincronizar(usuario) { datos -> api.crearDatos(usuario, datos) }
    }

    suspend fun descartarError(usuarioId: Int, idEnPantalla: Long) {
        daoRespaldo.descartarError(Long.MAX_VALUE - idEnPantalla, usuarioId)
    }

    private fun EnvioPendiente.aReporte(usuarioId: Int): ReporteIncidencia {
        val datos = JSONObject(datos)
        return ReporteIncidencia(idEnPantalla, usuarioId, datos.getString("espacioId"), datos.getString("descripcion"),
            fotoUri = if (datos.isNull("fotoBase64")) null else "data:image/jpeg;base64," + datos.getString("fotoBase64"),
            fechaCreacion = datos.getLong("fechaCreacion"), pendienteEnvio = remotoId == null && error == null, errorEnvio = error)
    }
}
