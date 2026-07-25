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
}

private class FakePoolStateDao(initial: Map<Level, Boolean>) : PoolStateDao {
    val enabled = initial.toMutableMap()

    override suspend fun insertAll(states: List<PoolState>) {
        for (s in states) enabled[s.level] = s.enabled
    }

    override suspend fun count(): Int = enabled.size

    override suspend fun getEnabledLevels(): List<Level> = enabled.filterValues { it }.keys.toList()

    override suspend fun setEnabled(level: Level, enabled: Boolean) {
        this.enabled[level] = enabled
    }
}

class AnswerServiceTest {
    private fun entry(id: Long, level: Level, streak: Int = 0, meanings: List<String> = listOf("blue")) =
        Entry(id = id, text = "x", meanings = meanings, level = level, correctStreak = streak)

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
    fun answerMatchingIsCaseInsensitiveTrimmedAndAcceptsAnyMeaning() {
        val e = entry(1, Level.N5, meanings = listOf("to eat", "to have a meal"))
        assertTrue(isCorrectAnswer(e, "  TO EAT  "))
        assertTrue(isCorrectAnswer(e, "to have a meal"))
        assertFalse(isCorrectAnswer(e, "to drink"))
    }
}
