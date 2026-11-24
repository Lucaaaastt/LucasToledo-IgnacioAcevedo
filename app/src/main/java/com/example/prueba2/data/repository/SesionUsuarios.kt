package com.example.prueba2.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.Usuario
import com.example.prueba2.data.remote.ErrorUsuarios
import com.example.prueba2.data.remote.UsuariosApi

internal class SesionUsuarios(
    private val database: AppDatabase,
    private val api: UsuariosApi = UsuariosApi()
) {
    suspend fun login(correo: String, password: String): Usuario {
        val correoLimpio = correo.trim()
        val usuarios = database.usuarioDao()
        try {
            val perfil = try {
                api.login(correoLimpio, password)
            } catch (error: ErrorUsuarios) {
                if (!error.servicioInaccesible) throw error
                return usuarios.login(correoLimpio, password) ?: throw error
            }

            return database.withTransaction {
                // Las reservas de Room apuntan al ID local, no al de PostgreSQL.
                val existente = usuarios.obtenerPorIdRemoto(perfil.id)
                    ?: usuarios.obtenerCuentaLocalSinVincular(correoLimpio, password)
                val cuenta = Usuario(
                    id = existente?.id ?: 0,
                    nombre = perfil.nombre,
                    correo = correoLimpio,
                    password = password,
                    rol = perfil.rol,
                    remotoId = perfil.id
                )
                if (existente == null) {
                    cuenta.copy(id = usuarios.registrarUsuario(cuenta).toInt())
                } else {
                    usuarios.actualizarUsuario(cuenta)
                    cuenta
                }
            }
        } catch (_: SQLiteException) {
            throw ErrorUsuarios(mensaje = "No se pudo preparar el acceso a tus reservas locales.")
        }
    }
}
