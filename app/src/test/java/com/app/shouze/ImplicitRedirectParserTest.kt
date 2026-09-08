package com.app.shouze

import com.app.shouze.data.auth.ImplicitRedirectParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImplicitRedirectParserTest {

    @Test
    fun `parses access token from url fragment`() {
        val uri = "shouze://anilist-auth#access_token=abc123.def.ghi&token_type=Bearer&expires=31536000"
        val result = ImplicitRedirectParser.parse(uri)

        assertTrue(result is ImplicitRedirectParser.Result.Success)
        result as ImplicitRedirectParser.Result.Success
        assertEquals("abc123.def.ghi", result.parsed.accessToken)
        assertEquals(31536000L, result.parsed.expiresInSeconds)
    }

    @Test
    fun `parses token from query string as a fallback`() {
        val uri = "shouze://anilist-auth?access_token=querytoken&expires=1000"
        val result = ImplicitRedirectParser.parse(uri)

        assertTrue(result is ImplicitRedirectParser.Result.Success)
        result as ImplicitRedirectParser.Result.Success
        assertEquals("querytoken", result.parsed.accessToken)
        assertEquals(1000L, result.parsed.expiresInSeconds)
    }

    @Test
    fun `denied response is surfaced`() {
        val uri = "shouze://anilist-auth#error=access_denied&error_description=The+user+denied+access"
        val result = ImplicitRedirectParser.parse(uri)

        assertTrue(result is ImplicitRedirectParser.Result.Denied)
        result as ImplicitRedirectParser.Result.Denied
        assertEquals("access_denied", result.error)
        assertEquals("The user denied access", result.description)
    }

    @Test
    fun `unrelated urls are ignored`() {
        assertEquals(
            ImplicitRedirectParser.Result.NotAnAuthRedirect,
            ImplicitRedirectParser.parse("https://anilist.co/some/page")
        )
        assertEquals(
            ImplicitRedirectParser.Result.NotAnAuthRedirect,
            ImplicitRedirectParser.parse("shouze://anilist-auth")
        )
    }

    @Test
    fun `missing token with token_type present is a denial`() {
        val uri = "shouze://anilist-auth#token_type=Bearer"
        assertTrue(ImplicitRedirectParser.parse(uri) is ImplicitRedirectParser.Result.Denied)
    }

    @Test
    fun `expires defaults to one year when absent`() {
        val uri = "shouze://anilist-auth#access_token=tok"
        val result = ImplicitRedirectParser.parse(uri) as ImplicitRedirectParser.Result.Success
        assertEquals(365L * 24 * 60 * 60, result.parsed.expiresInSeconds)
    }
}
