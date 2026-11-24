package com.example.prueba2.data.repository

import androidx.room.withTransaction
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.CopiaRemota
import com.example.prueba2.data.local.EnvioPendiente
import com.example.prueba2.data.local.Usuario
import com.example.prueba2.data.remote.ErrorReporteRemoto
import com.example.prueba2.data.remote.ErrorReservaRemota
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

internal fun List<CopiaRemota>.comoJson(): String = JSONArray().also { lista ->
    forEach { lista.put(JSONObject(it.datos)) }
}.toString()

internal const val TIPO_RESERVAS = "reservas"
internal const val TIPO_REPORTES = "reportes"
internal fun Throwable.esCaidaDeServicio(): Boolean =
    (this as? ErrorReservaRemota)?.servicioInaccesible == true ||
    (this as? ErrorReporteRemoto)?.servicioInaccesible == true

/** Room es la copia consultable y la cola durable. Solo el servidor confirma disponibilidad. */
internal class RespaldoRemoto(private val db: AppDatabase, private val tipo: String) {
    private val dao = db.respaldoDao()

    suspend fun encolar(usuarioId: Int, datos: JSONObject): Long {
        val clave = UUID.randomUUID().toString()
        datos.put("solicitudId", clave)
        return dao.encolar(EnvioPendiente(clave = clave, usuarioId = usuarioId, tipo = tipo, datos = datos.toString()))
    }

    suspend fun enviarNuevo(usuario: Usuario, datos: JSONObject, enviar: suspend (JSONObject) -> Long): EnvioPendiente = bloqueo.withLock {
        val id = encolar(usuario.id, datos)
        sincronizarDentroDelBloqueo(usuario, enviar)
        checkNotNull(dao.obtener(id))
    }

    suspend fun sincronizar(usuario: Usuario, enviar: suspend (JSONObject) -> Long) = bloqueo.withLock {
        sincronizarDentroDelBloqueo(usuario, enviar)
    }

    private suspend fun sincronizarDentroDelBloqueo(usuario: Usuario, enviar: suspend (JSONObject) -> Long) {
        for (pendiente in dao.porEnviar(usuario.id, tipo)) {
            try {
                val id = enviar(JSONObject(pendiente.datos))
                dao.confirmar(pendiente.id, id)
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                if (error.esCaidaDeServicio()) return
                dao.marcarError(pendiente.id, error.message ?: "No se pudo enviar. Revisa la solicitud.")
            }
        }
    }

    suspend fun guardarCopia(usuarioId: Int, alcance: String, json: String) = bloqueo.withLock {
        val lista = JSONArray(json)
        val ids = List(lista.length()) { lista.getJSONObject(it).getLong("id") }
        db.withTransaction {
            for (i in 0 until lista.length()) {
                val fila = lista.getJSONObject(i)
                val clave = fila.optString("solicitudId").takeIf { it.isNotBlank() && it != "null" }
                if (clave != null) dao.confirmarPorClave(usuarioId, tipo, clave, fila.getLong("id"))
            }
            // Una fila por registro evita juntar todas las fotos en una sola fila SQLite.
            dao.borrarCopia(usuarioId, alcance)
            dao.guardarCopias(List(lista.length()) { i ->
                val fila = lista.getJSONObject(i)
                CopiaRemota(usuarioId, alcance, fila.getLong("id"), fila.toString())
            })
            dao.retirarConfirmados(usuarioId, tipo, ids)
        }
    }

    companion object {
        // Evita dos envíos simultáneos de la misma fila desde pantallas diferentes.
        private val bloqueo = Mutex()
    }
}
