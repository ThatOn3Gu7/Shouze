package com.app.shouze.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MediaItemEntity::class, CategoryEntity::class, SyncOutboxEntity::class, RemoteCacheEntity::class],
    version = 7,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun categoryDao(): CategoryDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun remoteCacheDao(): RemoteCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "media_tracker.db"

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_items ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE media_items ADD COLUMN rewatchCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_items ADD COLUMN startDate INTEGER")
                db.execSQL("ALTER TABLE media_items ADD COLUMN endDate INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, colorHex, createdAt) VALUES ('MANGA', 'Manga', NULL, 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, colorHex, createdAt) VALUES ('MOVIE', 'Movie', NULL, 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, colorHex, createdAt) VALUES ('LIGHT_NOVEL', 'Light Novel', NULL, 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, colorHex, createdAt) VALUES ('OVA', 'OVA', NULL, 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, colorHex, createdAt) VALUES ('WEBTOON', 'Webtoon', NULL, 0)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // AniList-backed fields. Existing rows stay LOCAL-owned.
                db.execSQL("ALTER TABLE media_items ADD COLUMN source TEXT NOT NULL DEFAULT 'LOCAL'")
                db.execSQL("ALTER TABLE media_items ADD COLUMN anilistId INTEGER")
                db.execSQL("ALTER TABLE media_items ADD COLUMN listEntryId INTEGER")
                db.execSQL("ALTER TABLE media_items ADD COLUMN mediaType TEXT")
                db.execSQL("ALTER TABLE media_items ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_items_anilistId ON media_items(anilistId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_outbox (
                        seq INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        op TEXT NOT NULL,
                        localItemId TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        attempts INTEGER NOT NULL DEFAULT 0,
                        lastError TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS remote_cache (
                        cacheKey TEXT PRIMARY KEY NOT NULL,
                        json TEXT NOT NULL,
                        fetchedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('TV_SERIES', 'TV Series', NULL, 0)")
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('ANIME', 'Anime', NULL, 0)")
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('NOVEL', 'Novel', NULL, 0)")
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('MANGA', 'Manga', NULL, 0)")
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('MOVIE', 'Movie', NULL, 0)")
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('LIGHT_NOVEL', 'Light Novel', NULL, 0)")
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('OVA', 'OVA', NULL, 0)")
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('WEBTOON', 'Webtoon', NULL, 0)")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
