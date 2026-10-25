package com.example.prueba2

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prueba2.data.AppDatabase
import com.example.prueba2.data.EstadoReserva
import com.example.prueba2.data.Reserva
import com.example.prueba2.data.ReservaNoDisponibleException
import com.example.prueba2.data.ReservaRepository
import com.example.prueba2.data.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReservaRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ReservaRepository
    private var usuarioA = 0
    private var usuarioB = 0

    @Before
    fun crearBase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = ReservaRepository(database)
        usuarioA = database.usuarioDao().registrarUsuario(
            Usuario(nombre = "Profesor A", correo = "a@example.com", password = "prueba", rol = "SOLICITANTE")
        ).toInt()
        usuarioB = database.usuarioDao().registrarUsuario(
            Usuario(nombre = "Profesor B", correo = "b@example.com", password = "prueba", rol = "SOLICITANTE")
        ).toInt()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    private fun solicitud(usuarioId: Int) = Reserva(
        espacioId = "LAB-2",
        usuarioId = usuarioId,
        fecha = LocalDate.now().plusDays(1),
        horaInicio = LocalTime.of(10, 0),
        horaTermino = LocalTime.of(11, 0),
        motivo = "  Clase de tecnología  "
    )

    @Test
    fun guardaPendienteYFiltraPorUsuario() = runBlocking {
        val listaActualizada = async(start = CoroutineStart.UNDISPATCHED) {
            repository.obtenerPorUsuario(usuarioA).first { it.isNotEmpty() }
        }
        val id = repository.enviarSolicitud(solicitud(usuarioA))
        val guardada = listaActualizada.await().single()
        assertEquals(id, guardada.id)
        assertEquals(usuarioA, guardada.usuarioId)
        assertEquals(EstadoReserva.PENDIENTE, guardada.estado)
        assertEquals("Clase de tecnología", guardada.motivo)
        assertTrue(repository.obtenerPorUsuario(usuarioB).first().isEmpty())
    }

    @Test
    fun solicitudesPendientesDeDistintosUsuariosPuedenCoincidir() = runBlocking {
        repository.enviarSolicitud(solicitud(usuarioA))
        repository.enviarSolicitud(solicitud(usuarioB))
        assertEquals(1, database.reservaDao().obtenerPorUsuario(usuarioA).first().size)
        assertEquals(1, database.reservaDao().obtenerPorUsuario(usuarioB).first().size)
    }

    @Test
    fun reservaAprobadaBloqueaCrucesPeroPermiteHorarioConsecutivo() = runBlocking {
        database.reservaDao().registrarReserva(solicitud(usuarioA).copy(estado = EstadoReserva.APROBADA))
        try {
            repository.enviarSolicitud(solicitud(usuarioB).copy(horaInicio = LocalTime.of(10, 30)))
            fail("Debió rechazar el cruce con una reserva aprobada")
        } catch (_: ReservaNoDisponibleException) {
            assertTrue(database.reservaDao().obtenerPorUsuario(usuarioB).first().isEmpty())
        }
        repository.enviarSolicitud(solicitud(usuarioB).copy(
            horaInicio = LocalTime.of(11, 0), horaTermino = LocalTime.of(12, 0)
        ))
        assertEquals(1, database.reservaDao().obtenerPorUsuario(usuarioB).first().size)
    }

    @Test
    fun elSolicitanteNoPuedeGuardarUnaSolicitudComoAprobada() = runBlocking {
        repository.enviarSolicitud(solicitud(usuarioA).copy(estado = EstadoReserva.APROBADA))
        assertEquals(EstadoReserva.PENDIENTE,
            database.reservaDao().obtenerPorUsuario(usuarioA).first().single().estado)
    }

    @Test
    fun migracionDesdeVersionDosConservaUsuariosYReservas() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nombre = "test-migracion-reservas-${UUID.randomUUID()}.db"
        var migrada: AppDatabase? = null
        try {
            context.openOrCreateDatabase(nombre, 0, null).use { anterior ->
                anterior.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, correo TEXT NOT NULL, password TEXT NOT NULL, rol TEXT NOT NULL)")
                anterior.execSQL("CREATE TABLE reservas (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, espacioId TEXT NOT NULL, usuarioId INTEGER NOT NULL, fecha INTEGER NOT NULL, horaInicio INTEGER NOT NULL, horaTermino INTEGER NOT NULL, motivo TEXT NOT NULL, FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                anterior.execSQL("CREATE INDEX index_reservas_usuarioId ON reservas (usuarioId)")
                anterior.execSQL("CREATE INDEX index_reservas_espacioId_fecha_horaInicio ON reservas (espacioId, fecha, horaInicio)")
                anterior.execSQL("INSERT INTO usuarios VALUES (1, 'Profesor anterior', 'anterior@example.com', 'prueba', 'SOLICITANTE')")
                anterior.execSQL("INSERT INTO reservas VALUES (1, 'LAB-1', 1, 20000, 36000000000000, 39600000000000, 'Registro anterior')")
                anterior.version = 2
            }
            migrada = Room.databaseBuilder(context, AppDatabase::class.java, nombre)
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
            val guardada = migrada.reservaDao().obtenerPorUsuario(1).first().single()
            assertEquals("Registro anterior", guardada.motivo)
            assertEquals(EstadoReserva.PENDIENTE, guardada.estado)
            assertNotNull(migrada.usuarioDao().obtenerPorCorreo("anterior@example.com"))
        } finally {
            migrada?.close()
            context.deleteDatabase(nombre)
        }
    }
}
