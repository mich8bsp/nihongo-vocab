package io.github.mich8bsp.nihongovocab.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `entry with kanji has kanji`() {
        val entry = Entry(text = "大変", meanings = listOf("hard"), romaji = "taihen", level = Level.N5)
        assertTrue(entry.hasKanji())
    }

    @Test
    fun `entry written entirely in kana has no kanji`() {
        val entry = Entry(text = "ありがとう", meanings = listOf("thank you"), romaji = "arigatou", level = Level.N5)
        assertFalse(entry.hasKanji())
    }

    @Test
    fun `display text falls back to romaji when text is blank`() {
        val entry = Entry(text = "", meanings = listOf("dog"), romaji = "inu", level = Level.CUSTOM)
        assertEquals("inu", entry.displayText())
    }

    @Test
    fun `display text uses text when present`() {
        val entry = Entry(text = "犬", meanings = listOf("dog"), romaji = "inu", level = Level.CUSTOM)
        assertEquals("犬", entry.displayText())
    }

    @Test
    fun `text with romaji falls back to romaji alone when text is blank`() {
        val entry = Entry(text = "", meanings = listOf("dog"), romaji = "inu", level = Level.CUSTOM)
        assertEquals("inu", entry.textWithRomaji())
    }

    @Test
    fun `text with romaji appends romaji in parens when text is present`() {
        val entry = Entry(text = "犬", meanings = listOf("dog"), romaji = "inu", level = Level.N5)
        assertEquals("犬 (inu)", entry.textWithRomaji())
    }
}
