package com.example.prueba2.data.catalogo



enum class TipoEspacio(val etiqueta: String) {
    SALA("Sala"),
    LABORATORIO("Laboratorio de computación"),
    BIBLIOTECA("Biblioteca"),
    GIMNASIO("Gimnasio")
}

data class EspacioReserva(val id: String, val nombre: String)

fun espaciosDelTipo(tipo: TipoEspacio?): List<EspacioReserva> = when (tipo) {
    TipoEspacio.SALA -> (1..12).map { EspacioReserva("SALA-$it", "Sala $it") }
    TipoEspacio.LABORATORIO -> (1..3).map {
        EspacioReserva("LAB-$it", "Laboratorio de Computación $it")
    }
    TipoEspacio.BIBLIOTECA -> listOf(EspacioReserva("BIBLIOTECA", "Biblioteca"))
    TipoEspacio.GIMNASIO -> listOf(EspacioReserva("GIMNASIO", "Gimnasio"))
    null -> emptyList()
}
