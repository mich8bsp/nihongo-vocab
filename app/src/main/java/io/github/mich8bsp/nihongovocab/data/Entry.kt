package io.github.mich8bsp.nihongovocab.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val meanings: List<String>,
    val romaji: String = "",
    val level: Level,
    val correctStreak: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val comment: String = "",
)

/**
 * Meanings joined for display, with a "(romaji)" suffix for non-KANA
 * entries - KANA's meaning is already the romaji reading, so it'd be
 * redundant there.
 */
fun Entry.meaningsWithRomaji(): String {
    val romajiSuffix = if (level != Level.KANA && romaji.isNotBlank()) " ($romaji)" else ""
    return meanings.joinToString(", ") + romajiSuffix
}

/** Whether [text] contains a CJK ideograph - words written entirely in kana have no reading stage. */
fun Entry.hasKanji(): Boolean = text.any { it.code in 0x4E00..0x9FFF }
