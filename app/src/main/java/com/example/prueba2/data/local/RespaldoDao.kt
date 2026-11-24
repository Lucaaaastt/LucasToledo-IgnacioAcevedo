package com.example.prueba2.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "copias_remotas", primaryKeys = ["usuarioId", "tipo", "remotoId"])
internal data class CopiaRemota(val usuarioId: Int, val tipo: String, val remotoId: Long, val datos: String)

@Entity(tableName = "envios_pendientes")
internal data class EnvioPendiente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clave: String,
    val usuarioId: Int,
    val tipo: String,
    val datos: String,
    val remotoId: Long? = null,
    val error: String? = null
) {
    // Se conserva la convención existente: remoto negativo; local positivo.
    val idEnPantalla: Long get() = remotoId?.let { -it } ?: (Long.MAX_VALUE - id)
}

@Dao
internal interface RespaldoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarCopias(copias: List<CopiaRemota>): List<Long>

    @Query("SELECT * FROM copias_remotas WHERE usuarioId = :usuarioId AND tipo = :tipo")
    fun observarCopia(usuarioId: Int, tipo: String): Flow<List<CopiaRemota>>

    @Query("DELETE FROM copias_remotas WHERE usuarioId = :usuarioId AND tipo = :tipo")
    suspend fun borrarCopia(usuarioId: Int, tipo: String): Int

    @Insert
    suspend fun encolar(envio: EnvioPendiente): Long

    @Query("SELECT * FROM envios_pendientes WHERE usuarioId = :usuarioId AND tipo = :tipo ORDER BY id")
    fun observarEnvios(usuarioId: Int, tipo: String): Flow<List<EnvioPendiente>>

    @Query("SELECT * FROM envios_pendientes WHERE usuarioId = :usuarioId AND tipo = :tipo AND remotoId IS NULL AND error IS NULL ORDER BY id")
    suspend fun porEnviar(usuarioId: Int, tipo: String): List<EnvioPendiente>

    @Query("SELECT * FROM envios_pendientes WHERE id = :id")
    suspend fun obtener(id: Long): EnvioPendiente?

    @Query("UPDATE envios_pendientes SET remotoId = :remotoId WHERE id = :id")
    suspend fun confirmar(id: Long, remotoId: Long): Int

    @Query("UPDATE envios_pendientes SET remotoId = :remotoId, error = NULL WHERE usuarioId = :usuarioId AND tipo = :tipo AND clave = :clave")
    suspend fun confirmarPorClave(usuarioId: Int, tipo: String, clave: String, remotoId: Long): Int

    @Query("UPDATE envios_pendientes SET error = :error WHERE id = :id")
    suspend fun marcarError(id: Long, error: String): Int

    @Query("DELETE FROM envios_pendientes WHERE usuarioId = :usuarioId AND tipo = :tipo AND remotoId IN (:ids)")
    suspend fun retirarConfirmados(usuarioId: Int, tipo: String, ids: List<Long>): Int

    @Query("DELETE FROM envios_pendientes WHERE id = :id AND usuarioId = :usuarioId AND error IS NOT NULL")
    suspend fun descartarError(id: Long, usuarioId: Int): Int
}
