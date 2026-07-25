package io.github.mich8bsp.nihongovocab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// ponytail: exportSchema off - no migrations to track yet for a v1 personal app.
@Database(entities = [Entry::class, PoolState::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun poolStateDao(): PoolStateDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nihongo_vocab.db",
                ).build().also { instance = it }
            }
    }
}
