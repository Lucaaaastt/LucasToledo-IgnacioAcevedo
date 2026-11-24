package com.example.prueba2.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.example.prueba2.data.catalogo.EspacioReserva
import com.example.prueba2.data.catalogo.TipoEspacio
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EspacioCatalogoLocal
import com.example.prueba2.data.remote.CatalogoEspaciosApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class CatalogoEspaciosRepository(
    private val database: AppDatabase,
    private val obtenerRemoto: suspend () -> List<EspacioCatalogoLocal>? = CatalogoEspaciosApi()::obtener
) {
    fun cargar(): Flow<Map<TipoEspacio, List<EspacioReserva>>> = flow {
        val dao = database.catalogoEspaciosDao()
        val cache = try {
            dao.obtenerTodos()
        } catch (_: SQLiteException) {
            emptyList()
        }
        if (cache.isNotEmpty()) emit(cache.porTipo())

        val remoto = obtenerRemoto() ?: return@flow
        try {
            database.withTransaction {
                dao.borrarTodos()
                dao.insertarTodos(remoto)
            }
        } catch (_: SQLiteException) {
            // Si falla la escritura, la transacción conserva el catálogo anterior.
        }
        emit(remoto.porTipo())
    }

    private fun List<EspacioCatalogoLocal>.porTipo() =
        groupBy({ it.tipo }, { EspacioReserva(it.id, it.nombre) })
}
