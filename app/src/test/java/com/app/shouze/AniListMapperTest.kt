package com.app.shouze

import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.MediaSource
import com.app.shouze.data.local.Status
import com.app.shouze.data.mapper.AniListMapper
import com.app.shouze.data.remote.AniListCoverImage
import com.app.shouze.data.remote.AniListFuzzyDate
import com.app.shouze.data.remote.AniListListGroup
import com.app.shouze.data.remote.AniListListEntry
import com.app.shouze.data.remote.AniListMedia
import com.app.shouze.data.remote.AniListMediaListCollection
import com.app.shouze.data.remote.AniListTitle
import com.app.shouze.data.remote.FuzzyDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AniListMapperTest {

    // ---------------- Status mapping ----------------

    @Test
    fun `CURRENT maps to WATCHING for anime and READING for manga`() {
        assertEquals(Status.WATCHING, AniListMapper.statusFromAniList("CURRENT", "ANIME"))
        assertEquals(Status.READING, AniListMapper.statusFromAniList("CURRENT", "MANGA"))
    }

    @Test
    fun `aniList statuses map to app statuses`() {
        assertEquals(Status.PLAN_TO_WATCH, AniListMapper.statusFromAniList("PLANNING", "ANIME"))
        assertEquals(Status.COMPLETED, AniListMapper.statusFromAniList("COMPLETED", "ANIME"))
        assertEquals(Status.DROPPED, AniListMapper.statusFromAniList("DROPPED", "MANGA"))
        assertEquals(Status.PAUSED, AniListMapper.statusFromAniList("PAUSED", "ANIME"))
        assertEquals(Status.REPEATING, AniListMapper.statusFromAniList("REPEATING", "ANIME"))
    }

    @Test
    fun `unknown status falls back to PLAN_TO_WATCH`() {
        assertEquals(Status.PLAN_TO_WATCH, AniListMapper.statusFromAniList(null, "ANIME"))
        assertEquals(Status.PLAN_TO_WATCH, AniListMapper.statusFromAniList("SOMETHING_NEW", "ANIME"))
    }

    @Test
    fun `app statuses map back to aniList statuses`() {
        assertEquals("CURRENT", AniListMapper.statusToAniList(Status.WATCHING))
        assertEquals("CURRENT", AniListMapper.statusToAniList(Status.READING))
        assertEquals("PLANNING", AniListMapper.statusToAniList(Status.PLAN_TO_WATCH))
        assertEquals("COMPLETED", AniListMapper.statusToAniList(Status.COMPLETED))
        assertEquals("DROPPED", AniListMapper.statusToAniList(Status.DROPPED))
        assertEquals("PAUSED", AniListMapper.statusToAniList(Status.PAUSED))
        assertEquals("REPEATING", AniListMapper.statusToAniList(Status.REPEATING))
    }

    @Test
    fun `status round trip is stable`() {
        Status.entries.forEach { status ->
            val aniList = AniListMapper.statusToAniList(status)
            val mediaType = if (status == Status.READING) "MANGA" else "ANIME"
            assertEquals(status, AniListMapper.statusFromAniList(aniList, mediaType))
        }
    }

    // ---------------- Score conversion ----------------

    @Test
    fun `POINT_100 scores convert both ways`() {
        assertEquals(85.0, AniListMapper.ratingToAniListScore(8.5, "POINT_100"), 0.001)
        assertEquals(8.5, AniListMapper.scoreToAppRating(85.0, "POINT_100"), 0.001)
    }

    @Test
    fun `POINT_5 scores convert both ways`() {
        assertEquals(4.0, AniListMapper.ratingToAniListScore(8.0, "POINT_5"), 0.001)
        assertEquals(8.0, AniListMapper.scoreToAppRating(4.0, "POINT_5"), 0.001)
    }

    @Test
    fun `POINT_10 scores pass through`() {
        assertEquals(7.5, AniListMapper.ratingToAniListScore(7.5, "POINT_10_DECIMAL"), 0.001)
        assertEquals(7.5, AniListMapper.scoreToAppRating(7.5, "POINT_10"), 0.001)
    }

    @Test
    fun `scores clamp to their ranges`() {
        assertEquals(100.0, AniListMapper.ratingToAniListScore(99.0, "POINT_100"), 0.001)
        assertEquals(10.0, AniListMapper.scoreToAppRating(400.0, "POINT_100"), 0.001)
    }

    // ---------------- Fuzzy dates ----------------

    @Test
    fun `fuzzy date converts to utc epoch and back`() {
        val fuzzy = 20240229
        val millis = FuzzyDate.toEpochMillis(fuzzy)
        assertEquals(fuzzy, FuzzyDate.fromEpochMillis(millis))
    }

    @Test
    fun `null fuzzy date parts default sensibly`() {
        assertNull(FuzzyDate.toEpochMillis(null))
        assertEquals(
            FuzzyDate.toEpochMillis(20240101),
            FuzzyDate.toEpochMillis(AniListFuzzyDate(year = 2024))
        )
    }

    // ---------------- Entry -> Entity ----------------

    private fun testCategories() = listOf(
        CategoryEntity(id = "cat-anime", name = "Anime"),
        CategoryEntity(id = "cat-manga", name = "Manga")
    )

    private fun testMedia(
        id: Int = 42,
        type: String = "ANIME",
        episodes: Int? = 12,
        chapters: Int? = null
    ) = AniListMedia(
        id = id,
        title = AniListTitle(romaji = "Cowboy Bebop", english = "Cowboy Bebop"),
        coverImage = AniListCoverImage(large = "https://img.example/l.jpg"),
        episodes = episodes,
        chapters = chapters,
        genres = listOf("Action", "Sci-Fi"),
        type = type
    )

    private fun testEntry(
        entry: AniListListEntry
    ) = entry

    @Test
    fun `entry maps to entity with stable local id`() {
        val entry = AniListListEntry(
            id = 1001,
            mediaId = 42,
            status = "CURRENT",
            score = 85.0,
            progress = 5,
            repeat = 1,
            notes = "classic",
            startedAt = AniListFuzzyDate(2024, 1, 15),
            updatedAt = 1700000000,
            media = testMedia()
        )

        val entity = AniListMapper.entryToEntity(entry, "POINT_100", testCategories())!!

        assertEquals("anilist-42", entity.id)
        assertEquals(MediaSource.ANILIST, entity.source)
        assertEquals(42, entity.anilistId)
        assertEquals(1001, entity.listEntryId)
        assertEquals("ANIME", entity.mediaType)
        assertEquals(Status.WATCHING, entity.status)
        assertEquals(5, entity.currentProgress)
        assertEquals(12, entity.totalCount)
        assertEquals(8.5, entity.rating, 0.001)
        assertEquals(1, entity.rewatchCount)
        assertEquals("classic", entity.notes)
        assertEquals("cat-anime", entity.categoryId)
        assertEquals(false, entity.pendingSync)
    }

    @Test
    fun `manga entry maps chapters and volumes`() {
        val entry = AniListListEntry(
            id = 1002,
            mediaId = 7,
            status = "CURRENT",
            progress = 33,
            progressVolumes = 4,
            media = testMedia(id = 7, type = "MANGA", episodes = null, chapters = 200)
        )

        val entity = AniListMapper.entryToEntity(entry, "POINT_10", testCategories())!!

        assertEquals(Status.READING, entity.status)
        assertEquals(33, entity.currentProgress)
        assertEquals(200, entity.totalCount)
        assertEquals(4, entity.currentVolume)
        assertEquals("cat-manga", entity.categoryId)
    }

    @Test
    fun `entries without media or title are skipped`() {
        val noMedia = AniListListEntry(id = 1, mediaId = 42)
        assertNull(AniListMapper.entryToEntity(noMedia, "POINT_100", testCategories()))

        val noTitle = AniListListEntry(
            id = 2,
            mediaId = 43,
            media = testMedia(id = 43).copy(title = AniListTitle())
        )
        assertNull(AniListMapper.entryToEntity(noTitle, "POINT_100", testCategories()))
    }

    @Test
    fun `collection flattens duplicate entries across custom lists`() {
        val collection = AniListMediaListCollection(
            lists = listOf(
                AniListListGroup(
                    name = "Watching",
                    status = "CURRENT",
                    isCustomList = false,
                    entries = listOf(
                        AniListListEntry(id = 1, mediaId = 42, status = "CURRENT", media = testMedia())
                    )
                ),
                AniListListGroup(
                    name = "Favorites",
                    status = null,
                    isCustomList = true,
                    entries = listOf(
                        AniListListEntry(id = 1, mediaId = 42, status = "CURRENT", media = testMedia()),
                        AniListListEntry(
                            id = 2,
                            mediaId = 99,
                            status = "PLANNING",
                            media = testMedia(id = 99, episodes = null)
                        )
                    )
                )
            )
        )

        val entities = AniListMapper.collectionToEntities(collection, "POINT_100", testCategories())
        assertEquals(2, entities.size)
        assertEquals(listOf("anilist-42", "anilist-99"), entities.map { it.id })
    }

    // ---------------- Entity -> Save payload ----------------

    @Test
    fun `entity converts to save payload in user's score format`() {
        val entity = AniListMapper.entryToEntity(
            AniListListEntry(id = 1001, mediaId = 42, status = "CURRENT", media = testMedia()),
            "POINT_100",
            testCategories()
        )!!.copy(rating = 7.0, notes = "hello", currentProgress = 3)

        val payload = AniListMapper.entityToSavePayload(entity, "POINT_100")

        assertEquals(42, payload.mediaId)
        assertEquals("CURRENT", payload.status)
        assertEquals(70.0, payload.score!!, 0.001)
        assertEquals(3, payload.progress)
        assertEquals("hello", payload.notes)
    }

    @Test
    fun `zero rating is sent as null rather than zero`() {
        val entity = AniListMapper.entryToEntity(
            AniListListEntry(id = 1001, mediaId = 42, status = "PLANNING", media = testMedia()),
            "POINT_100",
            testCategories()
        )!!

        val payload = AniListMapper.entityToSavePayload(entity, "POINT_100")
        assertNull(payload.score)
        assertEquals("PLANNING", payload.status)
    }
}
