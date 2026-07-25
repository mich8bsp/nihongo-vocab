package io.github.mich8bsp.nihongovocab.data

import androidx.room.TypeConverter

/** U+001F unit separator - can't appear in an English meaning/gloss. */
private const val MEANINGS_DELIMITER = ""

class Converters {
    @TypeConverter
    fun levelToString(level: Level): String = level.name

    @TypeConverter
    fun stringToLevel(value: String): Level = Level.valueOf(value)

    @TypeConverter
    fun meaningsToString(meanings: List<String>): String = meanings.joinToString(MEANINGS_DELIMITER)

    @TypeConverter
    fun stringToMeanings(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(MEANINGS_DELIMITER)
}
