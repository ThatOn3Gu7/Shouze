package com.app.shouze.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.random.Random

enum class AvatarCategory(val label: String) {
    LEGACY("Current 16 Types"),
    ANIME("Anime"),
    CARTOON("Cartoon"),
    ROBOT("Robots & Monsters"),
    ABSTRACT("Abstract"),
    PHOTO("Real Photos")
}

data class AvatarOption(
    val id: String,
    val url: String,
    val sourceName: String,
    val category: AvatarCategory
)

private data class StaticSource(
    val id: String,
    val name: String,
    val category: AvatarCategory,
    val buildUrl: (seed: String, size: Int) -> String
)

private val STATIC_SOURCES = listOf(
    // DiceBear — open source, 55 styles, self-hostable
    StaticSource("ds-adventurers", "Adventurers", AvatarCategory.CARTOON) { s, sz ->
        "https://api.dicebear.com/9.x/adventurers/png?seed=$s&size=$sz"
    },
    StaticSource("ds-big-smile", "Big Smile", AvatarCategory.CARTOON) { s, sz ->
        "https://api.dicebear.com/9.x/big-smile/png?seed=$s&size=$sz"
    },
    StaticSource("ds-lorelei", "Lorelei", AvatarCategory.CARTOON) { s, sz ->
        "https://api.dicebear.com/9.x/lorelei/png?seed=$s&size=$sz"
    },
    StaticSource("ds-open-peeps", "Open Peeps", AvatarCategory.CARTOON) { s, sz ->
        "https://api.dicebear.com/9.x/open-peeps/png?seed=$s&size=$sz"
    },
    StaticSource("ds-pixel-art", "Pixel Art", AvatarCategory.CARTOON) { s, sz ->
        "https://api.dicebear.com/9.x/pixel-art/png?seed=$s&size=$sz"
    },
    StaticSource("ds-bottts", "Bottts", AvatarCategory.ROBOT) { s, sz ->
        "https://api.dicebear.com/9.x/bottts/png?seed=$s&size=$sz"
    },
    StaticSource("ds-shapes", "Shapes", AvatarCategory.ABSTRACT) { s, sz ->
        "https://api.dicebear.com/9.x/shapes/png?seed=$s&size=$sz"
    },
    // RoboHash — open source
    StaticSource("rh-robots", "Robots", AvatarCategory.ROBOT) { s, sz ->
        "https://robohash.org/$s?set=set1&size=${sz}x$sz"
    },
    StaticSource("rh-monsters", "Monsters", AvatarCategory.ROBOT) { s, sz ->
        "https://robohash.org/$s?set=set2&size=${sz}x$sz"
    },
    StaticSource("rh-cats", "Cats", AvatarCategory.ROBOT) { s, sz ->
        "https://robohash.org/$s?set=set4&size=${sz}x$sz"
    },
    // Pravatar — 70 real portraits, seed picks the index
    StaticSource("pravatar", "Portrait", AvatarCategory.PHOTO) { s, sz ->
        val n = (Math.abs(s.hashCode()) % 70) + 1
        "https://i.pravatar.cc/$sz?img=$n"
    }
)

private val avatarJson = Json { ignoreUnknownKeys = true }

@Serializable private data class RandomUserResponse(val results: List<RandomUserResult> = emptyList())
@Serializable private data class RandomUserResult(val picture: RandomUserPicture? = null)
@Serializable private data class RandomUserPicture(val medium: String = "")

@Serializable private data class JikanResponse(val data: List<JikanCharacter> = emptyList())
@Serializable private data class JikanCharacter(val mal_id: Int = 0, val images: JikanImages? = null)
@Serializable private data class JikanImages(val jpg: JikanJpg? = null)
@Serializable private data class JikanJpg(val image_url: String? = null)

@Serializable private data class WaifuImResponse(val images: List<WaifuImImage> = emptyList())
@Serializable private data class WaifuImImage(val url: String = "")

class AvatarRepository(private val client: OkHttpClient) {

    /** One refresh = one new [seed]. Returns a shuffled mixed batch. */
    suspend fun fetchBatch(seed: String, sizePx: Int = 240, perSource: Int = 4): List<AvatarOption> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val static = STATIC_SOURCES.flatMap { src ->
                    (0 until perSource).map { i ->
                        val s = "$seed-${src.id}-$i"
                        AvatarOption(s, src.buildUrl(s, sizePx), src.name, src.category)
                    }
                }
                val dynamic = listOf(
                    async { randomUsers() },
                    async { jikanCharacters() },
                    async { waifuIm() },
                    async { yourExistingApi(seed) }
                ).awaitAll().flatten()

                (static + dynamic).shuffled(Random(seed.hashCode()))
            }
        }

    /** Your current 16-type API. Pass the fresh seed on every refresh so the
     *  backend returns a new batch; if it ignores seeds, append "&r=$seed"
     *  as a cache-buster. */
    private fun yourExistingApi(seed: String): List<AvatarOption> {
        // TODO: replace with your real call, e.g.:
        // val body = get("https://your-api.example/pictures?batch=$seed") ?: return emptyList()
        // ...parse and map to AvatarOption(id, url, "Classic", AvatarCategory.LEGACY)
        return emptyList()
    }

    private fun randomUsers(): List<AvatarOption> {
        val body = get("https://randomuser.me/api/?results=12") ?: return emptyList()
        return runCatching {
            avatarJson.decodeFromString<RandomUserResponse>(body).results.mapNotNull { r ->
                r.picture?.medium?.takeIf { it.isNotBlank() }?.let {
                    AvatarOption("ruser-${it.hashCode()}", it, "Real Photo", AvatarCategory.PHOTO)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun jikanCharacters(): List<AvatarOption> {
        val page = Random.nextInt(1, 40) // random page = fresh anime faces per refresh
        val body = get("https://api.jikan.moe/v4/characters?page=$page&order_by=members&sort=desc")
            ?: return emptyList()
        return runCatching {
            avatarJson.decodeFromString<JikanResponse>(body).data.mapNotNull { c ->
                c.images?.jpg?.image_url?.takeIf { it.isNotBlank() }?.let {
                    AvatarOption("jikan-${c.mal_id}", it, "Anime Character", AvatarCategory.ANIME)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun waifuIm(): List<AvatarOption> {
        val body = get("https://api.waifu.im/images/?included_tags=waifu&is_nsfw=false&many=true")
            ?: return emptyList()
        return runCatching {
            avatarJson.decodeFromString<WaifuImResponse>(body).images.mapNotNull { img ->
                img.url.takeIf { it.isNotBlank() }?.let {
                    AvatarOption("waifuim-${it.hashCode()}", it, "Anime Art", AvatarCategory.ANIME)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun get(url: String): String? = runCatching {
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    }.getOrNull()
}