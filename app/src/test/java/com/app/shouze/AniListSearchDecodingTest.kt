package com.app.shouze

import com.app.shouze.data.remote.AniListSearchResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the search-screen crash:
 * `Field 'mediaId' is required for type 'AniListListEntry', but it was missing
 * at path $.[data.Page.media][0].mediaListEntry` — the search query selects a
 * partial mediaListEntry, so the model must tolerate missing identity fields.
 */
class AniListSearchDecodingTest {

    // Mirrors the Json configuration used by AniListApi.
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `partial mediaListEntry without mediaId decodes instead of crashing`() {
        val payload = """
            {"data":{"Page":{
                "pageInfo":{"total":1,"currentPage":1,"lastPage":1,"hasNextPage":false},
                "media":[{
                    "id":21,
                    "title":{"romaji":"One Piece","english":"One Piece","native":"ワンピース"},
                    "coverImage":{"large":"https://img.example/l.jpg","medium":"https://img.example/m.jpg"},
                    "bannerImage":null,
                    "description":"Pirates.",
                    "episodes":null,"chapters":null,"volumes":null,
                    "status":"RELEASING",
                    "genres":["Action","Adventure"],
                    "isAdult":false,
                    "countryOfOrigin":"JP",
                    "averageScore":88,
                    "mediaListEntry":{"id":98765,"status":"CURRENT","score":9.5,"progress":1100}
                }]
            }}}
        """.trimIndent()

        val decoded = json.decodeFromString(AniListSearchResponse.serializer(), payload)
        val entry = decoded.data?.Page?.media?.single()?.mediaListEntry

        assertTrue(entry != null)
        assertEquals(98765, entry!!.id)
        assertEquals(0, entry.mediaId) // tolerated default; mapper keys off media.id
        assertEquals("CURRENT", entry.status)
        assertEquals(9.5, entry.score!!, 0.001)
    }

    @Test
    fun `null mediaListEntry decodes`() {
        val payload = """
            {"data":{"Page":{"media":[{
                "id":5,
                "title":{"romaji":"Cowboy Bebop"},
                "mediaListEntry":null
            }]}}}
        """.trimIndent()

        val decoded = json.decodeFromString(AniListSearchResponse.serializer(), payload)
        assertNull(decoded.data?.Page?.media?.single()?.mediaListEntry)
    }

    @Test
    fun `full entry payload from collection sync still decodes with mediaId`() {
        val payload = """
            {"data":{"Page":{"media":[{
                "id":7,
                "title":{"romaji":"Trigun"},
                "mediaListEntry":{"id":111,"mediaId":7,"status":"COMPLETED","score":85.0,"progress":26}
            }]}}}
        """.trimIndent()

        val decoded = json.decodeFromString(AniListSearchResponse.serializer(), payload)
        val entry = decoded.data?.Page?.media?.single()?.mediaListEntry!!
        assertEquals(7, entry.mediaId)
        assertEquals("COMPLETED", entry.status)
    }
}
