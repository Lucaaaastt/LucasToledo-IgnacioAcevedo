package com.example.prueba2.ui.reservas

import com.example.prueba2.data.catalogo.TipoEspacio
import com.example.prueba2.data.catalogo.espaciosDelTipo
import com.example.prueba2.data.local.EstadoReserva
import com.example.prueba2.data.local.Reserva
import java.time.LocalDate
import java.time.LocalTime

data class ReservaResumen(
    val id: String,
    val espacio: String,
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaTermino: LocalTime,
    val motivo: String,
    val estado: EstadoReserva = EstadoReserva.PENDIENTE,
    val solicitanteNombre: String? = null
)

internal fun Reserva.aResumenReserva(): ReservaResumen {
    val nombre = TipoEspacio.entries.asSequence()
        .flatMap { espaciosDelTipo(it).asSequence() }
        .firstOrNull { it.id == espacioId }?.nombre ?: espacioId
    return ReservaResumen(
        id = id.toString(),
        espacio = nombre,
        fecha = fecha,
        horaInicio = horaInicio,
        horaTermino = horaTermino,
        motivo = motivo,
        estado = estado
    )
}
