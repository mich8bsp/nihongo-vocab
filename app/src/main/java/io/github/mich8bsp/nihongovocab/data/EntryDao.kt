package io.github.mich8bsp.nihongovocab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EntryDao {
    @Insert
    suspend fun insertAll(entries: List<Entry>)

    @Update
    suspend fun update(entry: Entry)

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun count(): Int

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): Entry?

    @Query("SELECT * FROM entries WHERE level IN (:levels) AND correctStreak < 3 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomActiveEntry(levels: List<Level>): Entry?

    @Query("SELECT COUNT(*) FROM entries WHERE level = :level AND correctStreak < 3")
    suspend fun countUnmastered(level: Level): Int
}
