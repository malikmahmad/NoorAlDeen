package com.nooraldeen.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentLogicTest {
    private fun ayah(id: Int, juz: Int, number: Int = 1) = Ayah(
        id, 1, number, juz, 1, "آية $id", "Verse $id", "Commentary $id"
    )

    @Test
    fun juzStartsUsesLowestVerifiedAyahId() {
        val starts = juzStarts(listOf(ayah(20, 2), ayah(11, 1), ayah(10, 1), ayah(30, 2)))
        assertEquals(2, starts.size)
        assertEquals(10, starts[1]?.id)
        assertEquals(20, starts[2]?.id)
    }

    @Test
    fun searchMatchesArabicTranslationAndTafseer() {
        assertTrue(matchesAyah(ayah(1, 1), "آية"))
        assertTrue(matchesAyah(ayah(1, 1), "verse"))
        assertTrue(matchesAyah(ayah(1, 1), "commentary"))
    }

    @Test
    fun bookmarkAndReadingProgressAreStable() {
        assertEquals("2:7", bookmarkKey(2, 7))
        assertEquals(12, readingProgress(8, 12))
        assertEquals(12, readingProgress(12, 4))
    }
}
