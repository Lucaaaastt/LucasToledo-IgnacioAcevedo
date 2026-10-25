package com.example.prueba2.data

import androidx.room.Dao
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

    // Incluye reservas de todos los usuarios. Horarios consecutivos no se cruzan.
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM reservas
            WHERE espacioId = :espacioId
              AND fecha = :fecha
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
