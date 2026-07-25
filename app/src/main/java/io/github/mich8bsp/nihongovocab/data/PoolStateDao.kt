package io.github.mich8bsp.nihongovocab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PoolStateDao {
    @Insert
    suspend fun insertAll(states: List<PoolState>)

    @Query("SELECT COUNT(*) FROM pool_state")
    suspend fun count(): Int

    @Query("SELECT level FROM pool_state WHERE enabled = 1")
    suspend fun getEnabledLevels(): List<Level>

    @Query("UPDATE pool_state SET enabled = :enabled WHERE level = :level")
    suspend fun setEnabled(level: Level, enabled: Boolean)
}
