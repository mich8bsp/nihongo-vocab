package io.github.mich8bsp.nihongovocab.data

data class AnswerResult(val correct: Boolean, val meanings: List<String>)

fun isCorrectAnswer(entry: Entry, answer: String): Boolean {
    val normalized = answer.trim().lowercase()
    return entry.meanings.any { meaning ->
        val normalizedMeaning = meaning.trim().lowercase()
        normalizedMeaning == normalized || stripParenthetical(normalizedMeaning) == normalized
    }
}

/**
 * Drops "(...)" context clarifications, e.g. "mother (formal)" -> "mother",
 * "(my) older brother (humble)" -> "older brother" - source glosses use
 * these for nuance/grammar hints, not as part of the required answer.
 */
private fun stripParenthetical(meaning: String): String =
    meaning.replace(Regex("\\(.*?\\)"), "").replace(Regex("\\s+"), " ").trim()

/**
 * True if [answer] is this entry's romaji reading rather than its English
 * meaning - the instinct when seeing kanji is sometimes to type the
 * reading, not translate it. Always false for KANA entries (`romaji` is
 * blank there - the meaning itself already is the romaji).
 */
fun isRomajiAnswer(entry: Entry, answer: String): Boolean =
    entry.romaji.isNotBlank() && entry.romaji.trim().lowercase() == answer.trim().lowercase()

/** Same selection notifications use - shared so "Practise" behaves identically to a real notification tap. */
suspend fun pickRandomActiveEntry(entryDao: EntryDao, poolStateDao: PoolStateDao): Entry? {
    val enabledLevels = poolStateDao.getEnabledLevels()
    return entryDao.getRandomActiveEntry(enabledLevels)
}

class AnswerService(
    private val entryDao: EntryDao,
    private val poolStateDao: PoolStateDao,
) {
    suspend fun getEntry(entryId: Long): Entry? = entryDao.getById(entryId)

    /** Same selection a notification/the Home "Practice" button would make. */
    suspend fun pickNext(): Entry? = pickRandomActiveEntry(entryDao, poolStateDao)

    suspend fun submitAnswer(entryId: Long, answer: String): AnswerResult {
        val entry = entryDao.getById(entryId) ?: error("Entry $entryId not found")
        return recordResult(entry, correct = isCorrectAnswer(entry, answer))
    }

    /** User gave up instead of typing - always recorded as wrong, same as an incorrect answer. */
    suspend fun giveUp(entryId: Long): AnswerResult {
        val entry = entryDao.getById(entryId) ?: error("Entry $entryId not found")
        return recordResult(entry, correct = false)
    }

    private suspend fun recordResult(entry: Entry, correct: Boolean): AnswerResult {
        val updated = if (correct) {
            entry.copy(
                correctStreak = minOf(entry.correctStreak + 1, 3),
                totalCorrect = entry.totalCorrect + 1,
            )
        } else {
            entry.copy(correctStreak = 0, totalWrong = entry.totalWrong + 1)
        }
        entryDao.update(updated)

        if (updated.correctStreak >= 3) {
            advancePoolIfComplete(entry.level)
        }

        return AnswerResult(correct = correct, meanings = entry.meanings)
    }

    private suspend fun advancePoolIfComplete(level: Level) {
        if (entryDao.countUnmastered(level) > 0) return
        poolStateDao.setEnabled(level, false)
        level.next()?.let { poolStateDao.setEnabled(it, true) }
    }
}
