package com.example.prueba2.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object CalendarioReservas {
    fun abrir(
        activity: Activity,
        espacio: String,
        fecha: LocalDate,
        horaInicio: LocalTime,
        horaTermino: LocalTime,
        motivo: String
    ) {
        val zonaHoraria = ZoneId.systemDefault()
        val inicio = fecha.atTime(horaInicio).atZone(zonaHoraria).toInstant().toEpochMilli()
        val termino = fecha.atTime(horaTermino).atZone(zonaHoraria).toInstant().toEpochMilli()
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Reserva de $espacio - Marcela Paz")
            putExtra(CalendarContract.Events.EVENT_LOCATION, espacio)
            putExtra(CalendarContract.Events.DESCRIPTION, motivo)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicio)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, termino)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
        }

        // El usuario revisa y guarda el evento desde la aplicación de calendario.
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                activity,
                "No hay una aplicación de calendario compatible instalada.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
