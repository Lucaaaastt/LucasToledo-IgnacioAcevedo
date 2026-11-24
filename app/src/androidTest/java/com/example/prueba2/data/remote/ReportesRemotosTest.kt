package com.example.prueba2.data.remote

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prueba2.data.local.EstadoReporte
import com.example.prueba2.data.local.ReporteIncidencia
import com.example.prueba2.data.local.Usuario
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Se activa explícitamente con -e neonPrueba IDENTIFICADOR; las pruebas normales no escriben en Neon. */
@RunWith(AndroidJUnit4::class)
class ReportesRemotosTest {
    @Test
    fun registraUsuarioYComparteReporteConFotoPorHttp() = runBlocking {
        val tag = InstrumentationRegistry.getArguments().getString("neonPrueba")
        assumeTrue(tag != null && tag.matches(Regex("ktprueba[0-9a-f]{12}")))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val usuarios = UsuariosApi("http://10.0.2.2:18081")
        val correo = "${tag}android@gmail.com"
        usuarios.registrar("Prueba Android", correo, "PruebaKT1234")
        val perfil = usuarios.login(correo, "PruebaKT1234")
        val usuario = Usuario(id = 77, nombre = perfil.nombre, correo = correo, password = "PruebaKT1234", rol = perfil.rol, remotoId = perfil.id)
        val foto = File(context.cacheDir, "evidencia-integracion.jpg")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try {
            foto.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 75, it) }
            val api = ReportesApi(context, "http://10.0.2.2:18080")
            val id = api.crear(usuario, ReporteIncidencia(usuarioId = usuario.id, espacioId = "SALA-1", descripcion = requireNotNull(tag), fotoUri = Uri.fromFile(foto).toString()))
            val reporte = api.listar(usuario, false).single().reporte
            assertEquals(-id, reporte.id)
            assertEquals(tag, reporte.descripcion)
            assertEquals(EstadoReporte.PENDIENTE, reporte.estado)
            assertTrue(reporte.fotoUri?.startsWith("data:image/jpeg;base64,") == true)
        } finally {
            bitmap.recycle()
            foto.delete()
        }
    }
}
