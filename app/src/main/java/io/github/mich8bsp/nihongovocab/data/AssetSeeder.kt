package io.github.mich8bsp.nihongovocab.data

import android.content.Context
import androidx.room.withTransaction
import org.json.JSONArray

private val ASSET_BY_LEVEL = mapOf(
    Level.KANA to "vocab/kana.json",
    Level.N5 to "vocab/n5.json",
    Level.N4 to "vocab/n4.json",
    Level.N3 to "vocab/n3.json",
    Level.N2 to "vocab/n2.json",
    Level.N1 to "vocab/n1.json",
)

/**
 * Populates Room from bundled asset JSON on first launch. On later launches,
 * instead re-syncs `meanings`/`romaji` for already-seeded entries against the
 * (possibly since-corrected) bundled data, matched by `text` - keeps `id`/
 * `correctStreak`/totals untouched, so a data-generation fix (e.g. a broken
 * meaning split) reaches existing installs without wiping progress. Doesn't
 * add newly-appeared vocab to an existing install - that needs a fresh seed.
 */
class AssetSeeder(private val context: Context, private val db: AppDatabase) {
    suspend fun seedIfNeeded() {
        if (db.entryDao().count() == 0) {
            // Read assets outside the transaction, then insert everything
            // atomically - if the process dies mid-seed, entries and
            // pool_state must never end up out of sync with each other, or
            // this count()-based guard would skip re-seeding forever.
            val entriesByLevel = ASSET_BY_LEVEL.map { (level, assetPath) ->
                val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
                parseEntries(json, level)
            }

            db.withTransaction {
                entriesByLevel.forEach { db.entryDao().insertAll(it) }
                db.poolStateDao().insertAll(
                    Level.entries.map { level ->
                        PoolState(level = level, enabled = level == Level.KANA || level == Level.N5)
                    },
                )
            }
            return
        }

        val updates = ASSET_BY_LEVEL.flatMap { (level, assetPath) ->
            val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            diffContentUpdates(db.entryDao().getAllForLevel(level), parseEntries(json, level))
        }
        if (updates.isNotEmpty()) {
            db.withTransaction { db.entryDao().updateAll(updates) }
        }
    }
}

/**
 * [existing] entries whose bundled `meanings`/`romaji` have since been
 * corrected (matched by `text`) - everything else about the entry (`id`,
 * `correctStreak`, totals) is preserved. A [fresh] entry with no matching
 * `text` in [existing] is ignored (new vocab isn't added outside a fresh
 * seed).
 */
internal fun diffContentUpdates(existing: List<Entry>, fresh: List<Entry>): List<Entry> {
    val freshByText = fresh.associateBy { it.text }
    return existing.mapNotNull { entry ->
        val freshEntry = freshByText[entry.text] ?: return@mapNotNull null
        if (freshEntry.meanings == entry.meanings && freshEntry.romaji == entry.romaji) {
            null
        } else {
            entry.copy(meanings = freshEntry.meanings, romaji = freshEntry.romaji)
        }
    }
}

internal fun parseEntries(json: String, level: Level): List<Entry> {
    val array = JSONArray(json)
    return (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        val meaningsArr = obj.getJSONArray("meanings")
        val meanings = (0 until meaningsArr.length()).map { meaningsArr.getString(it) }
        val romaji = obj.optString("romaji", "")
        Entry(text = obj.getString("text"), meanings = meanings, romaji = romaji, level = level)
    }
}
