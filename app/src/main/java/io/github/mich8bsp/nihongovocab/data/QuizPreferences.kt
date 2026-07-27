package io.github.mich8bsp.nihongovocab.data

import android.content.Context

private const val PREFS_NAME = "quiz_settings"
private const val KEY_MULTIPLE_CHOICE = "multiple_choice_mode"

/** Persisted quiz-mode toggle (free text vs. multiple choice), set from HomeScreen. */
object QuizPreferences {
    fun isMultipleChoiceEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_MULTIPLE_CHOICE, false)

    fun setMultipleChoiceEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_MULTIPLE_CHOICE, enabled)
            .apply()
    }
}
