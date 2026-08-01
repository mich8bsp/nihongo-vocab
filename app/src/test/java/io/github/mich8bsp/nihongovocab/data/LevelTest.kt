package io.github.mich8bsp.nihongovocab.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LevelTest {
    @Test
    fun kanaHasNoNext() {
        assertNull(Level.KANA.next())
    }

    @Test
    fun n1HasNoNext() {
        assertNull(Level.N1.next())
    }

    @Test
    fun jlptChainAdvancesEasiestToHardest() {
        assertEquals(Level.N4, Level.N5.next())
        assertEquals(Level.N3, Level.N4.next())
        assertEquals(Level.N2, Level.N3.next())
        assertEquals(Level.N1, Level.N2.next())
    }

    @Test
    fun customHasNoNext() {
        assertNull(Level.CUSTOM.next())
    }
}
