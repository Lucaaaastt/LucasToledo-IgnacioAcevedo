package com.example.prueba2.reportes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EstadoReporte
import com.example.prueba2.data.local.Usuario
import com.example.prueba2.data.repository.ReporteRepository
import com.example.prueba2.data.remote.ReportesApi
import kotlinx.coroutines.runBlocking
import com.example.prueba2.ui.reportes.ReporteViewModel
import com.example.prueba2.ui.reportes.ReporteViewModelFactory
import kotlinx.coroutines.Dispatchers
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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReporteViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: ReporteRepository
    private lateinit var viewModel: ReporteViewModel
    private var solicitanteId: Int = 0
    private var adminId: Int = 0

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ReporteRepository(database, context)
        viewModel = ReporteViewModel(repository)

        solicitanteId = database.usuarioDao().registrarUsuario(
            Usuario(
                nombre = "Ignacio",
                correo = "ignacio@duoc.cl",
                password = "Password123!",
                rol = "SOLICITANTE"
            )
        ).toInt()

        adminId = database.usuarioDao().registrarUsuario(
            Usuario(
                nombre = "Admin DUOC",
                correo = "admin@duoc.cl",
                password = "Password123!",
                rol = "ADMIN"
            )
        ).toInt()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun enviarReporte_actualizaMensajeExitoYReporteEnFlujoUsuario() {
        viewModel.cargarReportesUsuario(solicitanteId)

        viewModel.enviarReporte(
            usuarioId = solicitanteId,
            espacioId = "SALA-4",
            descripcion = "Proyector sin señal HDMI",
            fotoUri = null
        )

        esperarHasta { !viewModel.cargando.value && viewModel.reportesUsuario.value.isNotEmpty() }

        assertFalse(viewModel.cargando.value)
        assertEquals("Reporte guardado en este dispositivo.", viewModel.mensajeExito.value)
        assertNull(viewModel.mensajeError.value)
        assertEquals(1, viewModel.reportesUsuario.value.size)
        assertEquals("SALA-4", viewModel.reportesUsuario.value.first().espacioId)
    }

    @Test
    fun enviarReporte_conDescripcionVacia_registraMensajeError() {
        viewModel.enviarReporte(
            usuarioId = solicitanteId,
            espacioId = "SALA-4",
            descripcion = "   ",
            fotoUri = null
        )

        esperarHasta { !viewModel.cargando.value && viewModel.mensajeError.value != null }

        assertFalse(viewModel.cargando.value)
        assertNull(viewModel.mensajeExito.value)
        assertNotNull(viewModel.mensajeError.value)
    }

    @Test
    fun cargarTodosLosReportes_cargaParaAdminYCapturaErrorParaUsuarioNormal() {
        viewModel.enviarReporte(solicitanteId, "LAB-1", "Mouse dañado", null)
        esperarHasta { !viewModel.cargando.value && viewModel.mensajeExito.value != null }
        viewModel.limpiarMensajes()

        viewModel.cargarTodosLosReportes(adminId)
        esperarHasta { viewModel.todosLosReportes.value.isNotEmpty() }
        assertEquals(1, viewModel.todosLosReportes.value.size)
        assertEquals("Ignacio", viewModel.todosLosReportes.value.first().autorNombre)

        viewModel.cargarTodosLosReportes(solicitanteId)
        esperarHasta { viewModel.mensajeError.value != null }
        assertNotNull(viewModel.mensajeError.value)
    }

    @Test
    fun responderReporte_actualizaEstadoEnFlujoAdminYLimpiarMensajesReseteaAlertas() {
        viewModel.enviarReporte(solicitanteId, "BIBLIOTECA", "Luz parpadeando", null)
        esperarHasta { !viewModel.cargando.value && viewModel.mensajeExito.value != null }

        viewModel.cargarTodosLosReportes(adminId)
        esperarHasta { viewModel.todosLosReportes.value.isNotEmpty() }
        val reporteId = viewModel.todosLosReportes.value.first().reporte.id

        viewModel.responderReporte(
            administradorId = adminId,
            reporteId = reporteId,
            comentario = "Tubo fluorescente reemplazado"
        )

        esperarHasta {
            !viewModel.cargando.value &&
                    viewModel.todosLosReportes.value.first().reporte.estado == EstadoReporte.REVISADO
        }

        assertEquals(
            "Respuesta enviada y reporte marcado como revisado.",
            viewModel.mensajeExito.value
        )
        val actualizado = viewModel.todosLosReportes.value.first().reporte
        assertEquals(EstadoReporte.REVISADO, actualizado.estado)
        assertEquals("Tubo fluorescente reemplazado", actualizado.comentarioAdmin)

        viewModel.limpiarMensajes()
        assertNull(viewModel.mensajeExito.value)
        assertNull(viewModel.mensajeError.value)
    }

    @Test
    fun reporteOfflineInformaPendienteYReintentarNoCreaOtroEnvio() = runBlocking {
        val usuario = requireNotNull(database.usuarioDao().obtenerPorId(solicitanteId))
        database.usuarioDao().actualizarUsuario(usuario.copy(remotoId = 501))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val remoto = ReporteRepository(database, ReportesApi(context, "http://127.0.0.1:9"))
        val vm = ReporteViewModel(remoto)
        val store = ViewModelStore().apply { put("reporte", vm) }
        try {
            vm.enviarReporte(solicitanteId, "SALA-4", "Ventana rota", null)
            esperarHasta { !vm.cargando.value && vm.mensajeExito.value != null }
            assertTrue(vm.mensajeExito.value!!.contains("guardado en el dispositivo"))
            val primero = database.respaldoDao().porEnviar(solicitanteId, "reportes").single()
            vm.cargarReportesUsuario(solicitanteId)
            esperarHasta { vm.avisoConexion.value != null && !vm.actualizando.value }
            val despues = database.respaldoDao().porEnviar(solicitanteId, "reportes").single()
            assertEquals(primero.clave, despues.clave)
            assertEquals(primero.id, despues.id)
            assertNull(vm.mensajeError.value)
        } finally {
            store.clear()
        }
    }

    @Test
    fun factory_creaReporteViewModelYLanzaExcepcionParaClaseDesconocida() {
        val factory = ReporteViewModelFactory(repository)
        val creado = factory.create(ReporteViewModel::class.java)
        assertNotNull(creado)

        class OtroViewModel : ViewModel()
        try {
            factory.create(OtroViewModel::class.java)
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    private fun esperarHasta(condicion: () -> Boolean) {
        val limite = System.currentTimeMillis() + 3_000L
        while (!condicion()) {
            if (System.currentTimeMillis() > limite) {
                fail("Tiempo de espera agotado esperando actualización de estado en ReporteViewModel.")
            }
            Thread.sleep(10)
        }
    }
}
