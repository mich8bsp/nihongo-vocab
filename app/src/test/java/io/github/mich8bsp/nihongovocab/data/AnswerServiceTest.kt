package io.github.mich8bsp.nihongovocab.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeEntryDao(initial: List<Entry>) : EntryDao {
    private val entries = initial.associateBy { it.id }.toMutableMap()

    override suspend fun insertAll(entries: List<Entry>) {
        for (e in entries) this.entries[e.id] = e
    }

    override suspend fun update(entry: Entry) {
        entries[entry.id] = entry
    }

    override suspend fun count(): Int = entries.size

    override suspend fun getById(id: Long): Entry? = entries[id]

    override suspend fun getRandomActiveEntry(levels: List<Level>): Entry? =
        entries.values.filter { it.level in levels && it.correctStreak < 3 }.randomOrNull()

    override suspend fun countUnmastered(level: Level): Int =
        entries.values.count { it.level == level && it.correctStreak < 3 }

    override suspend fun getStatsByLevel(): List<LevelStats> =
        entries.values.groupBy { it.level }.map { (level, es) ->
            LevelStats(
                level = level,
                totalCorrect = es.sumOf { it.totalCorrect },
                totalWrong = es.sumOf { it.totalWrong },
                masteredCount = es.count { it.correctStreak >= 3 },
                totalCount = es.size,
            )
        }
}

private class FakePoolStateDao(initial: Map<Level, Boolean>) : PoolStateDao {
    val enabled = initial.toMutableMap()

    override suspend fun insertAll(states: List<PoolState>) {
        for (s in states) enabled[s.level] = s.enabled
    }

    override suspend fun count(): Int = enabled.size

    override suspend fun getEnabledLevels(): List<Level> = enabled.filterValues { it }.keys.toList()

    override suspend fun getAll(): List<PoolState> = enabled.map { (level, e) -> PoolState(level, e) }

    override suspend fun setEnabled(level: Level, enabled: Boolean) {
        this.enabled[level] = enabled
    }
}

class AnswerServiceTest {
    private fun entry(
        id: Long,
        level: Level,
        streak: Int = 0,
        meanings: List<String> = listOf("blue"),
        romaji: String = "",
    ) = Entry(id = id, text = "x", meanings = meanings, romaji = romaji, level = level, correctStreak = streak)

    @Test
    fun correctAnswerIncrementsStreakAndTotalCorrect() = runTest {
        val entryDao = FakeEntryDao(listOf(entry(1, Level.N5)))
        val service = AnswerService(entryDao, FakePoolStateDao(mapOf(Level.N5 to true)))

        val result = service.submitAnswer(1, " Blue ")

        assertTrue(result.correct)
        val updated = entryDao.getById(1)!!
        assertEquals(1, updated.correctStreak)
        assertEquals(1, updated.totalCorrect)
        assertEquals(0, updated.totalWrong)
    }

    @Test
    fun wrongAnswerResetsStreakAndIncrementsTotalWrong() = runTest {
        val entryDao = FakeEntryDao(listOf(entry(1, Level.N5, streak = 2)))
        val service = AnswerService(entryDao, FakePoolStateDao(mapOf(Level.N5 to true)))

        val result = service.submitAnswer(1, "wrong")

        assertFalse(result.correct)
        val updated = entryDao.getById(1)!!
        assertEquals(0, updated.correctStreak)
        assertEquals(1, updated.totalWrong)
    }

    @Test
    fun streakDoesNotExceedThree() = runTest {
        // shouldn't normally happen (mastered entries aren't re-quizzed) but the cap should hold anyway
        val entryDao = FakeEntryDao(listOf(entry(1, Level.N5, streak = 3)))
        val service = AnswerService(entryDao, FakePoolStateDao(mapOf(Level.N5 to true)))

        service.submitAnswer(1, "blue")

        assertEquals(3, entryDao.getById(1)!!.correctStreak)
    }

    @Test
    fun poolCompletesAndAdvancesToNextWhenLastEntryMastered() = runTest {
        val entryDao = FakeEntryDao(listOf(entry(1, Level.N5, streak = 2)))
        val poolDao = FakePoolStateDao(mapOf(Level.N5 to true, Level.N4 to false))
        val service = AnswerService(entryDao, poolDao)

        service.submitAnswer(1, "blue")

        assertFalse(poolDao.enabled[Level.N5]!!)
        assertTrue(poolDao.enabled[Level.N4]!!)
    }

    @Test
    fun poolNotCompleteYetLeavesPoolStateAlone() = runTest {
        val entryDao = FakeEntryDao(listOf(entry(1, Level.N5, streak = 2), entry(2, Level.N5, streak = 0)))
        val poolDao = FakePoolStateDao(mapOf(Level.N5 to true, Level.N4 to false))
        val service = AnswerService(entryDao, poolDao)

        service.submitAnswer(1, "blue")

        assertTrue(poolDao.enabled[Level.N5]!!)
        assertFalse(poolDao.enabled[Level.N4]!!)
    }

    @Test
    fun kanaCompletionHasNoNextPoolToTouch() = runTest {
        val entryDao = FakeEntryDao(listOf(entry(1, Level.KANA, streak = 2)))
        val poolDao = FakePoolStateDao(mapOf(Level.KANA to true))
        val service = AnswerService(entryDao, poolDao)

        service.submitAnswer(1, "blue")

        assertFalse(poolDao.enabled[Level.KANA]!!)
        assertEquals(setOf(Level.KANA), poolDao.enabled.keys)
    }

    @Test
    fun giveUpAlwaysRecordsAsWrongRegardlessOfStreak() = runTest {
        val entryDao = FakeEntryDao(listOf(entry(1, Level.N5, streak = 2)))
        val service = AnswerService(entryDao, FakePoolStateDao(mapOf(Level.N5 to true)))

        val result = service.giveUp(1)

        assertFalse(result.correct)
        val updated = entryDao.getById(1)!!
        assertEquals(0, updated.correctStreak)
        assertEquals(1, updated.totalWrong)
        assertEquals(0, updated.totalCorrect)
    }

    @Test
    fun answerMatchingIsCaseInsensitiveTrimmedAndAcceptsAnyMeaning() {
        val e = entry(1, Level.N5, meanings = listOf("to eat", "to have a meal"))
        assertTrue(isCorrectAnswer(e, "  TO EAT  "))
        assertTrue(isCorrectAnswer(e, "to have a meal"))
        assertFalse(isCorrectAnswer(e, "to drink"))
    }

    @Test
    fun answerWithoutParentheticalClarificationIsAccepted() {
        val e = entry(1, Level.N5, meanings = listOf("mother (formal)"))
        assertTrue(isCorrectAnswer(e, "mother"))
        assertTrue(isCorrectAnswer(e, "  Mother (formal) "))
        assertFalse(isCorrectAnswer(e, "formal"))
    }

    @Test
    fun answerWithoutLeadingParentheticalIsAccepted() {
        val e = entry(1, Level.N5, meanings = listOf("(my) older brother (humble)"))
        assertTrue(isCorrectAnswer(e, "older brother"))
        assertFalse(isCorrectAnswer(e, "my"))
    }

    @Test
    fun romajiAnswerIsDetectedCaseInsensitiveAndTrimmed() {
        val e = entry(1, Level.N5, meanings = listOf("to eat"), romaji = "taberu")
        assertTrue(isRomajiAnswer(e, "  TABERU  "))
        assertFalse(isRomajiAnswer(e, "to eat"))
        assertFalse(isRomajiAnswer(e, "tabemasu"))
    }

    @Test
    fun romajiAnswerIsNeverTrueWhenRomajiIsBlank() {
        // KANA entries have no romaji field (their meaning already is the romaji)
        val e = entry(1, Level.KANA, meanings = listOf("a"), romaji = "")
        assertFalse(isRomajiAnswer(e, "a"))
        assertFalse(isRomajiAnswer(e, ""))
    }
}
