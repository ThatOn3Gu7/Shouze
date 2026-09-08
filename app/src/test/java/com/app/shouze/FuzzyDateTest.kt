package com.app.shouze

import com.app.shouze.data.remote.AniListFuzzyDate
import com.app.shouze.data.remote.FuzzyDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FuzzyDateTest {

    @Test
    fun `toInput produces the FuzzyDateInput object shape AniList requires`() {
        val millis = FuzzyDate.toEpochMillis(20240229)
        assertEquals(AniListFuzzyDate(year = 2024, month = 2, day = 29), FuzzyDate.toInput(millis))
    }

    @Test
    fun `toInput round trips through toEpochMillis`() {
        val millis = FuzzyDate.toEpochMillis(20240115)
        val back = FuzzyDate.toEpochMillis(FuzzyDate.toInput(millis))
        assertEquals(millis, back)
    }

    @Test
    fun `partial dates keep their month and day defaults`() {
        val millis = FuzzyDate.toEpochMillis(AniListFuzzyDate(year = 2024))
        assertEquals(AniListFuzzyDate(year = 2024, month = 1, day = 1), FuzzyDate.toInput(millis))
    }
}
