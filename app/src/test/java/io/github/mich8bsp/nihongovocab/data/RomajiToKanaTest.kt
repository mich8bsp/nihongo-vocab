package io.github.mich8bsp.nihongovocab.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RomajiToKanaTest {
    @Test
    fun `plain word`() {
        assertEquals("あう", romajiToKana("au"))
    }

    @Test
    fun `doubled consonant is sokuon`() {
        assertEquals("きって", romajiToKana("kitte"))
    }

    @Test
    fun `tch spells sokuon before chi`() {
        assertEquals("まっち", romajiToKana("matchi"))
    }

    @Test
    fun `contracted sound yoon`() {
        assertEquals("きょうだい", romajiToKana("kyoudai"))
    }

    @Test
    fun `long vowel is just the next vowel mora`() {
        assertEquals("がっこう", romajiToKana("gakkou"))
        assertEquals("おおきい", romajiToKana("ookii"))
    }

    @Test
    fun `apostrophe disambiguates n from the next mora`() {
        assertEquals("けんい", romajiToKana("ken'i"))
    }

    @Test
    fun `loanword combo not in the base kana chart`() {
        assertEquals("ふぉーむ", romajiToKana("foomu"))
    }

    @Test
    fun `non-letter separators pass through unchanged`() {
        assertEquals("こうこう; こうとうがっこう", romajiToKana("koukou; koutougakkou"))
    }
}
