package com.example.prueba2.reservas

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EstadoReserva
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.local.Usuario
import com.example.prueba2.data.repository.ReservaNoDisponibleException
import com.example.prueba2.data.repository.ReservaRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReservaRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ReservaRepository
    private var solicitanteId: Int = 0
    private var adminId: Int = 0
    private val fechaFutura: LocalDate = LocalDate.now().plusDays(1)

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ReservaRepository(database)

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
    fun obtenerPendientes_retornaSolicitudesParaAdminYBloqueaUsuarioNormal() = runTest {
        repository.enviarSolicitud(
            Reserva(
                usuarioId = solicitanteId,
                espacioId = "SALA-1",
                fecha = fechaFutura,
                horaInicio = LocalTime.of(9, 0),
                horaTermino = LocalTime.of(10, 0),
                motivo = "Repaso de certamen"
            )
        )

        val pendientes = repository.obtenerPendientes(adminId).first()
        assertEquals(1, pendientes.size)
        assertEquals("Ignacio", pendientes.first().solicitanteNombre)
        assertEquals("SALA-1", pendientes.first().reserva.espacioId)

        assertFailsWith<IllegalArgumentException> {
            repository.obtenerPendientes(solicitanteId).first()
        }
    }

    @Test
    fun obtenerPorUsuario_retornaSoloReservasDelUsuarioYValidaId() = runTest {
        repository.enviarSolicitud(
            Reserva(
                usuarioId = solicitanteId,
                espacioId = "SALA-2",
                fecha = fechaFutura,
                horaInicio = LocalTime.of(10, 0),
                horaTermino = LocalTime.of(11, 0),
                motivo = "Trabajo en equipo"
            )
        )
        repository.enviarSolicitud(
            Reserva(
                usuarioId = adminId,
                espacioId = "LAB-1",
                fecha = fechaFutura,
                horaInicio = LocalTime.of(10, 0),
                horaTermino = LocalTime.of(11, 0),
                motivo = "Mantención de equipos"
            )
        )

        val reservasUsuario = repository.obtenerPorUsuario(solicitanteId).first()
        assertEquals(1, reservasUsuario.size)
        assertEquals("SALA-2", reservasUsuario.first().espacioId)

        assertFailsWith<IllegalArgumentException> {
            repository.obtenerPorUsuario(0)
        }
    }

    @Test
    fun enviarSolicitud_registraEnPendienteValidaHorarioYBloqueaCruces() = runTest {
        val idCreada = repository.enviarSolicitud(
            Reserva(
                usuarioId = solicitanteId,
                espacioId = "BIBLIOTECA",
                fecha = fechaFutura,
                horaInicio = LocalTime.of(10, 0),
                horaTermino = LocalTime.of(11, 0),
                motivo = "  Estudio individual  "
            )
        )

        assertTrue(idCreada > 0L)
        val guardada = database.reservaDao().obtenerPorId(idCreada)
        assertEquals("Estudio individual", guardada?.motivo)
        assertEquals(EstadoReserva.PENDIENTE, guardada?.estado)

        // Bloquea horario solapado (10:30 - 11:30) en el mismo espacio y fecha
        assertFailsWith<ReservaNoDisponibleException> {
            repository.enviarSolicitud(
                Reserva(
                    usuarioId = adminId,
                    espacioId = "BIBLIOTECA",
                    fecha = fechaFutura,
                    horaInicio = LocalTime.of(10, 30),
                    horaTermino = LocalTime.of(11, 30),
                    motivo = "Cruce de horario"
                )
            )
        }

        // Permite bloque contiguo sin solape (11:00 - 12:00)
        val idContigua = repository.enviarSolicitud(
            Reserva(
                usuarioId = adminId,
                espacioId = "BIBLIOTECA",
                fecha = fechaFutura,
                horaInicio = LocalTime.of(11, 0),
                horaTermino = LocalTime.of(12, 0),
                motivo = "Bloque siguiente"
            )
        )
        assertTrue(idContigua > 0L)

        // Rechaza fecha en el pasado
        assertFailsWith<IllegalArgumentException> {
            repository.enviarSolicitud(
                Reserva(
                    usuarioId = solicitanteId,
                    espacioId = "SALA-3",
                    fecha = LocalDate.now().minusDays(1),
                    horaInicio = LocalTime.of(10, 0),
                    horaTermino = LocalTime.of(11, 0),
                    motivo = "Fecha pasada"
                )
            )
        }
    }

    @Test
    fun resolverSolicitud_apruebaORechazaSoloConAdminYEvitaDobleResolucion() = runTest {
        val reservaId = repository.enviarSolicitud(
            Reserva(
                usuarioId = solicitanteId,
                espacioId = "GIMNASIO",
                fecha = fechaFutura,
                horaInicio = LocalTime.of(15, 0),
                horaTermino = LocalTime.of(16, 0),
                motivo = "Entrenamiento selección"
            )
        )

        // Usuario normal no puede aprobar
        assertFailsWith<IllegalArgumentException> {
            repository.resolverSolicitud(solicitanteId, reservaId, EstadoReserva.APROBADA)
        }

        // Admin aprueba correctamente
        repository.resolverSolicitud(adminId, reservaId, EstadoReserva.APROBADA)
        assertEquals(EstadoReserva.APROBADA, database.reservaDao().obtenerPorId(reservaId)?.estado)

        // No permite volver a resolver una solicitud ya aprobada
        assertFailsWith<IllegalStateException> {
            repository.resolverSolicitud(adminId, reservaId, EstadoReserva.RECHAZADA)
        }

        // Si se rechaza una solicitud pendiente, el bloque horario vuelve a quedar libre
        val segundaId = repository.enviarSolicitud(
            Reserva(
                usuarioId = solicitanteId,
                espacioId = "GIMNASIO",
                fecha = fechaFutura,
                horaInicio = LocalTime.of(17, 0),
                horaTermino = LocalTime.of(18, 0),
                motivo = "Partido amistoso"
            )
        )
        repository.resolverSolicitud(adminId, segundaId, EstadoReserva.RECHAZADA)
        assertEquals(EstadoReserva.RECHAZADA, database.reservaDao().obtenerPorId(segundaId)?.estado)

        val terceraId = repository.enviarSolicitud(
            Reserva(
                usuarioId = adminId,
                espacioId = "GIMNASIO",
                fecha = fechaFutura,
                horaInicio = LocalTime.of(17, 0),
                horaTermino = LocalTime.of(18, 0),
                motivo = "Nueva reserva en horario liberado"
            )
        )
        assertTrue(terceraId > 0L)
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