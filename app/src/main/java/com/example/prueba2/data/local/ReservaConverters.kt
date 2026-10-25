package com.example.prueba2.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

class ReservaConverters {
    // SQLite guarda la fecha como días desde 1970-01-01.
    @TypeConverter
    fun fechaAEntero(fecha: LocalDate): Long = fecha.toEpochDay()

    @TypeConverter
    fun enteroAFecha(dias: Long): LocalDate = LocalDate.ofEpochDay(dias)

    // SQLite guarda la hora como nanosegundos desde medianoche.
    @TypeConverter
    fun horaAEntero(hora: LocalTime): Long = hora.toNanoOfDay()

    @TypeConverter
    fun enteroAHora(nanosegundos: Long): LocalTime = LocalTime.ofNanoOfDay(nanosegundos)
}
