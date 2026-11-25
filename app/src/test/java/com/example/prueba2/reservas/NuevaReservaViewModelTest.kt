package com.example.prueba2.reservas

import com.example.prueba2.data.catalogo.TipoEspacio
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.repository.ReservaNoDisponibleException
import com.example.prueba2.ui.reservas.NuevaReservaViewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import com.example.prueba2.data.remote.ErrorReservaRemota
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class NuevaReservaViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fechaFutura: LocalDate = LocalDate.now().plusDays(1)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun seleccionarTipoYEspacio_autoseleccionaCategoriaUnicaYValidaPertenencia() {
        val viewModel = NuevaReservaViewModel(usuarioId = 5)

        // Categoría única (Biblioteca) se selecciona automáticamente
        viewModel.seleccionarTipo(TipoEspacio.BIBLIOTECA)
        assertEquals("BIBLIOTECA", viewModel.estado.espacioId)

        // Categoría múltiple (Sala) deja espacioId en null hasta elegir uno válido
        viewModel.seleccionarTipo(TipoEspacio.SALA)
        assertNull(viewModel.estado.espacioId)

        // Ignora un espacio que no pertenece a SALA
        viewModel.seleccionarEspacio("LAB-1")
        assertNull(viewModel.estado.espacioId)

        // Acepta un espacio válido de SALA
        viewModel.seleccionarEspacio("SALA-3")
        assertEquals("SALA-3", viewModel.estado.espacioId)
    }

    @Test
    fun revisarYEnviar_flujoExitoso_guardaReservaYBloqueaEdicionPosterior() = runTest {
        var reservaEnviada: Reserva? = null
        val viewModel = NuevaReservaViewModel(
            usuarioId = 12,
            enviarSolicitud = { reserva ->
                reservaEnviada = reserva
                99L
            }
        )

        // Revisar incompleto marca intentoRevisar pero no abre resumen
        viewModel.revisar()
        assertTrue(viewModel.estado.intentoRevisar)
        assertFalse(viewModel.estado.mostrarResumen)

        // Completar formulario válido
        viewModel.seleccionarTipo(TipoEspacio.LABORATORIO)
        viewModel.seleccionarEspacio("LAB-2")
        viewModel.seleccionarFecha(fechaFutura)
        viewModel.cambiarInicio("09:00")
        viewModel.cambiarTermino("11:00")
        viewModel.cambiarMotivo("  Taller de Kotlin  ")

        viewModel.revisar()
        assertTrue(viewModel.estado.mostrarResumen)

        viewModel.enviar()
        assertEquals(99L, viewModel.estado.reservaGuardadaId)
        assertFalse(viewModel.estado.mostrarResumen)
        assertFalse(viewModel.estado.guardando)
        assertNull(viewModel.estado.errorGuardado)

        assertEquals(12, reservaEnviada?.usuarioId)
        assertEquals("LAB-2", reservaEnviada?.espacioId)
        assertEquals(LocalTime.of(9, 0), reservaEnviada?.horaInicio)
        assertEquals("Taller de Kotlin", reservaEnviada?.motivo)

        // Una vez guardada, bloquea cambios posteriores
        viewModel.cambiarMotivo("Intento de edición posterior")
        assertEquals("  Taller de Kotlin  ", viewModel.estado.motivo)
    }

    @Test
    fun enviar_sinUsuarioValidoOConCruceDeHorario_registraMensajeDeError() = runTest {
        // Caso 1: usuarioId <= 0
        val vmSinUsuario = NuevaReservaViewModel(usuarioId = 0)
        completarFormularioValido(vmSinUsuario)
        vmSinUsuario.revisar()
        vmSinUsuario.enviar()
        assertEquals(
            "No se pudo identificar tu usuario. Vuelve a iniciar sesión.",
            vmSinUsuario.estado.errorGuardado
        )

        // Caso 2: cruce de horario (ReservaNoDisponibleException)
        val vmCruce = NuevaReservaViewModel(
            usuarioId = 4,
            enviarSolicitud = { throw ReservaNoDisponibleException() }
        )
        completarFormularioValido(vmCruce)
        vmCruce.revisar()
        vmCruce.enviar()
        assertNotNull(vmCruce.estado.errorGuardado)
        assertTrue(vmCruce.estado.errorGuardado!!.contains("ya está ocupado"))
        assertNull(vmCruce.estado.reservaGuardadaId)
        assertFalse(vmCruce.estado.guardando)
    }

    @Test
    fun doblePulsacionDuranteEnvioSoloGuardaUnaVez() = runTest {
        val confirmar = CompletableDeferred<Long>()
        var llamadas = 0
        val vm = NuevaReservaViewModel(usuarioId = 4, enviarSolicitud = {
            llamadas++
            confirmar.await()
        })
        completarFormularioValido(vm)
        vm.revisar()
        vm.enviar()
        vm.enviar()
        assertEquals(1, llamadas)
        assertTrue(vm.estado.guardando)
        vm.cambiarMotivo("No debe reemplazar lo que se está enviando")
        assertEquals("Entrenamiento deportivo", vm.estado.motivo)
        confirmar.complete(-19L)
        assertFalse(vm.estado.guardando)
        assertEquals(-19L, vm.estado.reservaGuardadaId)
    }

    @Test
    fun falloDeConexionConservaFormularioYPermiteReintentar() = runTest {
        var llamadas = 0
        val vm = NuevaReservaViewModel(usuarioId = 4, enviarSolicitud = {
            if (++llamadas == 1) throw ErrorReservaRemota(mensaje = "Servidor temporalmente inaccesible", servicioInaccesible = true)
            -20L
        })
        completarFormularioValido(vm)
        vm.revisar()
        vm.enviar()
        assertFalse(vm.estado.guardando)
        assertTrue(vm.estado.mostrarResumen)
        assertEquals("Entrenamiento deportivo", vm.estado.motivo)
        assertEquals("Servidor temporalmente inaccesible", vm.estado.errorGuardado)
        vm.enviar()
        assertEquals(2, llamadas)
        assertEquals(-20L, vm.estado.reservaGuardadaId)
        assertNull(vm.estado.errorGuardado)
    }

    @Test
    fun envioOfflineSeDistingueDeUnaSolicitudConfirmada() = runTest {
        val vm = NuevaReservaViewModel(usuarioId = 4, enviarSolicitud = { Long.MAX_VALUE - 1 })
        completarFormularioValido(vm)
        vm.revisar()
        vm.enviar()
        assertTrue(vm.estado.pendienteEnvio)
        assertFalse(vm.estado.guardando)
    }

    private fun completarFormularioValido(viewModel: NuevaReservaViewModel) {
        viewModel.seleccionarTipo(TipoEspacio.GIMNASIO)
        viewModel.seleccionarFecha(fechaFutura)
        viewModel.cambiarInicio("15:00")
        viewModel.cambiarTermino("16:30")
        viewModel.cambiarMotivo("Entrenamiento deportivo")
    }
}
