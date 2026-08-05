package io.github.mich8bsp.nihongovocab.data

data class AnswerResult(val correct: Boolean, val meanings: List<String>)

fun isCorrectAnswer(entry: Entry, answer: String): Boolean {
    val normalizedAnswer = digitizeNumberWords(answer.trim().lowercase())
    return entry.meanings.any { meaning ->
        meaningVariants(meaning.trim().lowercase()).any { digitizeNumberWords(it) == normalizedAnswer }
    }
}

/** All phrasings of a single meaning gloss that should count as a correct answer. */
private fun meaningVariants(meaning: String): List<String> {
    val base = listOf(
        meaning,
        stripParenthetical(meaning),
        unwrapParenthetical(meaning),
        substituteMidParenthetical(meaning),
    )
    return base + base.map(::stripLeadingArticle)
}

/**
 * Drops "(...)" context clarifications, e.g. "mother (formal)" -> "mother",
 * "(my) older brother (humble)" -> "older brother" - source glosses use
 * these for nuance/grammar hints, not as part of the required answer.
 */
private fun stripParenthetical(meaning: String): String =
    meaning.replace(Regex("\\(.*?\\)"), "").replace(Regex("\\s+"), " ").trim()

/**
 * Keeps the parenthetical's content but drops the parens themselves, e.g.
 * "to take off (clothes)" -> "to take off clothes" - some glosses use
 * parens around a word that's still part of a natural answer, not just a
 * dropped clarification.
 */
private fun unwrapParenthetical(meaning: String): String =
    meaning.replace(Regex("[()]"), "").replace(Regex("\\s+"), " ").trim()

/**
 * Replaces a "word (altword)" pair with just the altword, but only when
 * more text follows, e.g. "clear (sunny) weather" -> "sunny weather" -
 * here the parenthetical is an alternate word choice, not a trailing note
 * (a trailing "word (note)" like "mother (formal)" is left alone, since
 * "note" alone isn't a valid answer).
 */
private fun substituteMidParenthetical(meaning: String): String =
    meaning.replace(Regex("\\S+\\s*\\(([^)]+)\\)(?=\\s+\\S)"), "$1")

/**
 * Drops a leading "a"/"an"/"the", e.g. "an exit" -> "exit" - source
 * glosses often include the article, but requiring it from the user adds
 * nothing to whether they knew the word.
 */
private fun stripLeadingArticle(meaning: String): String =
    meaning.replace(Regex("^(a|an|the)\\s+"), "")

private val NUMBER_WORDS = mapOf(
    "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5",
    "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
    "eleven" to "11", "twelve" to "12", "thirteen" to "13", "fourteen" to "14",
    "fifteen" to "15", "sixteen" to "16", "seventeen" to "17", "eighteen" to "18",
    "nineteen" to "19", "twenty" to "20", "thirty" to "30", "forty" to "40",
    "fifty" to "50", "sixty" to "60", "seventy" to "70", "eighty" to "80",
    "ninety" to "90",
)

/**
 * Normalizes spelled-out numbers to digits, e.g. "twenty days" -> "20
 * days" - applied to both sides of a comparison, so it also normalizes a
 * digit meaning against a spelled-out answer ("20 years old" vs "twenty
 * years old").
 */
private fun digitizeNumberWords(text: String): String =
    NUMBER_WORDS.entries.fold(text) { acc, (word, digit) -> acc.replace(Regex("\\b$word\\b"), digit) }

/**
 * True if [answer] is this entry's romaji reading rather than its English
 * meaning - the instinct when seeing kanji is sometimes to type the
 * reading, not translate it. Always false for KANA entries (`romaji` is
 * blank there - the meaning itself already is the romaji).
 */
fun isRomajiAnswer(entry: Entry, answer: String): Boolean =
    entry.romaji.isNotBlank() && entry.romaji.trim().lowercase() == answer.trim().lowercase()

/**
 * Reverse-quiz check: true if [answer] matches this entry's Japanese side,
 * either the word/kana itself or its romaji reading - typing kanji without
 * a JP IME often isn't practical, so the reading is accepted too.
 */
fun isCorrectJapaneseAnswer(entry: Entry, answer: String): Boolean {
    val normalized = answer.trim()
    if (normalized.isEmpty()) return false
    return normalized.equals(entry.displayText().trim(), ignoreCase = true) ||
        (entry.romaji.isNotBlank() && normalized.equals(entry.romaji.trim(), ignoreCase = true))
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

    /** Same selection a notification/the Home "Practice" button would make. */
    suspend fun pickNext(): Entry? = pickRandomActiveEntry(entryDao, poolStateDao)

    /**
     * 3 shuffled answer options for multiple-choice mode: [entry]'s own first
     * meaning (or, in [reverse] mode, its Japanese word) plus 2 distractors
     * from other same-level entries. Distractors that happen to equal one of
     * [entry]'s own options are dropped (over-fetches a few extra candidates
     * to allow for this) so an option is never ambiguously "also correct".
     */
    suspend fun buildQuizOptions(entry: Entry, reverse: Boolean = false): List<String> {
        val correctAnswer = if (reverse) entry.displayText() else entry.meanings.first()
        val ownAnswers = if (reverse) {
            setOf(correctAnswer.trim().lowercase())
        } else {
            entry.meanings.map { it.trim().lowercase() }.toSet()
        }
        val distractors = entryDao.getRandomOtherEntries(entry.level, entry.id, limit = 6)
            .map { if (reverse) it.displayText() else it.meanings.first() }
            .filter { it.trim().lowercase() !in ownAnswers }
            .distinct()
            .take(2)
        return (listOf(correctAnswer) + distractors).shuffled()
    }

    suspend fun submitAnswer(entryId: Long, answer: String, reverse: Boolean = false): AnswerResult {
        val entry = entryDao.getById(entryId) ?: error("Entry $entryId not found")
        val correct = if (reverse) isCorrectJapaneseAnswer(entry, answer) else isCorrectAnswer(entry, answer)
        return recordResult(entry, correct = correct)
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
