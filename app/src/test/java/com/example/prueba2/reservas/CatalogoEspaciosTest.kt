package com.example.prueba2.reservas

import com.example.prueba2.data.catalogo.EspacioReserva
import com.example.prueba2.data.catalogo.TipoEspacio
import com.example.prueba2.data.catalogo.espaciosDelTipo
import org.junit.Assert
import org.junit.Test

class CatalogoEspaciosTest {

    @Test
    fun espaciosDelTipo_retornaListaCorrectaSegunTipoEspacio() {
        val salas = espaciosDelTipo(TipoEspacio.SALA)
        Assert.assertEquals(12, salas.size)
        Assert.assertEquals(EspacioReserva("SALA-1", "Sala 1"), salas.first())
        Assert.assertEquals(EspacioReserva("SALA-12", "Sala 12"), salas.last())

        val laboratorios = espaciosDelTipo(TipoEspacio.LABORATORIO)
        Assert.assertEquals(3, laboratorios.size)
        Assert.assertEquals(
            EspacioReserva("LAB-1", "Laboratorio de Computación 1"),
            laboratorios.first()
        )

        Assert.assertEquals(
            listOf(EspacioReserva("BIBLIOTECA", "Biblioteca")),
            espaciosDelTipo(TipoEspacio.BIBLIOTECA)
        )
        Assert.assertEquals(
            listOf(EspacioReserva("GIMNASIO", "Gimnasio")),
            espaciosDelTipo(TipoEspacio.GIMNASIO)
        )
        Assert.assertTrue(espaciosDelTipo(null).isEmpty())
    }
}