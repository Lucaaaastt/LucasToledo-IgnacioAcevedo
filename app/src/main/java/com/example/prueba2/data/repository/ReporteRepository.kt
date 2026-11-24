package com.example.prueba2.data.repository

import android.content.Context
import com.example.prueba2.data.remote.ReportesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import androidx.room.withTransaction
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EstadoReporte
import com.example.prueba2.data.local.ReporteConAutor
import com.example.prueba2.data.local.Usuario
import com.example.prueba2.data.local.ReporteIncidencia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ReporteRepository(private val database: AppDatabase, context: Context) {
    private val api = ReportesApi(context.applicationContext)
    private val recargar = MutableStateFlow(0)

    fun obtenerPorUsuario(usuarioId: Int): Flow<List<ReporteIncidencia>> = flow {
        val usuario = checkNotNull(database.usuarioDao().obtenerPorId(usuarioId)) { "Vuelve a iniciar sesión." }
        val locales = database.reporteDao().obtenerPorUsuario(usuarioId)
        if (usuario.remotoId == null) {
            emitAll(locales)
        } else {
            emitAll(combine(locales, remotos(usuario, false)) { anteriores, remotos ->
                (anteriores + remotos.map { it.reporte }).sortedByDescending { it.fechaCreacion }
            })
        }
    }

    fun obtenerTodosConAutor(administradorId: Int): Flow<List<ReporteConAutor>> = flow {
        val usuario = checkNotNull(database.usuarioDao().obtenerPorId(administradorId)) { "Vuelve a iniciar sesión." }
        require(usuario.rol == "ADMIN") { "Se requiere una cuenta de administrador para revisar los reportes." }
        val locales = database.reporteDao().obtenerTodosConAutor()
        if (usuario.remotoId == null) {
            emitAll(locales)
        } else {
            emitAll(combine(locales, remotos(usuario, true)) { anteriores, remotos ->
                (anteriores + remotos).sortedWith(compareBy({ it.reporte.estado != EstadoReporte.PENDIENTE }, { -it.reporte.fechaCreacion }))
            })
        }
    }

    suspend fun enviarReporte(reporte: ReporteIncidencia): Long {
        require(reporte.id == 0L) { "El reporte debe ser nuevo." }
        require(reporte.usuarioId > 0) { "El reporte requiere un usuario válido." }
        require(reporte.espacioId.isNotBlank()) { "Selecciona el espacio donde ocurrió la incidencia." }

        val descripcionLimpia = reporte.descripcion.trim()
        require(descripcionLimpia.isNotEmpty() && descripcionLimpia.length <= 500) {
            "La descripción debe tener entre 1 y 500 caracteres."
        }

        val fotoLimpia = reporte.fotoUri?.takeIf { it.isNotBlank() }

        val usuario = checkNotNull(database.usuarioDao().obtenerPorId(reporte.usuarioId)) { "Vuelve a iniciar sesión." }
        if (usuario.remotoId != null) {
            val id = api.crear(usuario, reporte.copy(descripcion = descripcionLimpia, fotoUri = fotoLimpia))
            recargar.update { it + 1 }
            return -id
        }
        return database.withTransaction {
            checkNotNull(database.usuarioDao().obtenerPorId(reporte.usuarioId)) {
                "El usuario que intenta reportar no existe."
            }
            database.reporteDao().insertarReporte(
                reporte.copy(
                    descripcion = descripcionLimpia,
                    fotoUri = fotoLimpia,
                    estado = EstadoReporte.PENDIENTE,
                    comentarioAdmin = null,
                    fechaCreacion = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun responderReporte(
        administradorId: Int,
        reporteId: Long,
        comentario: String
    ) {
        val comentarioLimpio = comentario.trim()
        require(comentarioLimpio.isNotEmpty() && comentarioLimpio.length <= 500) {
            "La respuesta debe tener entre 1 y 500 caracteres."
        }

        if (reporteId < 0) {
            val usuario = checkNotNull(database.usuarioDao().obtenerPorId(administradorId)) { "Vuelve a iniciar sesión." }
            require(usuario.rol == "ADMIN" && usuario.remotoId != null) { "Se requiere un administrador conectado al servidor." }
            api.responder(usuario, -reporteId, comentarioLimpio)
            recargar.update { it + 1 }
            return
        }
        database.withTransaction {
            require(database.usuarioDao().obtenerPorId(administradorId)?.rol == "ADMIN") {
                "Se requiere una cuenta de administrador para responder reportes."
            }
            val dao = database.reporteDao()
            checkNotNull(dao.obtenerPorId(reporteId)) { "El reporte ya no existe." }
            val filasActualizadas = dao.responderReporte(
                reporteId = reporteId,
                comentarioAdmin = comentarioLimpio,
                estado = EstadoReporte.REVISADO
            )
            check(filasActualizadas == 1) { "No se pudo actualizar el reporte." }
        }
    }

    private fun remotos(usuario: Usuario, todos: Boolean): Flow<List<ReporteConAutor>> =
        recargar.map { api.listar(usuario, todos) }

}
