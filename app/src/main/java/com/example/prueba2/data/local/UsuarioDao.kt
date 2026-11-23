package com.example.prueba2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuarios WHERE id = :usuarioId LIMIT 1")
    suspend fun obtenerPorId(usuarioId: Int): Usuario?

    @Query("SELECT * FROM usuarios WHERE remotoId = :remotoId LIMIT 1")
    suspend fun obtenerPorIdRemoto(remotoId: Int): Usuario?

    @Insert
    suspend fun registrarUsuario(usuario: Usuario): Long

    @Update
    suspend fun actualizarUsuario(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE correo = :correo AND password = :password LIMIT 1")
    suspend fun login(correo: String, password: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE correo = :correo AND password = :password AND remotoId IS NULL ORDER BY id LIMIT 1")
    suspend fun obtenerCuentaLocalSinVincular(correo: String, password: String): Usuario?
}
