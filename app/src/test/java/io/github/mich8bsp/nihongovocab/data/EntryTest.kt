package io.github.mich8bsp.nihongovocab.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EntryTest {
    @Test
    fun `kana entry has no romaji suffix since the meaning is already the romaji`() {
        val entry = Entry(text = "あ", meanings = listOf("a"), romaji = "", level = Level.KANA)
        assertEquals("a", entry.meaningsWithRomaji())
    }

    @Test
    fun `vocab entry appends romaji in parens`() {
        val entry = Entry(text = "大変", meanings = listOf("hard", "difficult"), romaji = "taihen", level = Level.N5)
        assertEquals("hard, difficult (taihen)", entry.meaningsWithRomaji())
    }

    @Test
    fun `vocab entry with blank romaji has no suffix`() {
        val entry = Entry(text = "秋", meanings = listOf("fall"), romaji = "", level = Level.N5)
        assertEquals("fall", entry.meaningsWithRomaji())
    }
}
