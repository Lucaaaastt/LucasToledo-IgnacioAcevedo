package com.example.prueba2.data.remote

import com.example.prueba2.BuildConfig
import com.example.prueba2.data.catalogo.TipoEspacio
import com.example.prueba2.data.local.EspacioCatalogoLocal
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException

internal class CatalogoEspaciosApi(
    private val baseUrl: String = BuildConfig.ESPACIOS_API_URL
) {
    suspend fun obtener(): List<EspacioCatalogoLocal>? = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext null
        try {
            val conexion = URI("$baseUrl/api/espacios").toURL().openConnection() as HttpURLConnection
            try {
                conexion.connectTimeout = 5_000
                conexion.readTimeout = 5_000
                conexion.instanceFollowRedirects = false
                conexion.setRequestProperty("Accept", "application/json")
                if (conexion.responseCode != 200) return@withContext null
                val respuesta = conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                leerEspacios(respuesta)
            } finally {
                conexion.disconnect()
            }
        } catch (_: IOException) {
            null
        } catch (_: JSONException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun leerEspacios(respuesta: String): List<EspacioCatalogoLocal>? {
        val json = JSONArray(respuesta)
        val ids = mutableSetOf<String>()
        return List(json.length()) { indice ->
            val item = json.getJSONObject(indice)
            val id = item.getString("id")
            val nombre = item.getString("nombre")
            val tipo = TipoEspacio.valueOf(item.getString("tipo"))
            require(id.isNotBlank() && nombre.isNotBlank() && ids.add(id))
            EspacioCatalogoLocal(id, nombre, tipo, indice)
        }.takeIf { it.isNotEmpty() }
    }
}
