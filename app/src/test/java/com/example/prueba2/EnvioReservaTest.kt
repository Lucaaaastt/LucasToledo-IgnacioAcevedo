package com.example.prueba2

import com.example.prueba2.data.EstadoReserva
import com.example.prueba2.data.Reserva
import com.example.prueba2.data.ReservaNoDisponibleException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class EnvioReservaTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun cerrarScope() {
        scope.cancel()
    }

    private fun formularioCompleto(
        usuarioId: Int = 7,
        guardar: suspend (Reserva) -> Long
    ): NuevaReservaViewModel = NuevaReservaViewModel(usuarioId, guardar, scope).apply {
        seleccionarTipo(TipoEspacio.LABORATORIO)
        seleccionarEspacio("LAB-2")
        seleccionarFecha(LocalDate.now().plusDays(1))
        cambiarInicio("10:00")
        cambiarTermino("11:00")
        cambiarMotivo("  Clase de tecnología  ")
        revisar()
    }

    @Test
    fun guardaLaSolicitudPendienteConElUsuarioAutenticado() {
        var recibida: Reserva? = null
        val viewModel = formularioCompleto { reserva -> recibida = reserva; 42L }
        viewModel.enviar()
        assertEquals(7, recibida?.usuarioId)
        assertEquals("LAB-2", recibida?.espacioId)
        assertEquals("Clase de tecnología", recibida?.motivo)
        assertEquals(EstadoReserva.PENDIENTE, recibida?.estado)
        assertEquals(42L, viewModel.estado.reservaGuardadaId)
        assertFalse(viewModel.estado.guardando)
        assertFalse(viewModel.estado.mostrarResumen)
    }

    @Test
    fun pulsacionesRepetidasNoDuplicanElEnvio() {
        val respuesta = CompletableDeferred<Long>()
        var envios = 0
        val viewModel = formularioCompleto { envios++; respuesta.await() }
        viewModel.enviar()
        assertTrue(viewModel.estado.guardando)
        viewModel.enviar()
        viewModel.cambiarMotivo("Cambio durante el envío")
        assertEquals(1, envios)
        assertEquals("  Clase de tecnología  ", viewModel.estado.motivo)
        respuesta.complete(1L)
        viewModel.enviar()
        assertEquals(1, envios)
        assertEquals(1L, viewModel.estado.reservaGuardadaId)
    }

    @Test
    fun errorDeDisponibilidadConservaLosDatosYPermiteReintentar() {
        var intentos = 0
        val viewModel = formularioCompleto {
            intentos++
            if (intentos == 1) throw ReservaNoDisponibleException()
            10L
        }
        viewModel.enviar()
        assertNull(viewModel.estado.reservaGuardadaId)
        assertNotNull(viewModel.estado.errorGuardado)
        assertTrue(viewModel.estado.mostrarResumen)
        assertFalse(viewModel.estado.guardando)
        assertEquals("10:00", viewModel.estado.horaInicioTexto)
        viewModel.enviar()
        assertEquals(10L, viewModel.estado.reservaGuardadaId)
        assertNull(viewModel.estado.errorGuardado)
    }

    @Test
    fun sinUsuarioValidoNoSeInvocaElGuardado() {
        var envios = 0
        val viewModel = formularioCompleto(usuarioId = -1) { envios++; 1L }
        viewModel.enviar()
        assertEquals(0, envios)
        assertNotNull(viewModel.estado.errorGuardado)
    }

    @Test
    fun unFormularioIncompletoNoSeEnvia() {
        var envios = 0
        val viewModel = NuevaReservaViewModel(7, { envios++; 1L }, scope)
        viewModel.revisar()
        viewModel.enviar()
        assertEquals(0, envios)
        assertFalse(viewModel.estado.mostrarResumen)
    }
}
