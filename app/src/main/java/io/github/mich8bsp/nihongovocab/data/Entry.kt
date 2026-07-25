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
)
