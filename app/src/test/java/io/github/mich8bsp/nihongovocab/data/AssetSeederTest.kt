package io.github.mich8bsp.nihongovocab.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetSeederTest {
    @Test
    fun parsesTextAndMeaningsFromJsonArray() {
        val json = """
            [
              {"text": "青", "meanings": ["blue"]},
              {"text": "ぢ", "meanings": ["ji", "di"]}
            ]
        """.trimIndent()

        val entries = parseEntries(json, Level.N5)

        assertEquals(2, entries.size)
        assertEquals("青", entries[0].text)
        assertEquals(listOf("blue"), entries[0].meanings)
        assertEquals(Level.N5, entries[0].level)
        assertEquals(listOf("ji", "di"), entries[1].meanings)
    }

    @Test
    fun emptyArrayParsesToEmptyList() {
        assertEquals(emptyList<Entry>(), parseEntries("[]", Level.KANA))
    }

    @Test
    fun diffContentUpdatesRefreshesChangedMeaningsPreservingProgress() {
        val existing = Entry(
            id = 42,
            text = "コート",
            meanings = listOf("coat; court (e.g.", "tennis)"),
            romaji = "kooto",
            level = Level.N5,
            correctStreak = 2,
            totalCorrect = 5,
            totalWrong = 1,
        )
        val fresh = Entry(
            text = "コート",
            meanings = listOf("coat", "court (e.g., tennis)"),
            romaji = "kooto",
            level = Level.N5,
        )

        val updates = diffContentUpdates(listOf(existing), listOf(fresh))

        assertEquals(1, updates.size)
        val updated = updates.single()
        assertEquals(42L, updated.id)
        assertEquals(listOf("coat", "court (e.g., tennis)"), updated.meanings)
        assertEquals(2, updated.correctStreak)
        assertEquals(5, updated.totalCorrect)
        assertEquals(1, updated.totalWrong)
    }

    @Test
    fun diffContentUpdatesSkipsUnchangedEntries() {
        val entry = Entry(id = 1, text = "青", meanings = listOf("blue"), romaji = "ao", level = Level.N5)

        assertEquals(emptyList<Entry>(), diffContentUpdates(listOf(entry), listOf(entry)))
    }

    @Test
    fun diffContentUpdatesIgnoresFreshEntriesWithNoExistingMatch() {
        val existing = Entry(id = 1, text = "青", meanings = listOf("blue"), romaji = "ao", level = Level.N5)
        val newVocab = Entry(text = "新しい", meanings = listOf("new"), romaji = "atarashii", level = Level.N5)

        assertEquals(emptyList<Entry>(), diffContentUpdates(listOf(existing), listOf(existing, newVocab)))
    }
}
