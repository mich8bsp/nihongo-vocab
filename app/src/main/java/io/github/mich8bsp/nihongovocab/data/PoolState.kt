package io.github.mich8bsp.nihongovocab.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pool_state")
data class PoolState(
    @PrimaryKey val level: Level,
    val enabled: Boolean,
)
