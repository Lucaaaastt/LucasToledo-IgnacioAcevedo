package com.example.prueba2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface CatalogoEspaciosDao {
    @Query("SELECT * FROM catalogo_espacios ORDER BY orden")
    suspend fun obtenerTodos(): List<EspacioCatalogoLocal>

    @Query("DELETE FROM catalogo_espacios")
    suspend fun borrarTodos(): Int

    @Insert
    suspend fun insertarTodos(espacios: List<EspacioCatalogoLocal>): List<Long>
}
