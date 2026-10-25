package com.example.prueba2.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

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

    // Solo las aprobadas bloquean el horario, independientemente del usuario.
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM reservas
            WHERE espacioId = :espacioId
              AND fecha = :fecha
              AND estado = 'APROBADA'
              AND horaInicio < :horaTermino
              AND horaTermino > :horaInicio
        )
        """
    )
    suspend fun existeCruceDeHorario(
        espacioId: String,
        fecha: LocalDate,
        horaInicio: LocalTime,
        horaTermino: LocalTime
    ): Boolean
}

data class ReservaConSolicitante(
    @Embedded val reserva: Reserva,
    val solicitanteNombre: String
)
