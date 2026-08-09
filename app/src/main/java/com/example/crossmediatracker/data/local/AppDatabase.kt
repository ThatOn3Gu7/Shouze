package com.example.crossmediatracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MediaItemEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        colorHex TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    ALTER TABLE media_items ADD COLUMN categoryId TEXT NOT NULL DEFAULT 'TV_SERIES'
                """.trimIndent())

                db.execSQL("""
                    UPDATE media_items SET categoryId = CASE
                        WHEN mediaType = '0' OR mediaType = 'TV_SERIES' THEN 'TV_SERIES'
                        WHEN mediaType = '1' OR mediaType = 'ANIME' THEN 'ANIME'
                        WHEN mediaType = '2' OR mediaType = 'NOVEL' THEN 'NOVEL'
                        ELSE 'TV_SERIES'
                    END
                """.trimIndent())

                db.execSQL("INSERT OR IGNORE INTO categories (id, name, colorHex, createdAt) VALUES ('TV_SERIES', 'TV Series', NULL, 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, colorHex, createdAt) VALUES ('ANIME', 'Anime', NULL, 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, colorHex, createdAt) VALUES ('NOVEL', 'Novel', NULL, 0)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "media_tracker.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
