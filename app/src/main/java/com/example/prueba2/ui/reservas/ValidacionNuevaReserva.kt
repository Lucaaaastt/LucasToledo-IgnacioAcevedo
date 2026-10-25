package com.example.prueba2.ui.reservas

import java.time.LocalDateTime
import java.time.LocalTime

private val patronHoraReserva = Regex("([01][0-9]|2[0-3]):[0-5][0-9]")

fun interpretarHoraReserva(texto: String): LocalTime? {
    if (!patronHoraReserva.matches(texto)) return null
    return LocalTime.of(texto.substring(0, 2).toInt(), texto.substring(3, 5).toInt())
}

data class ErroresNuevaReserva(
    val tipo: String? = null,
    val espacio: String? = null,
    val fecha: String? = null,
    val inicio: String? = null,
    val termino: String? = null,
    val motivo: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(tipo, espacio, fecha, inicio, termino, motivo).any { it != null }
}

fun validarNuevaReserva(
    estado: EstadoNuevaReserva,
    ahora: LocalDateTime = LocalDateTime.now()
): ErroresNuevaReserva {
    val horaInicio = estado.horaInicio
    val horaTermino = estado.horaTermino
    return ErroresNuevaReserva(
        tipo = if (estado.tipo == null) "Selecciona un tipo de espacio." else null,
        espacio = if (estado.tipo != null && estado.espacioSeleccionado == null) {
            "Selecciona un espacio de la categoría elegida."
        } else null,
        fecha = when {
            estado.fecha == null -> "Selecciona la fecha de la reserva."
            estado.fecha.isBefore(ahora.toLocalDate()) -> "La fecha no puede estar en el pasado."
            else -> null
        },
        inicio = when {
            estado.horaInicioTexto.isBlank() -> "Ingresa la hora de inicio."
            horaInicio == null -> "Usa HH:mm: horas de 00 a 23 y minutos de 00 a 59."
            estado.fecha == ahora.toLocalDate() && horaInicio.isBefore(ahora.toLocalTime()) ->
                "La hora de inicio no puede estar en el pasado."
            else -> null
        },
        termino = when {
            estado.horaTerminoTexto.isBlank() -> "Ingresa la hora de término."
            horaTermino == null -> "Usa HH:mm: horas de 00 a 23 y minutos de 00 a 59."
            horaInicio != null && !horaTermino.isAfter(horaInicio) ->
                "La hora de término debe ser posterior al inicio."
            else -> null
        },
        motivo = when {
            estado.motivo.isBlank() -> "Ingresa el motivo de la reserva."
            estado.motivo.length > 300 -> "El motivo admite hasta 300 caracteres."
            else -> null
        }
    )
}
