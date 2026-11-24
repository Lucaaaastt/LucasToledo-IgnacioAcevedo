package com.example.prueba2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Usuario::class, Reserva::class, EspacioCatalogoLocal::class, ReporteIncidencia::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(ReservaConverters::class)
abstract class AppDatabase : RoomDatabase (){
    abstract fun usuarioDao(): UsuarioDao
    abstract fun reservaDao(): ReservaDao
    abstract fun reporteDao(): ReporteDao
    internal abstract fun catalogoEspaciosDao(): CatalogoEspaciosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reservas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        espacioId TEXT NOT NULL,
                        usuarioId INTEGER NOT NULL,
                        fecha INTEGER NOT NULL,
                        horaInicio INTEGER NOT NULL,
                        horaTermino INTEGER NOT NULL,
                        motivo TEXT NOT NULL,
                        FOREIGN KEY(usuarioId) REFERENCES usuarios(id)
                            ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reservas_usuarioId ON reservas (usuarioId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reservas_espacioId_fecha_horaInicio ON reservas (espacioId, fecha, horaInicio)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reservas ADD COLUMN estado TEXT NOT NULL DEFAULT 'PENDIENTE'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN remotoId INTEGER")
                db.execSQL("CREATE UNIQUE INDEX index_usuarios_remotoId ON usuarios (remotoId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE catalogo_espacios (
                        id TEXT NOT NULL PRIMARY KEY,
                        nombre TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        orden INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Las dos ramas tenían versión 5: una con catálogo y otra con reportes.
        // Crear lo que falta permite abrir cualquiera de esas instalaciones sin borrar datos.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS catalogo_espacios (
                        id TEXT NOT NULL PRIMARY KEY,
                        nombre TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        orden INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reportes_incidencia (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        usuarioId INTEGER NOT NULL,
                        espacioId TEXT NOT NULL,
                        descripcion TEXT NOT NULL,
                        fotoUri TEXT,
                        estado TEXT NOT NULL,
                        comentarioAdmin TEXT,
                        fechaCreacion INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "colegioMP_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
