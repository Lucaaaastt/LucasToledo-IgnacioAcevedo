package com.example.prueba2.data.remote

import android.util.Base64
import com.example.prueba2.BuildConfig
import com.example.prueba2.data.local.EstadoReserva
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.local.ReservaConSolicitante
import com.example.prueba2.data.local.Usuario
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class ErrorReservaRemota(
    val estado: Int? = null,
    mensaje: String,
    val servicioInaccesible: Boolean = false
) : Exception(mensaje)

internal class ReservasApi(private val baseUrl: String = BuildConfig.ESPACIOS_API_URL) {
    suspend fun crear(usuario: Usuario, reserva: Reserva): Long {
        val datos = JSONObject().put("espacioId", reserva.espacioId)
            .put("fecha", reserva.fecha.toString())
            .put("horaInicio", reserva.horaInicio.toString())
            .put("horaTermino", reserva.horaTermino.toString())
            .put("motivo", reserva.motivo)
        return crearDatos(usuario, datos)
    }

    suspend fun crearDatos(usuario: Usuario, datos: JSONObject): Long =
        JSONObject(enviar(usuario, "POST", "", 201, datos)).getLong("id")

    suspend fun obtenerListado(usuario: Usuario, pendientes: Boolean): String =
        enviar(usuario, "GET", if (pendientes) "/pendientes" else "/mias", 200)

    suspend fun listar(usuario: Usuario, pendientes: Boolean): List<ReservaConSolicitante> =
        leerListado(usuario, obtenerListado(usuario, pendientes))

    fun leerListado(usuario: Usuario, json: String): List<ReservaConSolicitante> {
        try {
            val datos = JSONArray(json)
            return List(datos.length()) { indice ->
                val fila = datos.getJSONObject(indice)
                ReservaConSolicitante(
                    reserva = Reserva(
                        id = -fila.getLong("id").also { require(it > 0) },
                        espacioId = fila.getString("espacioId"),
                        usuarioId = usuario.id,
                        fecha = LocalDate.parse(fila.getString("fecha")),
                        horaInicio = LocalTime.parse(fila.getString("horaInicio")),
                        horaTermino = LocalTime.parse(fila.getString("horaTermino")),
                        motivo = fila.getString("motivo"),
                        estado = EstadoReserva.valueOf(fila.getString("estado"))
                    ),
                    solicitanteNombre = fila.getString("solicitanteNombre")
                )
            }
        } catch (_: JSONException) {
            throw ErrorReservaRemota(mensaje = "El servidor devolvió un listado inválido.")
        }
    }

    suspend fun decidir(usuario: Usuario, id: Long, estado: EstadoReserva) {
        enviar(usuario, "PUT", "/$id/estado", 204, JSONObject().put("estado", estado.name))
    }

    private suspend fun enviar(
        usuario: Usuario, metodo: String, ruta: String, esperado: Int, datos: JSONObject? = null
    ): String = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) throw ErrorReservaRemota(mensaje = "Configura el servicio de reservas.")
        try {
            val conexion = URI("$baseUrl/api/reservas$ruta").toURL().openConnection() as HttpURLConnection
            try {
                conexion.requestMethod = metodo
                conexion.instanceFollowRedirects = false
                conexion.connectTimeout = 5_000
                conexion.readTimeout = 7_000
                val credenciales = "${usuario.correo}:${usuario.password}".toByteArray(Charsets.UTF_8)
                conexion.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(credenciales, Base64.NO_WRAP))
                conexion.setRequestProperty("Accept", "application/json")
                if (datos != null) {
                    conexion.doOutput = true
                    conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    conexion.outputStream.use { it.write(datos.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = conexion.responseCode
                if (status != esperado) {
                    val mensaje = when (status) {
                        400 -> "Revisa la fecha, el horario y el motivo."
                        401, 403 -> "La cuenta no tiene acceso. Vuelve a iniciar sesión."
                        404 -> "El espacio no está disponible en el catálogo del servidor."
                        409 -> if (metodo == "PUT") "La solicitud ya cambió de estado. Actualiza las reservas antes de decidir."
                            else "El horario está ocupado. Elige otro horario o espacio antes de enviar."
                        else -> "No se pudo acceder al servicio de reservas. Intenta nuevamente."
                    }
                    throw ErrorReservaRemota(status, mensaje, servicioInaccesible = status >= 500)
                }
                if (esperado == 204) "" else conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                conexion.disconnect()
            }
        } catch (_: SocketTimeoutException) {
            throw ErrorReservaRemota(mensaje = "El servidor tarda en responder. Espera unos segundos y vuelve a intentarlo.", servicioInaccesible = true)
        } catch (_: IOException) {
            throw ErrorReservaRemota(mensaje = "No se pudo conectar al servicio de reservas. Revisa tu conexión a internet y vuelve a intentarlo.", servicioInaccesible = true)
        }
    }
}
