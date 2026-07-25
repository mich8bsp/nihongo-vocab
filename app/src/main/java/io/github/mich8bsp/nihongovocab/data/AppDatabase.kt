package io.github.mich8bsp.nihongovocab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ponytail: exportSchema off - no migrations to track yet for a v1 personal app.
@Database(entities = [Entry::class, PoolState::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun poolStateDao(): PoolStateDao

    companion object {
        // Adds Entry.romaji (existing rows get "" - only newly-seeded entries
        // have it populated; acceptable since this only affects display of
        // an existing user's already-mastered/in-progress words).
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN romaji TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nihongo_vocab.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
