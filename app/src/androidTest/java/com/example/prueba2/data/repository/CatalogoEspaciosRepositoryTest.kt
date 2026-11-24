package com.example.prueba2.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prueba2.data.catalogo.EspacioReserva
import com.example.prueba2.data.catalogo.TipoEspacio
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.local.EspacioCatalogoLocal
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogoEspaciosRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        AppDatabase::class.java
    ).build()

    @After
    fun cerrar() {
        database.close()
    }

    @Test
    fun conservaLaUltimaRespuestaCuandoElServicioFalla() = runBlocking {
        val salas = listOf(
            EspacioCatalogoLocal("SALA-2", "Sala 2", TipoEspacio.SALA, 0),
            EspacioCatalogoLocal("SALA-1", "Sala 1", TipoEspacio.SALA, 1)
        )
        val remoto = CatalogoEspaciosRepository(database) { salas }.cargar().toList()
        val sinRed = CatalogoEspaciosRepository(database) { null }.cargar().toList()

        assertEquals(listOf(EspacioReserva("SALA-2", "Sala 2"), EspacioReserva("SALA-1", "Sala 1")),
            remoto.single()[TipoEspacio.SALA])
        assertEquals(remoto, sinRed)
    }

    @Test
    fun reemplazaLaCopiaGuardadaCuandoElServidorActualizaElCatalogo() = runBlocking {
        val anterior = EspacioCatalogoLocal("SALA-1", "Sala 1", TipoEspacio.SALA, 0)
        val actual = EspacioCatalogoLocal("SALA-1", "Sala principal", TipoEspacio.SALA, 0)
        CatalogoEspaciosRepository(database) { listOf(anterior) }.cargar().toList()
        val resultado = CatalogoEspaciosRepository(database) { listOf(actual) }.cargar().toList()

        assertEquals(listOf("Sala 1", "Sala principal"), resultado.map { it.getValue(TipoEspacio.SALA).single().nombre })
        assertEquals(listOf(actual), database.catalogoEspaciosDao().obtenerTodos())
    }
}
