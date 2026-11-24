package com.example.prueba2.data.repository

import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EstadoReserva
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.local.ReservaConSolicitante
import com.example.prueba2.data.local.Usuario
import com.example.prueba2.data.remote.ErrorReservaRemota
import com.example.prueba2.data.remote.ReservasApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

internal class ReservasCompartidas(private val database: AppDatabase) {
    private val room = ReservaRepository(database)
    private val api = ReservasApi()
    private val recargar = MutableStateFlow(0)

    fun obtenerPorUsuario(usuarioId: Int): Flow<List<Reserva>> = flow {
        val usuario = obtenerUsuario(usuarioId)
        val locales = room.obtenerPorUsuario(usuarioId)
        if (usuario.remotoId == null) {
            emitAll(locales)
        } else {
            locales.first().takeIf { it.isNotEmpty() }?.let { emit(it) }
            emitAll(combine(locales, remotas(usuario, false)) { anteriores, compartidas ->
                (anteriores + compartidas.map { it.reserva })
                    .sortedWith(compareBy({ it.fecha }, { it.horaInicio }, { it.id }))
            })
        }
    }

    fun obtenerPendientes(usuarioId: Int): Flow<List<ReservaConSolicitante>> = flow {
        val usuario = obtenerUsuario(usuarioId)
        val locales = room.obtenerPendientes(usuarioId)
        if (usuario.remotoId == null) {
            emitAll(locales)
        } else {
            locales.first().takeIf { it.isNotEmpty() }?.let { emit(it) }
            emitAll(combine(locales, remotas(usuario, true)) { anteriores, compartidas ->
                (anteriores + compartidas).sortedWith(compareBy({ it.reserva.fecha }, { it.reserva.horaInicio }, { it.reserva.id }))
            })
        }
    }

    suspend fun enviarSolicitud(reserva: Reserva): Long {
        val usuario = obtenerUsuario(reserva.usuarioId)
        if (usuario.remotoId == null) return room.enviarSolicitud(reserva)
        return try {
            // Reservas de servidor usan signo negativo en pantalla para distinguirlas de IDs de Room.
            -api.crear(usuario, reserva)
        } catch (error: ErrorReservaRemota) {
            if (error.estado == 409) throw ReservaNoDisponibleException()
            throw error
        }
    }

    suspend fun resolverSolicitud(usuarioId: Int, reservaId: Long, estado: EstadoReserva) {
        if (reservaId > 0) {
            room.resolverSolicitud(usuarioId, reservaId, estado)
        } else {
            api.decidir(obtenerUsuario(usuarioId), -reservaId, estado)
            recargar.update { it + 1 }
        }
    }

    private fun remotas(usuario: Usuario, pendientes: Boolean) =
        combine(recargar, actualizaciones()) { _, _ -> api.listar(usuario, pendientes) }

    private suspend fun obtenerUsuario(id: Int): Usuario =
        checkNotNull(database.usuarioDao().obtenerPorId(id)) { "Vuelve a iniciar sesión." }

    private fun actualizaciones(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(5_000)
        }
    }
}
