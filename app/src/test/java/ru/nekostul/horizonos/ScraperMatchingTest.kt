package ru.nekostul.horizonos

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.nekostul.horizonos.ui.settings.launcher.scanning.HttpClient
import ru.nekostul.horizonos.ui.settings.launcher.scanning.LibretroCatalog
import ru.nekostul.horizonos.ui.settings.launcher.scanning.TitleMatcher

class ScraperMatchingTest {

    @Test
    fun searchQuery_stripsRegionAndRevisionTags() {
        assertEquals("Driver 2", TitleMatcher.searchQuery("Driver 2 [RUS].cue"))
        assertEquals("Driver 2", TitleMatcher.searchQuery("Driver 2 (USA).cue"))
        assertEquals("Gran Turismo 2", TitleMatcher.searchQuery("Gran Turismo 2 (USA)"))
        assertEquals("Final Fantasy VII", TitleMatcher.searchQuery("Final Fantasy VII [Disc 1]"))
    }

    @Test
    fun matchScore_rejectsSequelMismatch() {
        // "Driver 2" must not match a plain "Driver"
        assertTrue(TitleMatcher.matchScore("Driver 2", "Driver") < 70)
        assertEquals(100, TitleMatcher.matchScore("Driver 2", "Driver 2"))
    }

    @Test
    fun libretroCatalog_findsDriver2OnRealCdn() = runBlocking {
        val entry = LibretroCatalog.best("Sony - PlayStation", "Named_Boxarts", "Driver 2")
        assertNotNull("Expected a Libretro boxart match for Driver 2", entry)
        assertTrue(
            "Unexpected match: ${entry?.displayName}",
            entry!!.displayName.startsWith("Driver 2")
        )
        assertTrue(
            "Match should not be the plain 'Driver': ${entry.displayName}",
            entry.displayName != "Driver (Europe).png"
        )
    }

    @Test
    fun libretroBoxart_downloadsRealImage() = runBlocking {
        val entry = LibretroCatalog.best("Sony - PlayStation", "Named_Boxarts", "Driver 2")
        assertNotNull(entry)
        val url = "https://thumbnails.libretro.com/Sony%20-%20PlayStation/Named_Boxarts/${entry!!.href}"
        val bytes = HttpClient.getBytes(url)
        assertNotNull("Boxart download returned null for $url", bytes)
        assertTrue("Boxart download was empty for $url", (bytes?.size ?: 0) > 1000)
    }
}
