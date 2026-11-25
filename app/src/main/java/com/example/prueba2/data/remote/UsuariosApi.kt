package com.example.prueba2.data.remote

import com.example.prueba2.BuildConfig
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

internal class ErrorUsuarios(
    val estadoHttp: Int? = null,
    val servicioInaccesible: Boolean = false,
    mensaje: String
) : Exception(mensaje)

internal data class PerfilUsuario(val id: Int, val nombre: String, val rol: String)

internal class UsuariosApi(private val baseUrl: String = BuildConfig.USUARIOS_API_URL) {

    suspend fun registrar(nombre: String, correo: String, password: String, codigo: String? = null) {
        val datos = JSONObject().put("nombre", nombre.trim()).put("correo", correo.trim())
            .put("password", password)
        if (codigo != null) datos.put("codigoSeguridad", codigo.trim())
        enviar(if (codigo == null) "registro" else "registro-admin", datos, 201)
    }

    suspend fun login(correo: String, password: String): PerfilUsuario {
        val respuesta = enviar("login", JSONObject().put("correo", correo).put("password", password), 200)
        try {
            val datos = JSONObject(respuesta)
            val perfil = PerfilUsuario(datos.getInt("id"), datos.getString("nombre"), datos.getString("rol"))
            if (perfil.id <= 0 || perfil.nombre.isBlank() || perfil.rol !in listOf("ADMIN", "SOLICITANTE")) {
                throw JSONException("Perfil inválido")
            }
            return perfil
        } catch (_: JSONException) {
            throw ErrorUsuarios(mensaje = "El servicio de usuarios devolvió un perfil inválido.")
        }
    }

    private suspend fun enviar(ruta: String, datos: JSONObject, esperado: Int): String =
        withContext(Dispatchers.IO) {
            if (baseUrl.isBlank()) {
                throw ErrorUsuarios(servicioInaccesible = true, mensaje = "Configura la URL del servicio de usuarios.")
            }
            try {
                val conexion = URL("$baseUrl/api/usuarios/$ruta").openConnection() as HttpURLConnection
                try {
                    conexion.requestMethod = "POST"
                    conexion.instanceFollowRedirects = false
                    conexion.connectTimeout = 5_000
                    conexion.readTimeout = 5_000
                    conexion.doOutput = true
                    conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    conexion.outputStream.use { it.write(datos.toString().toByteArray(Charsets.UTF_8)) }
                    val estado = conexion.responseCode
                    if (estado != esperado) throw errorHttp(estado, ruta)
                    if (esperado == 200) {
                        conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        ""
                    }
                } finally {
                    conexion.disconnect()
                }
            } catch (_: SocketTimeoutException) {
                throw ErrorUsuarios(servicioInaccesible = true, mensaje = "El servidor tarda en responder. Espera unos segundos y vuelve a intentarlo.")
            } catch (_: IOException) {
                throw ErrorUsuarios(servicioInaccesible = true, mensaje = "No se pudo conectar al servicio de usuarios. Revisa tu conexión a internet y vuelve a intentarlo.")
            }
        }

    private fun errorHttp(estado: Int, ruta: String): ErrorUsuarios {
        val mensaje = when (estado) {
            400 -> "Revisa los campos del formulario."
            401 -> "Correo o contraseña incorrectos."
            403 -> "Código de administrador incorrecto."
            409 -> "El correo ya está registrado."
            503 -> if (ruta == "registro-admin") {
                "El registro administrativo no está disponible."
            } else {
                "El servicio de usuarios no está disponible."
            }
            in 500..599 -> "El servicio de usuarios no está disponible."
            else -> "No se pudo completar la operación."
        }
        return ErrorUsuarios(estado, servicioInaccesible = estado in 500..599, mensaje = mensaje)
    }
}
