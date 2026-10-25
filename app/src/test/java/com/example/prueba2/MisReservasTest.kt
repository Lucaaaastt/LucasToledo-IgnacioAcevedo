package com.example.prueba2

import com.example.prueba2.data.EstadoReserva
import com.example.prueba2.data.Reserva
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MisReservasTest {
    private val reserva = Reserva(
        id = 42L,
        espacioId = "LAB-2",
        usuarioId = 7,
        fecha = LocalDate.of(2026, 10, 26),
        horaInicio = LocalTime.of(10, 0),
        horaTermino = LocalTime.of(11, 0),
        motivo = "Clase de tecnología"
    )

    @Test
    fun laTarjetaConservaLosDatosYElEstadoReal() {
        val tarjeta = reserva.aResumenReserva()
        assertEquals("42", tarjeta.id)
        assertEquals("Laboratorio de Computación 2", tarjeta.espacio)
        assertEquals(reserva.fecha, tarjeta.fecha)
        assertEquals(reserva.horaInicio, tarjeta.horaInicio)
        assertEquals(reserva.horaTermino, tarjeta.horaTermino)
        assertEquals(reserva.motivo, tarjeta.motivo)
        assertEquals(EstadoReserva.PENDIENTE, tarjeta.estado)
        assertEquals(EstadoReserva.APROBADA, reserva.copy(estado = EstadoReserva.APROBADA).aResumenReserva().estado)
    }

    @Test
    fun laListaSeActualizaCuandoLaFuenteRecibeUnaReserva() = runBlocking {
        val fuente = MutableStateFlow<List<Reserva>>(emptyList())
        val viewModel = MisReservasViewModel(fuente)
        val resultados = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.estado.take(3).toList()
        }
        fuente.value = listOf(reserva)
        val estados = resultados.await()
        assertTrue(estados[0].cargando)
        assertFalse(estados[1].cargando)
        assertTrue(estados[1].reservas.isEmpty())
        assertEquals("42", estados[2].reservas.single().id)
        assertEquals(EstadoReserva.PENDIENTE, estados[2].reservas.single().estado)
    }

    @Test
    fun unErrorDeLecturaNoSePresentaComoUnaListaVaciaExitosa() = runBlocking {
        val fuente = flow<List<Reserva>> { throw IllegalStateException("Fallo simulado") }
        val estado = MisReservasViewModel(fuente).estado.toList().last()
        assertFalse(estado.cargando)
        assertNotNull(estado.error)
        assertTrue(estado.reservas.isEmpty())
    }
}
