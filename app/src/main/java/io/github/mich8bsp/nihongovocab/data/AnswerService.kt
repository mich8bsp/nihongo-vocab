package io.github.mich8bsp.nihongovocab.data

data class AnswerResult(val correct: Boolean, val meanings: List<String>)

fun isCorrectAnswer(entry: Entry, answer: String): Boolean {
    val normalized = answer.trim().lowercase()
    return entry.meanings.any { it.trim().lowercase() == normalized }
}

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

    suspend fun submitAnswer(entryId: Long, answer: String): AnswerResult {
        val entry = entryDao.getById(entryId) ?: error("Entry $entryId not found")
        val correct = isCorrectAnswer(entry, answer)

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
