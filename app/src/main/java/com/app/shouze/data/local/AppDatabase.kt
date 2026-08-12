package com.app.shouze.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(
    entities = [MediaItemEntity::class, CategoryEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
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

                db.execSQL("ALTER TABLE media_items ADD COLUMN categoryId TEXT NOT NULL DEFAULT 'TV_SERIES'")

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN genres TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
    return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('TV_SERIES', 'TV Series', NULL, 0)")
                db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('ANIME', 'Anime', NULL, 0)")
                db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('NOVEL', 'Novel', NULL, 0)")
            }
        })
        .build()
        INSTANCE = instance
        instance
    }
}

        fun isCorruptionError(e: Throwable): Boolean {
            val message = e.message ?: return false
            return message.contains("malformed") ||
                message.contains("not a database") ||
                message.contains("disk I/O error")
        }

        fun recoverFromCorruption(context: Context) {
            runCatching {
                INSTANCE?.close()
                INSTANCE = null
                val dbFile = context.getDatabasePath(DB_NAME)
                listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                    val file = File(dbFile.path + suffix)
                    if (file.exists()) file.delete()
                }
                Log.w("Shouze", "Corrupt database files deleted; a fresh database will be created on next open")
            }
        }

        private const val DB_NAME = "media_tracker.db"
    }
}
