package io.github.mich8bsp.nihongovocab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

data class LevelStats(
    val level: Level,
    val totalCorrect: Int,
    val totalWrong: Int,
    val masteredCount: Int,
    val totalCount: Int,
)

@Dao
interface EntryDao {
    @Insert
    suspend fun insertAll(entries: List<Entry>)

    @Update
    suspend fun update(entry: Entry)

    @Update
    suspend fun updateAll(entries: List<Entry>)

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun count(): Int

    @Query("SELECT * FROM entries WHERE level = :level")
    suspend fun getAllForLevel(level: Level): List<Entry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): Entry?

    @Query("SELECT * FROM entries WHERE level IN (:levels) AND correctStreak < 3 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomActiveEntry(levels: List<Level>): Entry?

    /** Candidate distractors for multiple-choice quizzing - same level, any mastery state. */
    @Query("SELECT * FROM entries WHERE level = :level AND id != :excludeId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomOtherEntries(level: Level, excludeId: Long, limit: Int): List<Entry>

    @Query("SELECT COUNT(*) FROM entries WHERE level = :level AND correctStreak < 3")
    suspend fun countUnmastered(level: Level): Int

    @Query(
        """
        SELECT level,
               SUM(totalCorrect) AS totalCorrect,
               SUM(totalWrong) AS totalWrong,
               SUM(CASE WHEN correctStreak >= 3 THEN 1 ELSE 0 END) AS masteredCount,
               COUNT(*) AS totalCount
        FROM entries
        GROUP BY level
        """,
    )
    suspend fun getStatsByLevel(): List<LevelStats>
}
