package com.example.prueba2.data.repository

import androidx.room.withTransaction
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EstadoReserva
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.local.ReservaConSolicitante
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ReservaNoDisponibleException : IllegalStateException(
    "El espacio ya está ocupado por una solicitud pendiente o una reserva aprobada para ese horario."
)

class ReservaRepository(private val database: AppDatabase) {
    fun obtenerPendientes(administradorId: Int): Flow<List<ReservaConSolicitante>> = flow {
        require(database.usuarioDao().obtenerPorId(administradorId)?.rol == "ADMIN") {
            "Se requiere una cuenta de administrador para consultar las solicitudes."
        }
        emitAll(database.reservaDao().obtenerPendientes())
    }

    fun obtenerPorUsuario(usuarioId: Int): Flow<List<Reserva>> {
        require(usuarioId > 0) { "Se requiere un usuario válido." }
        return database.reservaDao().obtenerPorUsuario(usuarioId)
    }

    suspend fun resolverSolicitud(administradorId: Int, reservaId: Long, estado: EstadoReserva) {
        require(estado == EstadoReserva.APROBADA || estado == EstadoReserva.RECHAZADA) {
            "La decisión debe ser aprobar o rechazar."
        }
        database.withTransaction {
            require(database.usuarioDao().obtenerPorId(administradorId)?.rol == "ADMIN") {
                "Se requiere una cuenta de administrador para resolver solicitudes."
            }
            val dao = database.reservaDao()
            val reserva = checkNotNull(dao.obtenerPorId(reservaId)) { "La solicitud ya no existe." }
            check(reserva.estado == EstadoReserva.PENDIENTE) { "La solicitud ya fue resuelta." }
            if (estado == EstadoReserva.APROBADA && dao.existeCruceDeHorario(
                    reserva.espacioId, reserva.fecha, reserva.horaInicio, reserva.horaTermino, reserva.id
                )
            ) {
                throw ReservaNoDisponibleException()
            }
            check(dao.resolverPendiente(reservaId, estado) == 1) { "La solicitud ya fue resuelta." }
        }
    }

    suspend fun enviarSolicitud(reserva: Reserva): Long {
        require(reserva.id == 0L) { "La solicitud debe ser nueva." }
        require(reserva.usuarioId > 0) { "La solicitud requiere un usuario válido." }
        require(reserva.espacioId.isNotBlank()) { "Selecciona un espacio." }
        require(reserva.horaTermino.isAfter(reserva.horaInicio)) { "El horario no es válido." }
        require(!LocalDateTime.of(reserva.fecha, reserva.horaInicio).isBefore(LocalDateTime.now())) {
            "El inicio de la reserva no puede estar en el pasado."
        }
        val motivo = reserva.motivo.trim()
        require(motivo.isNotEmpty() && motivo.length <= 300) { "El motivo no es válido." }

        return database.withTransaction {
            val dao = database.reservaDao()
            if (dao.existeCruceDeHorario(
                    reserva.espacioId, reserva.fecha, reserva.horaInicio, reserva.horaTermino
                )
            ) {
                throw ReservaNoDisponibleException()
            }
            // El solicitante no puede aprobar su propia solicitud.
            dao.registrarReserva(reserva.copy(motivo = motivo, estado = EstadoReserva.PENDIENTE))
        }
    }
}
