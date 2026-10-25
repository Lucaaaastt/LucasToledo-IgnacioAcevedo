package com.example.prueba2.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "reservas",
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"]
        )
    ],
    indices = [
        Index(value = ["usuarioId"]),
        Index(value = ["espacioId", "fecha", "horaInicio"])
    ]
)
data class Reserva(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val espacioId: String,
    val usuarioId: Int,
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaTermino: LocalTime,
    val motivo: String,
    @ColumnInfo(defaultValue = "'PENDIENTE'")
    val estado: EstadoReserva = EstadoReserva.PENDIENTE
)

enum class EstadoReserva {
    PENDIENTE,
    APROBADA
}
