package com.example.prueba2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservaDao {
    @Insert
    suspend fun registrarReserva(reserva: Reserva): Long

    @Query("SELECT * FROM reservas WHERE usuarioId = :usuarioId ORDER BY fecha ASC, horaInicio ASC, id ASC")
    fun obtenerPorUsuario(usuarioId: Int): Flow<List<Reserva>>

    @Query(
        """
        SELECT reservas.*, usuarios.nombre AS solicitanteNombre
        FROM reservas
        INNER JOIN usuarios ON usuarios.id = reservas.usuarioId
        WHERE reservas.estado = 'PENDIENTE'
        ORDER BY reservas.fecha ASC, reservas.horaInicio ASC, reservas.id ASC
        """
    )
    fun obtenerPendientes(): Flow<List<ReservaConSolicitante>>

    @Query("SELECT * FROM reservas WHERE id = :reservaId LIMIT 1")
    suspend fun obtenerPorId(reservaId: Long): Reserva?

    @Query("UPDATE reservas SET estado = :estado WHERE id = :reservaId AND estado = 'PENDIENTE'")
    suspend fun resolverPendiente(reservaId: Long, estado: EstadoReserva): Int

    // Las solicitudes pendientes y aprobadas bloquean el horario para todos los usuarios.
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM reservas
            WHERE espacioId = :espacioId
              AND fecha = :fecha
              AND id != :reservaIdExcluir
              AND estado IN ('PENDIENTE', 'APROBADA')
              AND horaInicio < :horaTermino
              AND horaTermino > :horaInicio
        )
        """
    )
    suspend fun existeCruceDeHorario(
        espacioId: String,
        fecha: LocalDate,
        horaInicio: LocalTime,
        horaTermino: LocalTime,
        reservaIdExcluir: Long = 0
    ): Boolean
}
