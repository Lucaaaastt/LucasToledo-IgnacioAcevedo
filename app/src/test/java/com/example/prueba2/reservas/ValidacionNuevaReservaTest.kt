package com.example.prueba2.reservas

import com.example.prueba2.data.catalogo.TipoEspacio
import com.example.prueba2.ui.reservas.EstadoNuevaReserva
import com.example.prueba2.ui.reservas.interpretarHoraReserva
import com.example.prueba2.ui.reservas.validarNuevaReserva
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionNuevaReservaTest {

    private val ahoraFijo: LocalDateTime = LocalDateTime.of(2026, 11, 24, 10, 0)

    @Test
    fun interpretarHoraReserva_validaFormatoHHmmYRechazaInvalidos() {
        assertEquals(LocalTime.of(0, 0), interpretarHoraReserva("00:00"))
        assertEquals(LocalTime.of(9, 45), interpretarHoraReserva("09:45"))
        assertEquals(LocalTime.of(23, 59), interpretarHoraReserva("23:59"))

        assertNull(interpretarHoraReserva(""))
        assertNull(interpretarHoraReserva("9:45"))
        assertNull(interpretarHoraReserva("24:00"))
        assertNull(interpretarHoraReserva("12:60"))
        assertNull(interpretarHoraReserva("10:00:00"))
    }

    @Test
    fun validarNuevaReserva_estadoVacio_reportaTodosLosErroresObligatorios() {
        val errores = validarNuevaReserva(EstadoNuevaReserva(), ahora = ahoraFijo)

        assertTrue(errores.hayErrores)
        assertEquals("Selecciona un tipo de espacio.", errores.tipo)
        assertNull(errores.espacio)
        assertEquals("Selecciona la fecha de la reserva.", errores.fecha)
        assertEquals("Ingresa la hora de inicio.", errores.inicio)
        assertEquals("Ingresa la hora de término.", errores.termino)
        assertEquals("Ingresa el motivo de la reserva.", errores.motivo)
    }

    @Test
    fun validarNuevaReserva_detectaFechaPasadaHorasInvertidasYMotivoLargo() {
        val estadoInvalido = EstadoNuevaReserva(
            tipo = TipoEspacio.SALA,
            espacioId = null,
            fecha = ahoraFijo.toLocalDate().minusDays(1),
            horaInicioTexto = "14:00",
            horaTerminoTexto = "13:00",
            motivo = "a".repeat(301)
        )

        val errores = validarNuevaReserva(estadoInvalido, ahora = ahoraFijo)

        assertTrue(errores.hayErrores)
        assertEquals("Selecciona un espacio de la categoría elegida.", errores.espacio)
        assertEquals("La fecha no puede estar en el pasado.", errores.fecha)
        assertNull(errores.inicio)
        assertEquals("La hora de término debe ser posterior al inicio.", errores.termino)
        assertEquals("El motivo admite hasta 300 caracteres.", errores.motivo)
    }

    @Test
    fun validarNuevaReserva_hoyConHoraPasada_rechazaInicioEnElPasado() {
        val estadoHoyPasado = EstadoNuevaReserva(
            tipo = TipoEspacio.BIBLIOTECA,
            espacioId = "BIBLIOTECA",
            fecha = ahoraFijo.toLocalDate(),
            horaInicioTexto = "09:30",
            horaTerminoTexto = "11:00",
            motivo = "Estudio previo a evaluación"
        )

        val errores = validarNuevaReserva(estadoHoyPasado, ahora = ahoraFijo)

        assertTrue(errores.hayErrores)
        assertEquals("La hora de inicio no puede estar en el pasado.", errores.inicio)
    }

    @Test
    fun validarNuevaReserva_datosValidos_noTieneErrores() {
        val estadoValido = EstadoNuevaReserva(
            tipo = TipoEspacio.LABORATORIO,
            espacioId = "LAB-1",
            fecha = ahoraFijo.toLocalDate().plusDays(1),
            horaInicioTexto = "10:00",
            horaTerminoTexto = "12:00",
            motivo = "Práctica de desarrollo móvil"
        )

        val errores = validarNuevaReserva(estadoValido, ahora = ahoraFijo)

        assertFalse(errores.hayErrores)
        assertNull(errores.tipo)
        assertNull(errores.espacio)
        assertNull(errores.fecha)
        assertNull(errores.inicio)
        assertNull(errores.termino)
        assertNull(errores.motivo)
    }
}