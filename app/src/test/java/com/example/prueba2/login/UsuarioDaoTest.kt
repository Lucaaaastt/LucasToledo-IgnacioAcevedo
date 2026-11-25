package com.example.prueba2.login

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.Usuario
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UsuarioDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun registrarYObtenerPorId_guardaYRecuperaUsuarioConSuRol() = runTest {
        val dao = database.usuarioDao()
        val idCreado = dao.registrarUsuario(
            Usuario(
                nombre = "Ignacio Acevedo",
                correo = "ignacio.acevedo@gmail.com",
                password = "Password123!",
                rol = "SOLICITANTE"
            )
        ).toInt()

        val guardado = dao.obtenerPorId(idCreado)
        assertNotNull(guardado)
        assertEquals("Ignacio Acevedo", guardado?.nombre)
        assertEquals("SOLICITANTE", guardado?.rol)
        assertNull(guardado?.remotoId)
    }

    @Test
    fun login_validaCredencialesExactasYRetornaNullSiNoCoinciden() = runTest {
        val dao = database.usuarioDao()
        dao.registrarUsuario(
            Usuario(
                nombre = "Admin Marcela Paz",
                correo = "admin.marcelapaz@gmail.com",
                password = "ClaveSegura99",
                rol = "ADMIN"
            )
        )

        val autenticado = dao.login("admin.marcelapaz@gmail.com", "ClaveSegura99")
        assertNotNull(autenticado)
        assertEquals("ADMIN", autenticado?.rol)

        assertNull(dao.login("admin.marcelapaz@gmail.com", "ClaveIncorrecta"))
        assertNull(dao.login("inexistente@gmail.com", "ClaveSegura99"))
    }

    @Test
    fun cuentaLocalSinVincularYActualizar_vinculaIdRemotoCorrectamente() = runTest {
        val dao = database.usuarioDao()
        val idLocal = dao.registrarUsuario(
            Usuario(
                nombre = "Lucas Toledo",
                correo = "lucas.toledo.dev@gmail.com",
                password = "Password123!",
                rol = "SOLICITANTE"
            )
        ).toInt()

        val sinVincular = dao.obtenerCuentaLocalSinVincular("lucas.toledo.dev@gmail.com", "Password123!")
        assertNotNull(sinVincular)

        val filasActualizadas = dao.actualizarUsuario(sinVincular!!.copy(remotoId = 501))
        assertEquals(1, filasActualizadas)

        assertEquals(idLocal, dao.obtenerPorIdRemoto(501)?.id)
        assertNull(dao.obtenerCuentaLocalSinVincular("lucas.toledo.dev@gmail.com", "Password123!"))
    }
}