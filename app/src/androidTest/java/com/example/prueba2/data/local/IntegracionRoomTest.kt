package com.example.prueba2.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntegracionRoomTest {
    @Test
    fun conservaCatalogoDeLaRamaDeReservas() = verificarVersionCinco(conCatalogo = true)

    @Test
    fun conservaReportesDeLaRamaDeIncidencias() = verificarVersionCinco(conCatalogo = false)

    private fun verificarVersionCinco(conCatalogo: Boolean) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nombre = "integracion-v5-${if (conCatalogo) "catalogo" else "reportes"}.db"
        context.deleteDatabase(nombre)
        context.getDatabasePath(nombre).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(nombre), null).use { db ->
            db.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, correo TEXT NOT NULL, password TEXT NOT NULL, rol TEXT NOT NULL, remotoId INTEGER)")
            db.execSQL("CREATE UNIQUE INDEX index_usuarios_remotoId ON usuarios (remotoId)")
            db.execSQL("INSERT INTO usuarios VALUES (1, 'Ana', 'ana.prueba@gmail.com', 'clave-original', 'SOLICITANTE', NULL)")
            db.execSQL("""
                CREATE TABLE reservas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    espacioId TEXT NOT NULL, usuarioId INTEGER NOT NULL, fecha INTEGER NOT NULL,
                    horaInicio INTEGER NOT NULL, horaTermino INTEGER NOT NULL, motivo TEXT NOT NULL,
                    estado TEXT NOT NULL DEFAULT 'PENDIENTE',
                    FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE NO ACTION)
            """.trimIndent())
            db.execSQL("CREATE INDEX index_reservas_usuarioId ON reservas (usuarioId)")
            db.execSQL("CREATE INDEX index_reservas_espacioId_fecha_horaInicio ON reservas (espacioId, fecha, horaInicio)")
            if (conCatalogo) {
                db.execSQL("CREATE TABLE catalogo_espacios (id TEXT NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, tipo TEXT NOT NULL, orden INTEGER NOT NULL)")
                db.execSQL("INSERT INTO catalogo_espacios VALUES ('SALA-1', 'Sala original', 'SALA', 1)")
            } else {
                db.execSQL("""
                    CREATE TABLE reportes_incidencia (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, usuarioId INTEGER NOT NULL,
                        espacioId TEXT NOT NULL, descripcion TEXT NOT NULL, fotoUri TEXT,
                        estado TEXT NOT NULL, comentarioAdmin TEXT, fechaCreacion INTEGER NOT NULL)
                """.trimIndent())
                db.execSQL("INSERT INTO reportes_incidencia VALUES (1, 1, 'SALA-1', 'Reporte original', NULL, 'PENDIENTE', NULL, 1)")
            }
            db.version = 5
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, nombre)
            .addMigrations(AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7).build()
        try {
            assertEquals(7, database.openHelper.writableDatabase.version)
            val usuario = database.usuarioDao().obtenerPorId(1)
            assertEquals("Ana", usuario?.nombre)
            assertEquals("clave-original", usuario?.password)
            if (conCatalogo) {
                assertEquals("Sala original", database.catalogoEspaciosDao().obtenerTodos().single().nombre)
                assertTrue(database.reporteDao().obtenerPorUsuario(1).first().isEmpty())
            } else {
                assertEquals("Reporte original", database.reporteDao().obtenerPorUsuario(1).first().single().descripcion)
                assertTrue(database.catalogoEspaciosDao().obtenerTodos().isEmpty())
            }
        } finally {
            database.close()
            context.deleteDatabase(nombre)
        }
    }
}
