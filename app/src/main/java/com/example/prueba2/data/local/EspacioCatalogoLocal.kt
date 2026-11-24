package com.example.prueba2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.prueba2.data.catalogo.TipoEspacio

@Entity(tableName = "catalogo_espacios")
internal data class EspacioCatalogoLocal(
    @PrimaryKey val id: String,
    val nombre: String,
    val tipo: TipoEspacio,
    val orden: Int
)
