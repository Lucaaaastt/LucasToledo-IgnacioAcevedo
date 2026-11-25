package com.example.prueba2.data.repository

import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prueba2.data.local.*
import com.example.prueba2.data.remote.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RespaldoOfflineTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val usuario = Usuario(1, "Ana", "ana.offline@gmail.com", "demo1234", "SOLICITANTE", 51)
    private val reservaJson = """[{"id":31,"espacioId":"SALA-1","fecha":"2099-01-01","horaInicio":"10:00","horaTermino":"11:00","motivo":"Clase","estado":"APROBADA","solicitanteNombre":"Ana"}]"""
    private fun database() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Test
    fun consultaReservasYReportesGuardadosAunqueFalleElServicio() = runBlocking {
        val db = database()
        try {
            db.usuarioDao().registrarUsuario(usuario)
            RespaldoRemoto(db, TIPO_RESERVAS).guardarCopia(1, TIPO_RESERVAS, reservaJson)
            val reportes = """[{"id":42,"espacioId":"SALA-1","descripcion":"Ventana","fotoBase64":"/9j/AA==","estado":"REVISADO","comentarioAdmin":"Reparada","fechaCreacion":12,"autorNombre":"Ana"}]"""
            RespaldoRemoto(db, TIPO_REPORTES).guardarCopia(1, TIPO_REPORTES, reportes)
            val reservas = ReservasCompartidas(db, ReservasApi("http://127.0.0.1:9"))
            val repositorio = ReporteRepository(db, ReportesApi(context, "http://127.0.0.1:9"))
            val trabajo = launch { reservas.obtenerPorUsuario(1).collect {} }
            val trabajoReportes = launch { repositorio.obtenerPorUsuario(1).collect {} }
            try {
                withTimeout(5_000) { reservas.avisoConexion.first { it != null } }
                withTimeout(5_000) { repositorio.avisoConexion.first { it != null } }
                val copia = withTimeout(5_000) { reservas.obtenerPorUsuario(1).first { it.isNotEmpty() } }.single()
                assertEquals(EstadoReserva.APROBADA, copia.estado)
                val reporte = withTimeout(5_000) { repositorio.obtenerPorUsuario(1).first { it.isNotEmpty() } }.single()
                assertEquals("Reparada", reporte.comentarioAdmin)
                assertTrue(reporte.fotoUri!!.startsWith("data:image/jpeg;base64,"))
            } finally { trabajo.cancelAndJoin(); trabajoReportes.cancelAndJoin() }
        } finally { db.close() }
    }

    @Test
    fun guardaSolicitudesYReportesNuevosSinConexionComoPendientesDeEnvio() = runBlocking {
        val db = database()
        try {
            db.usuarioDao().registrarUsuario(usuario)
            val reservas = ReservasCompartidas(db, ReservasApi("http://127.0.0.1:9"))
            val reservaId = reservas.enviarSolicitud(Reserva(espacioId = "SALA-1", usuarioId = 1,
                fecha = java.time.LocalDate.of(2099, 1, 1), horaInicio = java.time.LocalTime.of(10, 0),
                horaTermino = java.time.LocalTime.of(11, 0), motivo = "Clase"))
            assertTrue(reservaId > Long.MAX_VALUE / 2)
            val reserva = withTimeout(5_000) { reservas.obtenerPorUsuario(1).first { it.isNotEmpty() } }.single()
            assertTrue(reserva.pendienteEnvio)
            assertEquals(EstadoReserva.PENDIENTE, reserva.estado)
            val reportes = ReporteRepository(db, ReportesApi(context, "http://127.0.0.1:9"))
            val reporteId = reportes.enviarReporte(ReporteIncidencia(usuarioId = 1, espacioId = "SALA-1", descripcion = "Ventana"))
            assertTrue(reporteId > Long.MAX_VALUE / 2)
            val reporte = withTimeout(5_000) { reportes.obtenerPorUsuario(1).first { it.isNotEmpty() } }.single()
            assertTrue(reporte.pendienteEnvio)
        } finally { db.close() }
    }

    @Test
    fun conservaVariasFotosGrandesYNoMezclaCopiasDeCuentasOAdministracion() = runBlocking {
        val db = database()
        try {
            val foto = "/9j/" + "A".repeat(900_000)
            val datos = org.json.JSONArray()
            for (id in 1..3) datos.put(JSONObject().put("id", id).put("fotoBase64", foto))
            val respaldo = RespaldoRemoto(db, TIPO_REPORTES)
            respaldo.guardarCopia(1, TIPO_REPORTES, datos.toString())
            respaldo.guardarCopia(2, TIPO_REPORTES, "[{\"id\":88}]")
            respaldo.guardarCopia(1, "reportes-admin", "[{\"id\":99}]")
            val copia = db.respaldoDao().observarCopia(1, TIPO_REPORTES).first()
            assertEquals(3, copia.size)
            assertEquals(foto, JSONObject(copia.first().datos).getString("fotoBase64"))
            respaldo.guardarCopia(1, TIPO_REPORTES, "[]")
            assertTrue(db.respaldoDao().observarCopia(1, TIPO_REPORTES).first().isEmpty())
            assertEquals(88L, db.respaldoDao().observarCopia(2, TIPO_REPORTES).first().single().remotoId)
            assertEquals(99L, db.respaldoDao().observarCopia(1, "reportes-admin").first().single().remotoId)
        } finally { db.close() }
    }

    @Test
    fun conservaColaAlCerrarYReabrirLaBaseYSoloEnviaLaCuentaCorrecta() = runBlocking {
        val nombre = "cola-offline-prueba.db"
        context.deleteDatabase(nombre)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, nombre).build()
        var clave: String? = null
        try {
            val respaldo = RespaldoRemoto(db, TIPO_RESERVAS)
            val pendiente = respaldo.enviarNuevo(usuario, JSONObject().put("motivo", "Clase")) { datos ->
                clave = datos.getString("solicitudId")
                throw ErrorReservaRemota(mensaje = "Sin conexión", servicioInaccesible = true)
            }
            respaldo.enviarNuevo(usuario.copy(id = 2), JSONObject().put("motivo", "Otra cuenta")) {
                throw ErrorReservaRemota(mensaje = "Sin conexión", servicioInaccesible = true)
            }
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, nombre).build()
            var enviados = 0
            val reabierto = RespaldoRemoto(db, TIPO_RESERVAS)
            reabierto.sincronizar(usuario) { datos ->
                assertEquals(clave, datos.getString("solicitudId")); enviados++; 31L
            }
            reabierto.sincronizar(usuario) { enviados++; 32L }
            assertEquals(1, enviados)
            assertEquals(31L, db.respaldoDao().obtener(pendiente.id)?.remotoId)
            assertEquals(1, db.respaldoDao().porEnviar(2, TIPO_RESERVAS).size)
            reabierto.guardarCopia(1, TIPO_RESERVAS, reservaJson)
            assertNull(db.respaldoDao().obtener(pendiente.id))
        } finally { db.close(); context.deleteDatabase(nombre) }
    }

    @Test
    fun reutilizaIdentificadorSiLaRespuestaSePierdeDespuesDeGuardar() = runBlocking {
        val db = database()
        try {
            val recibidos = mutableMapOf<String, Long>()
            val respaldo = RespaldoRemoto(db, TIPO_RESERVAS)
            val envio = respaldo.enviarNuevo(usuario, JSONObject()) { datos ->
                recibidos[datos.getString("solicitudId")] = 31L
                throw ErrorReservaRemota(mensaje = "Respuesta perdida", servicioInaccesible = true)
            }
            respaldo.sincronizar(usuario) { datos -> recibidos.getOrPut(datos.getString("solicitudId")) { 32L } }
            assertEquals(1, recibidos.size)
            assertEquals(31L, db.respaldoDao().obtener(envio.id)?.remotoId)
        } finally { db.close() }
    }

    @Test
    fun informaConflictoSinReintentarNiBorrarDatosDeOtraCuenta() = runBlocking {
        val db = database()
        try {
            val respaldo = RespaldoRemoto(db, TIPO_RESERVAS)
            val envio = respaldo.enviarNuevo(usuario, JSONObject()) {
                throw ErrorReservaRemota(409, "El horario está ocupado.")
            }
            assertEquals("El horario está ocupado.", envio.error)
            respaldo.sincronizar(usuario) { fail("Un conflicto no debe enviarse repetidamente"); 1L }
            db.respaldoDao().descartarError(envio.id, 2)
            assertNotNull(db.respaldoDao().obtener(envio.id))
            db.respaldoDao().descartarError(envio.id, usuario.id)
            assertNull(db.respaldoDao().obtener(envio.id))
        } finally { db.close() }
    }

    @Test
    fun conservaFotoEnColaAunqueElArchivoOriginalYaNoExista() = runBlocking {
        val db = database()
        val foto = File(context.cacheDir, "foto-offline.jpg")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try {
            foto.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 75, it) }
            val datos = ReportesApi(context).preparar(ReporteIncidencia(usuarioId = 1, espacioId = "SALA-1", descripcion = "Ventana", fotoUri = Uri.fromFile(foto).toString()))
                .put("fechaCreacion", 10L)
            val respaldo = RespaldoRemoto(db, TIPO_REPORTES)
            respaldo.enviarNuevo(usuario, datos) { throw ErrorReporteRemoto("Sin conexión", servicioInaccesible = true) }
            foto.delete()
            var fotoEnviada: String? = null
            respaldo.sincronizar(usuario) { fotoEnviada = it.getString("fotoBase64"); 42L }
            assertTrue(fotoEnviada!!.startsWith("/9j/"))
        } finally { bitmap.recycle(); foto.delete(); db.close() }
    }
}
