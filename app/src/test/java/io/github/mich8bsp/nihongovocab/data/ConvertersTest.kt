package io.github.mich8bsp.nihongovocab.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun meaningsRoundTripThroughString() {
        val meanings = listOf("to eat", "to have (a meal)")
        val serialized = converters.meaningsToString(meanings)
        assertEquals(meanings, converters.stringToMeanings(serialized))
    }

    @Test
    fun emptyMeaningsRoundTrip() {
        assertEquals(emptyList<String>(), converters.stringToMeanings(converters.meaningsToString(emptyList())))
    }

    @Test
    fun singleMeaningRoundTrip() {
        assertEquals(listOf("blue"), converters.stringToMeanings(converters.meaningsToString(listOf("blue"))))
    }

    @Test
    fun levelRoundTripsForEveryValue() {
        for (level in Level.entries) {
            assertEquals(level, converters.stringToLevel(converters.levelToString(level)))
        }
    }
}
