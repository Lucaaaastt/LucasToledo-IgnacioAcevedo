package com.example.prueba2.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EstadoReporte {
    PENDIENTE,
    REVISADO
}

@Entity(tableName = "reportes_incidencia")
data class ReporteIncidencia(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Int,
    val espacioId: String,
    val descripcion: String,
    val fotoUri: String? = null,
    val estado: EstadoReporte = EstadoReporte.PENDIENTE,
    val comentarioAdmin: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val pendienteEnvio: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    val errorEnvio: String? = null
)

data class ReporteConAutor(
    @Embedded val reporte: ReporteIncidencia,
    val autorNombre: String
)