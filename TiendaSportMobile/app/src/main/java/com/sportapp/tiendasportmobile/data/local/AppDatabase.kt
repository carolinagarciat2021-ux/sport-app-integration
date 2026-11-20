package com.sportapp.tiendasportmobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CarritoItemEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun carritoDao(): CarritoDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        // v1 -> v2: cada fila del carrito pertenece a un cliente (0 = invitado)
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE carrito_items ADD COLUMN idCliente INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tienda_sport.db"
                ).addMigrations(MIGRACION_1_2).build().also { instancia = it }
            }
    }
}
