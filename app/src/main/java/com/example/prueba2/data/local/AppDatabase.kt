package com.example.prueba2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Usuario::class, Reserva::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(ReservaConverters::class)
abstract class AppDatabase : RoomDatabase (){
    abstract fun usuarioDao(): UsuarioDao
    abstract fun reservaDao(): ReservaDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "colegioMP_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
