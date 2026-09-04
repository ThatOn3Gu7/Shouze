package com.app.shouze.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LibraryEntryEntity::class, CategoryEntity::class],
    version = 7,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "media_tracker.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS categories (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    colorHex TEXT,
                    createdAt INTEGER NOT NULL
                )""")
                db.execSQL("ALTER TABLE media_items ADD COLUMN categoryId TEXT NOT NULL DEFAULT 'TV_SERIES'")
                db.execSQL("""UPDATE media_items SET categoryId = CASE
                    WHEN mediaType = '0' OR mediaType = 'TV_SERIES' THEN 'TV_SERIES'
                    WHEN mediaType = '1' OR mediaType = 'ANIME' THEN 'ANIME'
                    WHEN mediaType = '2' OR mediaType = 'NOVEL' THEN 'NOVEL'
                    ELSE 'TV_SERIES' END""")
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

        /**
         * 6 -> 7: the big online-first move.
         *
         * Creates the new `library_entries` table (AniList-shaped) and migrates
         * the legacy `media_items` rows into it as local-only entries so nothing
         * the user already tracked is lost. The old table is intentionally left in
         * place (Room ignores tables it doesn't declare) to avoid a destructive DROP.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS library_entries (
                    localId TEXT PRIMARY KEY NOT NULL,
                    anilistListId INTEGER,
                    mediaId INTEGER,
                    title TEXT NOT NULL,
                    type TEXT NOT NULL,
                    categoryId TEXT,
                    format TEXT,
                    status TEXT NOT NULL,
                    progress INTEGER NOT NULL,
                    progressVolumes INTEGER,
                    totalEpisodes INTEGER,
                    totalChapters INTEGER,
                    totalVolumes INTEGER,
                    score INTEGER NOT NULL,
                    `repeat` INTEGER NOT NULL,
                    notes TEXT NOT NULL,
                    coverImageUrl TEXT,
                    bannerImageUrl TEXT,
                    genres TEXT NOT NULL,
                    description TEXT,
                    season TEXT,
                    seasonYear INTEGER,
                    averageScore INTEGER,
                    meanScore INTEGER,
                    popularity INTEGER,
                    startedAt TEXT,
                    completedAt TEXT,
                    isFavorite INTEGER NOT NULL,
                    pendingSync INTEGER NOT NULL,
                    lastSyncedAt INTEGER NOT NULL,
                    lastUpdated INTEGER NOT NULL
                )""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_library_entries_mediaId ON library_entries (mediaId)")

                db.execSQL("""INSERT OR IGNORE INTO library_entries (
                    localId, anilistListId, mediaId, title, type, categoryId, format, status, progress,
                    progressVolumes, totalEpisodes, totalChapters, totalVolumes, score, `repeat`,
                    notes, coverImageUrl, bannerImageUrl, genres, description, season, seasonYear,
                    averageScore, meanScore, popularity, startedAt, completedAt, isFavorite,
                    pendingSync, lastSyncedAt, lastUpdated
                )
                SELECT
                    id, NULL, NULL, title,
                    CASE WHEN categoryId IN ('MANGA','LIGHT_NOVEL','NOVEL','WEBTOON') THEN 'MANGA' ELSE 'ANIME' END,
                    categoryId,
                    NULL,
                    CASE status
                        WHEN 'WATCHING' THEN 'CURRENT'
                        WHEN 'READING' THEN 'CURRENT'
                        WHEN 'COMPLETED' THEN 'COMPLETED'
                        WHEN 'DROPPED' THEN 'DROPPED'
                        ELSE 'PLANNING'
                    END,
                    currentProgress, currentVolume,
                    CASE WHEN categoryId NOT IN ('MANGA','LIGHT_NOVEL','NOVEL','WEBTOON') THEN totalCount END,
                    CASE WHEN categoryId IN ('MANGA','LIGHT_NOVEL','NOVEL','WEBTOON') THEN totalCount END,
                    NULL,
                    CAST(ROUND(rating * 10) AS INTEGER), rewatchCount,
                    notes, coverImageUri, NULL, genres, NULL, NULL, NULL,
                    NULL, NULL, NULL,
                    CASE WHEN startDate IS NOT NULL THEN strftime('%Y-%m-%d', startDate / 1000, 'unixepoch') END,
                    CASE WHEN endDate IS NOT NULL THEN strftime('%Y-%m-%d', endDate / 1000, 'unixepoch') END,
                    isFavorite, 0, 0, lastUpdated
                FROM media_items""")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('ANIME', 'Anime', NULL, 0)")
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('MANGA', 'Manga', NULL, 0)")
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('NOVEL', 'Light Novel', NULL, 0)")
                        db.execSQL("INSERT INTO categories (id, name, colorHex, createdAt) VALUES ('MOVIE', 'Movie', NULL, 0)")
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
