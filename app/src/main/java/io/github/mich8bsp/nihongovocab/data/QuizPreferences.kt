package io.github.mich8bsp.nihongovocab.data

import android.content.Context

private const val PREFS_NAME = "quiz_settings"
private const val KEY_MULTIPLE_CHOICE = "multiple_choice_mode"
private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
private const val KEY_KANA_HINT = "kana_hint_enabled"

/** Persisted quiz-mode/notification toggles, set from SettingsScreen. */
object QuizPreferences {
    fun isMultipleChoiceEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_MULTIPLE_CHOICE, false)

    fun setMultipleChoiceEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_MULTIPLE_CHOICE, enabled)
            .apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }

    fun isKanaHintEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_KANA_HINT, false)

    fun setKanaHintEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_KANA_HINT, enabled)
            .apply()
    }
}
