package com.example.prueba2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {

    @Insert
    suspend fun insertarReporte(reporte: ReporteIncidencia): Long

    @Query("SELECT * FROM reportes_incidencia WHERE usuarioId = :usuarioId ORDER BY fechaCreacion DESC, id DESC")
    fun obtenerPorUsuario(usuarioId: Int): Flow<List<ReporteIncidencia>>

    @Query(
        """
        SELECT reportes_incidencia.*, usuarios.nombre AS autorNombre
        FROM reportes_incidencia
        INNER JOIN usuarios ON usuarios.id = reportes_incidencia.usuarioId
        ORDER BY 
            CASE WHEN reportes_incidencia.estado = 'PENDIENTE' THEN 0 ELSE 1 END ASC,
            reportes_incidencia.fechaCreacion DESC,
            reportes_incidencia.id DESC
        """
    )
    fun obtenerTodosConAutor(): Flow<List<ReporteConAutor>>

    @Query("SELECT * FROM reportes_incidencia WHERE id = :reporteId LIMIT 1")
    suspend fun obtenerPorId(reporteId: Long): ReporteIncidencia?

    @Query(
        """
        UPDATE reportes_incidencia 
        SET comentarioAdmin = :comentarioAdmin, estado = :estado 
        WHERE id = :reporteId
        """
    )
    suspend fun responderReporte(
        reporteId: Long,
        comentarioAdmin: String,
        estado: EstadoReporte = EstadoReporte.REVISADO
    ): Int
}