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
}
