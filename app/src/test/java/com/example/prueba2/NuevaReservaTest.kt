package com.example.prueba2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NuevaReservaTest {
    private val ahora = LocalDateTime.of(2026, 10, 24, 10, 0)
    private val valida = EstadoNuevaReserva(
        tipo = TipoEspacio.LABORATORIO,
        espacioId = "LAB-2",
        fecha = LocalDate.of(2026, 10, 26),
        horaInicioTexto = "10:00",
        horaTerminoTexto = "11:00",
        motivo = "Clase de tecnología"
    )

    @Test
    fun formularioVacioInformaLosCamposObligatorios() {
        val errores = validarNuevaReserva(EstadoNuevaReserva(), ahora)
        assertNotNull(errores.tipo)
        assertNotNull(errores.fecha)
        assertNotNull(errores.inicio)
        assertNotNull(errores.termino)
        assertNotNull(errores.motivo)
        assertTrue(errores.hayErrores)
    }

    @Test
    fun formularioCompletoConHorarioFuturoEsValido() {
        assertFalse(validarNuevaReserva(valida, ahora).hayErrores)
    }

    @Test
    fun noAceptaEspaciosDeOtraCategoria() {
        assertNotNull(validarNuevaReserva(valida.copy(espacioId = "SALA-2"), ahora).espacio)
        assertNotNull(validarNuevaReserva(valida.copy(espacioId = null), ahora).espacio)
    }

    @Test
    fun noAceptaFechaPasadaNiHoraPasadaDelMismoDia() {
        assertNotNull(validarNuevaReserva(valida.copy(fecha = ahora.toLocalDate().minusDays(1)), ahora).fecha)
        val hoy = valida.copy(fecha = ahora.toLocalDate(), horaInicioTexto = "09:00")
        assertNotNull(validarNuevaReserva(hoy, ahora).inicio)
        assertNull(validarNuevaReserva(hoy.copy(horaInicioTexto = "10:30"), ahora).inicio)
    }

    @Test
    fun noAceptaTerminoIgualOAnteriorAlInicio() {
        assertNotNull(validarNuevaReserva(valida.copy(horaTerminoTexto = "10:00"), ahora).termino)
        assertNotNull(validarNuevaReserva(valida.copy(horaTerminoTexto = "09:00"), ahora).termino)
    }

    @Test
    fun motivoDebeContenerTextoYNoSuperarElLimite() {
        assertNotNull(validarNuevaReserva(valida.copy(motivo = "   "), ahora).motivo)
        assertNotNull(validarNuevaReserva(valida.copy(motivo = "a".repeat(301)), ahora).motivo)
        assertNull(validarNuevaReserva(valida.copy(motivo = "a".repeat(300)), ahora).motivo)
    }

    @Test
    fun catalogoTieneLosEspaciosAcordadosSinIdsDuplicados() {
        assertEquals(12, espaciosDelTipo(TipoEspacio.SALA).size)
        assertEquals(3, espaciosDelTipo(TipoEspacio.LABORATORIO).size)
        assertEquals(1, espaciosDelTipo(TipoEspacio.BIBLIOTECA).size)
        assertEquals(1, espaciosDelTipo(TipoEspacio.GIMNASIO).size)
        val ids = TipoEspacio.entries.flatMap { espaciosDelTipo(it) }.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun cambiarCategoriaDescartaElEspacioAnteriorYSeleccionaLosUnicos() {
        val viewModel = NuevaReservaViewModel()
        viewModel.seleccionarTipo(TipoEspacio.SALA)
        viewModel.seleccionarEspacio("SALA-2")
        assertEquals("SALA-2", viewModel.estado.espacioId)
        viewModel.seleccionarTipo(TipoEspacio.LABORATORIO)
        assertNull(viewModel.estado.espacioId)
        viewModel.seleccionarEspacio("SALA-2")
        assertNull(viewModel.estado.espacioId)
        viewModel.seleccionarTipo(TipoEspacio.BIBLIOTECA)
        assertEquals("BIBLIOTECA", viewModel.estado.espacioId)
        viewModel.seleccionarTipo(TipoEspacio.GIMNASIO)
        assertEquals("GIMNASIO", viewModel.estado.espacioId)
    }

    @Test
    fun interpretaHorasDe24HorasIncluyendoSusLimites() {
        assertEquals(LocalTime.MIDNIGHT, interpretarHoraReserva("00:00"))
        assertEquals(LocalTime.of(23, 59), interpretarHoraReserva("23:59"))
        assertEquals(LocalTime.of(9, 30), interpretarHoraReserva("09:30"))
        listOf("24:00", "09:60", "9:00", "09:", "0900", "aa:bb", "12:30 PM").forEach {
            assertNull(interpretarHoraReserva(it))
        }
    }

    @Test
    fun horasIncompletasOInvalidasImpidenRevisarLaReserva() {
        assertNotNull(validarNuevaReserva(valida.copy(horaInicioTexto = "09:"), ahora).inicio)
        assertNotNull(validarNuevaReserva(valida.copy(horaTerminoTexto = "24:00"), ahora).termino)
    }

    @Test
    fun alBorrarUnaHoraNoSeConservaElValorAnterior() {
        val viewModel = NuevaReservaViewModel()
        viewModel.cambiarInicio("09:30")
        assertEquals(LocalTime.of(9, 30), viewModel.estado.horaInicio)
        viewModel.cambiarInicio("09:")
        assertEquals("09:", viewModel.estado.horaInicioTexto)
        assertNull(viewModel.estado.horaInicio)
        viewModel.cambiarTermino("10:30")
        viewModel.cambiarTermino("")
        assertNull(viewModel.estado.horaTermino)
    }
}
