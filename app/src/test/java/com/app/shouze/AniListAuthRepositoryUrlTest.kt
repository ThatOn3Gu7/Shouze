package com.app.shouze

import com.app.shouze.data.auth.AniListAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AniListAuthRepositoryUrlTest {

    @Test
    fun `authorize url matches anilist implicit grant exactly`() {
        assertEquals(
            "https://anilist.co/api/v2/oauth/authorize?client_id=50591&response_type=token",
            AniListAuthRepository.buildAuthorizeUrl("50591")
        )
    }

    @Test
    fun `authorize url must not carry redirect_uri (anilist rejects it)`() {
        val url = AniListAuthRepository.buildAuthorizeUrl("50591")
        assertFalse(url.contains("redirect_uri"))
    }
}
