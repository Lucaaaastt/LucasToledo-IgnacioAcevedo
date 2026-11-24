package com.example.prueba2.data.repository

import androidx.room.withTransaction
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EstadoReporte
import com.example.prueba2.data.local.ReporteConAutor
import com.example.prueba2.data.local.ReporteIncidencia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ReporteRepository(private val database: AppDatabase) {

    fun obtenerPorUsuario(usuarioId: Int): Flow<List<ReporteIncidencia>> {
        require(usuarioId > 0) { "Se requiere un usuario válido." }
        return database.reporteDao().obtenerPorUsuario(usuarioId)
    }

    fun obtenerTodosConAutor(administradorId: Int): Flow<List<ReporteConAutor>> = flow {
        require(database.usuarioDao().obtenerPorId(administradorId)?.rol == "ADMIN") {
            "Se requiere una cuenta de administrador para revisar los reportes."
        }
        emitAll(database.reporteDao().obtenerTodosConAutor())
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
}