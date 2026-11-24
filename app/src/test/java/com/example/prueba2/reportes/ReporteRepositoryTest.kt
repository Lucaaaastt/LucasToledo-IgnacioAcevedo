package com.example.prueba2.reportes

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EstadoReporte
import com.example.prueba2.data.local.ReporteIncidencia
import com.example.prueba2.data.local.Usuario
import com.example.prueba2.data.repository.ReporteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReporteRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ReporteRepository
    private var solicitanteId: Int = 0
    private var adminId: Int = 0

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ReporteRepository(database)

        solicitanteId = database.usuarioDao().registrarUsuario(
            Usuario(
                nombre = "Ignacio",
                correo = "ignacio@duoc.cl",
                password = "Password123!",
                rol = "USUARIO"
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
    }

    @Test
    fun obtenerPorUsuario_retornaSoloReportesDelUsuarioYValidaId() = runTest {
        repository.enviarReporte(
            ReporteIncidencia(
                usuarioId = solicitanteId,
                espacioId = "SALA-1",
                descripcion = "Aire acondicionado con ruido"
            )
        )
        repository.enviarReporte(
            ReporteIncidencia(
                usuarioId = adminId,
                espacioId = "LAB-1",
                descripcion = "Monitor apagado"
            )
        )

        val reportesSolicitante = repository.obtenerPorUsuario(solicitanteId).first()
        assertEquals(1, reportesSolicitante.size)
        assertEquals("SALA-1", reportesSolicitante.first().espacioId)

        assertFailsWith<IllegalArgumentException> {
            repository.obtenerPorUsuario(0)
        }
    }

    @Test
    fun obtenerTodosConAutor_retornaListaParaAdminYBloqueaUsuarioNormal() = runTest {
        repository.enviarReporte(
            ReporteIncidencia(
                usuarioId = solicitanteId,
                espacioId = "BIBLIOTECA",
                descripcion = "Enchufe suelto"
            )
        )

        val todos = repository.obtenerTodosConAutor(adminId).first()
        assertEquals(1, todos.size)
        assertEquals("Ignacio", todos.first().autorNombre)
        assertEquals("BIBLIOTECA", todos.first().reporte.espacioId)

        assertFailsWith<IllegalArgumentException> {
            repository.obtenerTodosConAutor(solicitanteId).first()
        }
    }

    @Test
    fun enviarReporte_guardaEnEstadoPendienteLimpiaTextoYValidaCampos() = runTest {
        val idCreado = repository.enviarReporte(
            ReporteIncidencia(
                usuarioId = solicitanteId,
                espacioId = "LAB-2",
                descripcion = "  Teclado sin tecla Enter  ",
                fotoUri = "   "
            )
        )

        assertTrue(idCreado > 0L)
        val guardado = database.reporteDao().obtenerPorId(idCreado)
        assertEquals("Teclado sin tecla Enter", guardado?.descripcion)
        assertNull(guardado?.fotoUri)
        assertEquals(EstadoReporte.PENDIENTE, guardado?.estado)
        assertNull(guardado?.comentarioAdmin)

        assertFailsWith<IllegalArgumentException> {
            repository.enviarReporte(
                ReporteIncidencia(
                    usuarioId = solicitanteId,
                    espacioId = "",
                    descripcion = "Sin espacio seleccionado"
                )
            )
        }
    }

    @Test
    fun responderReporte_actualizaEstadoYComentarioSoloSiEsAdmin() = runTest {
        val reporteId = repository.enviarReporte(
            ReporteIncidencia(
                usuarioId = solicitanteId,
                espacioId = "GIMNASIO",
                descripcion = "Red de vóleibol rota"
            )
        )

        assertFailsWith<IllegalArgumentException> {
            repository.responderReporte(
                administradorId = solicitanteId,
                reporteId = reporteId,
                comentario = "Intento no autorizado"
            )
        }

        repository.responderReporte(
            administradorId = adminId,
            reporteId = reporteId,
            comentario = "  Reparación programada para mañana  "
        )

        val actualizado = database.reporteDao().obtenerPorId(reporteId)
        assertEquals(EstadoReporte.REVISADO, actualizado?.estado)
        assertEquals("Reparación programada para mañana", actualizado?.comentarioAdmin)
    }

    private suspend inline fun <reified T : Throwable> assertFailsWith(block: suspend () -> Unit) {
        try {
            block()
            fail("Se esperaba la excepción ${T::class.java.simpleName}, pero no se lanzó.")
        } catch (e: Throwable) {
            if (!T::class.java.isInstance(e)) {
                throw e
            }
        }
    }
}