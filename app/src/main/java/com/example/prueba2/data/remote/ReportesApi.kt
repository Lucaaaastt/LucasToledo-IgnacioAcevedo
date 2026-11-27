package com.example.prueba2.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.prueba2.BuildConfig
import com.example.prueba2.data.local.EstadoReporte
import com.example.prueba2.data.local.ReporteConAutor
import com.example.prueba2.data.local.ReporteIncidencia
import com.example.prueba2.data.local.Usuario
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class ErrorReporteRemoto(mensaje: String, val servicioInaccesible: Boolean = false) : Exception(mensaje)

internal class ReportesApi(
    private val context: Context,
    private val baseUrl: String = BuildConfig.ESPACIOS_API_URL
) {
    suspend fun preparar(reporte: ReporteIncidencia): JSONObject = withContext(Dispatchers.IO) {
        JSONObject().put("espacioId", reporte.espacioId).put("descripcion", reporte.descripcion.trim()).also { datos ->
            fotoBase64(reporte.fotoUri)?.let { datos.put("fotoBase64", it) }
        }
    }

    suspend fun crear(usuario: Usuario, reporte: ReporteIncidencia): Long = crearDatos(usuario, preparar(reporte))

    suspend fun crearDatos(usuario: Usuario, datos: JSONObject): Long =
        JSONObject(enviar(usuario, "POST", "", 201, datos)).getLong("id")

    suspend fun obtenerListado(usuario: Usuario, todos: Boolean): String =
        enviar(usuario, "GET", if (todos) "" else "/mios", 200)

    suspend fun listar(usuario: Usuario, todos: Boolean): List<ReporteConAutor> =
        leerListado(usuario, obtenerListado(usuario, todos))

    fun leerListado(usuario: Usuario, json: String): List<ReporteConAutor> {
        val datos = JSONArray(json)
        return List(datos.length()) { i ->
            val fila = datos.getJSONObject(i)
            ReporteConAutor(
                ReporteIncidencia(
                    id = -fila.getLong("id").also { require(it > 0) },
                    usuarioId = usuario.id,
                    espacioId = fila.getString("espacioId"),
                    descripcion = fila.getString("descripcion"),
                    fotoUri = if (fila.isNull("fotoBase64")) null else "data:image/jpeg;base64," + fila.getString("fotoBase64"),
                    estado = EstadoReporte.valueOf(fila.getString("estado")),
                    comentarioAdmin = if (fila.isNull("comentarioAdmin")) null else fila.getString("comentarioAdmin"),
                    fechaCreacion = fila.getLong("fechaCreacion")
                ),
                fila.getString("autorNombre")
            )
        }
    }

    suspend fun responder(usuario: Usuario, id: Long, comentario: String) {
        enviar(usuario, "PUT", "/$id/respuesta", 204, JSONObject().put("comentario", comentario.trim()))
    }

    private fun fotoBase64(uri: String?): String? {
        if (uri == null) return null
        val origen = Uri.parse(uri)
        val dimensiones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(origen)?.use { BitmapFactory.decodeStream(it, null, dimensiones) }
        val opciones = BitmapFactory.Options().apply {
            inSampleSize = 1
            while (dimensiones.outWidth / inSampleSize > 960 || dimensiones.outHeight / inSampleSize > 960) inSampleSize *= 2
        }
        val imagen = context.contentResolver.openInputStream(origen)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: throw ErrorReporteRemoto("No se pudo leer la foto. Adjunta otra imagen.")
        val bytes = try {
            ByteArrayOutputStream().use { salida ->
                imagen.compress(Bitmap.CompressFormat.JPEG, 75, salida)
                salida.toByteArray()
            }
        } finally { imagen.recycle() }
        if (bytes.size > 1_000_000) throw ErrorReporteRemoto("La foto es demasiado grande. Adjunta otra imagen.")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private suspend fun enviar(usuario: Usuario, metodo: String, ruta: String, esperado: Int, datos: JSONObject? = null): String =
        withContext(Dispatchers.IO) {
            if (baseUrl.isBlank()) throw ErrorReporteRemoto("Configura el servicio de espacios y reportes.")
            try {
                val conexion = URI("$baseUrl/api/reportes$ruta").toURL().openConnection() as HttpURLConnection
                try {
                    conexion.requestMethod = metodo
                    conexion.instanceFollowRedirects = false
                    conexion.connectTimeout = 5_000
                    conexion.readTimeout = TIEMPO_LECTURA_SERVIDOR_MS
                    val credenciales = "${usuario.correo}:${usuario.password}".toByteArray(Charsets.UTF_8)
                    conexion.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(credenciales, Base64.NO_WRAP))
                    conexion.setRequestProperty("Accept", "application/json")
                    if (datos != null) {
                        conexion.doOutput = true
                        conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        conexion.outputStream.use { it.write(datos.toString().toByteArray(Charsets.UTF_8)) }
                    }
                    val estado = conexion.responseCode
                    if (estado != esperado) throw ErrorReporteRemoto(when (estado) {
                        400 -> "Revisa el espacio, la descripción y la foto del reporte."
                        401, 403 -> "La cuenta no tiene acceso. Vuelve a iniciar sesión."
                        404 -> "El reporte o el espacio ya no está disponible."
                        else -> "No se pudo guardar o consultar el reporte. Intenta nuevamente."
                    }, servicioInaccesible = estado >= 500)
                    if (esperado == 204) "" else conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } finally { conexion.disconnect() }
            } catch (_: SocketTimeoutException) {
                throw ErrorReporteRemoto("El servidor tarda en responder. Espera unos segundos y vuelve a intentarlo.", servicioInaccesible = true)
            } catch (_: IOException) {
                throw ErrorReporteRemoto("No se pudo conectar al servicio de reportes. Revisa tu conexión a internet y vuelve a intentarlo.", servicioInaccesible = true)
            }
        }
}
