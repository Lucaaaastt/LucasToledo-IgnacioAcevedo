package com.example.prueba2.reservas

import com.example.prueba2.data.local.EstadoReserva
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.local.ReservaConSolicitante
import com.example.prueba2.data.repository.ReservaNoDisponibleException
import com.example.prueba2.ui.reservas.MisReservasViewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import com.example.prueba2.ui.reservas.EstadoMisReservas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MisReservasViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fechaFutura: LocalDate = LocalDate.now().plusDays(2)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reintentarRecuperaElListadoSinSalirDeLaPantalla() = runTest {
        var intentos = 0
        val reserva = Reserva(id = 90, usuarioId = 2, espacioId = "SALA-1", fecha = fechaFutura,
            horaInicio = LocalTime.of(10, 0), horaTermino = LocalTime.of(11, 0), motivo = "Clase")
        val vm = MisReservasViewModel(flow {
            if (++intentos == 1) throw IllegalStateException("Sin conexión")
            emit(listOf(reserva))
        })
        var ultimo = EstadoMisReservas()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect { ultimo = it } }
        runCurrent()
        assertNotNull(ultimo.error)
        vm.reintentarCarga()
        runCurrent()
        assertEquals(2, intentos)
        assertNull(ultimo.error)
        assertEquals("90", ultimo.reservas.single().id)
    }

    @Test
    fun confirmarDosVecesNoDuplicaLaDecisionAdministrativa() = runTest {
        val confirmar = CompletableDeferred<Unit>()
        var llamadas = 0
        val reserva = Reserva(id = 90, usuarioId = 2, espacioId = "SALA-1", fecha = fechaFutura,
            horaInicio = LocalTime.of(10, 0), horaTermino = LocalTime.of(11, 0), motivo = "Clase")
        val vm = MisReservasViewModel.paraAdministrador(flowOf(listOf(ReservaConSolicitante(reserva, "Ana")))) { _, _ ->
            llamadas++
            confirmar.await()
        }
        val resumen = vm.estado.first { !it.cargando }.reservas.single()
        vm.solicitarDecision(resumen, EstadoReserva.APROBADA)
        vm.confirmarDecision()
        vm.confirmarDecision()
        assertEquals(1, llamadas)
        assertTrue(vm.estado.first { !it.cargando }.decision.guardando)
        confirmar.complete(Unit)
        assertFalse(vm.estado.first { !it.cargando }.decision.guardando)
    }

    @Test
    fun modoUsuario_mapeaReservasYNoPermiteTomarDecisionesAdmin() = runTest {
        val reserva = Reserva(
            id = 10L,
            usuarioId = 2,
            espacioId = "SALA-1",
            fecha = fechaFutura,
            horaInicio = LocalTime.of(10, 0),
            horaTermino = LocalTime.of(11, 0),
            motivo = "Reunión de proyecto",
            estado = EstadoReserva.PENDIENTE
        )

        val viewModel = MisReservasViewModel(flowOf(listOf(reserva)))
        val estadoActual = viewModel.estado.first { !it.cargando }

        assertFalse(estadoActual.cargando)
        assertNull(estadoActual.error)
        assertEquals(1, estadoActual.reservas.size)
        assertEquals("10", estadoActual.reservas.first().id)

        // Ignora acciones administrativas cuando no es modo administrador
        viewModel.solicitarDecision(estadoActual.reservas.first(), EstadoReserva.APROBADA)
        val trasIntento = viewModel.estado.first { !it.cargando }
        assertNull(trasIntento.decision.reserva)
    }

    @Test
    fun modoAdmin_apruebaYRechazaSolicitudesConMensajesDeConfirmacion() = runTest {
        val pendiente = ReservaConSolicitante(
            reserva = Reserva(
                id = 25L,
                usuarioId = 3,
                espacioId = "LAB-1",
                fecha = fechaFutura,
                horaInicio = LocalTime.of(14, 0),
                horaTermino = LocalTime.of(15, 0),
                motivo = "Laboratorio de redes",
                estado = EstadoReserva.PENDIENTE
            ),
            solicitanteNombre = "Ignacio"
        )

        var llamadaId: Long? = null
        var llamadaEstado: EstadoReserva? = null

        val viewModel = MisReservasViewModel.paraAdministrador(
            reservas = flowOf(listOf(pendiente)),
            resolverSolicitud = { id, estado ->
                llamadaId = id
                llamadaEstado = estado
            }
        )

        val inicial = viewModel.estado.first { !it.cargando }
        val resumen = inicial.reservas.first()
        assertEquals("Ignacio", resumen.solicitanteNombre)

        // Abrir y cancelar decisión
        viewModel.solicitarDecision(resumen, EstadoReserva.RECHAZADA)
        assertEquals("25", viewModel.estado.first { !it.cargando }.decision.reserva?.id)

        viewModel.cancelarDecision()
        assertNull(viewModel.estado.first { !it.cargando }.decision.reserva)

        // Solicitar y confirmar aprobación
        viewModel.solicitarDecision(resumen, EstadoReserva.APROBADA)
        viewModel.confirmarDecision()

        val final = viewModel.estado.first { !it.cargando }
        assertEquals(25L, llamadaId)
        assertEquals(EstadoReserva.APROBADA, llamadaEstado)
        assertEquals("Solicitud aprobada. El horario permanece ocupado.", final.decision.mensaje)
        assertNull(final.decision.error)
    }

    @Test
    fun modoAdmin_capturaErroresDeFlujoYConflictosAlConfirmar() = runTest {
        // Error en flujo de carga
        val vmErrorCarga = MisReservasViewModel.paraAdministrador(
            reservas = flow { throw IllegalStateException("Sin permisos") },
            resolverSolicitud = { _, _ -> }
        )
        val estadoErrorCarga = vmErrorCarga.estado.first { !it.cargando }
        assertNotNull(estadoErrorCarga.error)
        assertTrue(estadoErrorCarga.error!!.contains("No se pudieron cargar las solicitudes"))

        // Conflicto de horario al aprobar
        val pendiente = ReservaConSolicitante(
            reserva = Reserva(
                id = 30L,
                usuarioId = 3,
                espacioId = "GIMNASIO",
                fecha = fechaFutura,
                horaInicio = LocalTime.of(16, 0),
                horaTermino = LocalTime.of(17, 0),
                motivo = "Actividad deportiva",
                estado = EstadoReserva.PENDIENTE
            ),
            solicitanteNombre = "Lucas"
        )

        val vmConflicto = MisReservasViewModel.paraAdministrador(
            reservas = flowOf(listOf(pendiente)),
            resolverSolicitud = { _, _ -> throw ReservaNoDisponibleException() }
        )

        val resumen = vmConflicto.estado.first { !it.cargando }.reservas.first()
        vmConflicto.solicitarDecision(resumen, EstadoReserva.APROBADA)
        vmConflicto.confirmarDecision()

        val estadoConflicto = vmConflicto.estado.first { !it.cargando }
        assertFalse(estadoConflicto.decision.guardando)
        assertNotNull(estadoConflicto.decision.error)
        assertTrue(estadoConflicto.decision.error!!.contains("Hay otra solicitud o reserva"))
    }
}
