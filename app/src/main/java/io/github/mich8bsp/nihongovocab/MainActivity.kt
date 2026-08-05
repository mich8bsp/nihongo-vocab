package io.github.mich8bsp.nihongovocab

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.mich8bsp.nihongovocab.data.AnswerService
import io.github.mich8bsp.nihongovocab.data.AppDatabase
import io.github.mich8bsp.nihongovocab.data.AssetSeeder
import io.github.mich8bsp.nihongovocab.data.QuizPreferences
import io.github.mich8bsp.nihongovocab.notification.QuizAlarmReceiver
import io.github.mich8bsp.nihongovocab.ui.HomeScreen
import io.github.mich8bsp.nihongovocab.ui.MyVocabularyScreen
import io.github.mich8bsp.nihongovocab.ui.QuizScreen
import io.github.mich8bsp.nihongovocab.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    private var quizEntryId by mutableStateOf<Long?>(null)
    private var multipleChoiceMode by mutableStateOf(false)
    private var notificationsEnabled by mutableStateOf(true)
    private var kanaHintEnabled by mutableStateOf(false)
    private var reverseQuizMode by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)
    private var showMyVocabulary by mutableStateOf(false)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        quizEntryId = extractEntryId(intent)
        multipleChoiceMode = QuizPreferences.isMultipleChoiceEnabled(applicationContext)
        notificationsEnabled = QuizPreferences.isNotificationsEnabled(applicationContext)
        kanaHintEnabled = QuizPreferences.isKanaHintEnabled(applicationContext)
        reverseQuizMode = QuizPreferences.isReverseQuizEnabled(applicationContext)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (getSystemService<AlarmManager>()?.canScheduleExactAlarms() == false) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", packageName, null)),
            )
        }
        QuizAlarmReceiver.ensureScheduled(applicationContext)

        val db = AppDatabase.getInstance(applicationContext)
        val answerService = AnswerService(db.entryDao(), db.poolStateDao())

        setContent {
            var seeded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                AssetSeeder(applicationContext, db).seedIfNeeded()
                seeded = true
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val entryId = quizEntryId
                    when {
                        !seeded -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Seeding...") }
                        entryId != null -> QuizScreen(
                            entryId = entryId,
                            answerService = answerService,
                            multipleChoice = multipleChoiceMode,
                            kanaHintEnabled = kanaHintEnabled,
                            reverseQuiz = reverseQuizMode,
                            onBack = { quizEntryId = null },
                            onNext = { id -> quizEntryId = id },
                        )
                        showSettings -> SettingsScreen(
                            multipleChoiceMode = multipleChoiceMode,
                            onMultipleChoiceModeChange = { enabled ->
                                QuizPreferences.setMultipleChoiceEnabled(applicationContext, enabled)
                                multipleChoiceMode = enabled
                            },
                            notificationsEnabled = notificationsEnabled,
                            onNotificationsEnabledChange = { enabled ->
                                QuizAlarmReceiver.setEnabled(applicationContext, enabled)
                                notificationsEnabled = enabled
                            },
                            kanaHintEnabled = kanaHintEnabled,
                            onKanaHintEnabledChange = { enabled ->
                                QuizPreferences.setKanaHintEnabled(applicationContext, enabled)
                                kanaHintEnabled = enabled
                            },
                            reverseQuizEnabled = reverseQuizMode,
                            onReverseQuizEnabledChange = { enabled ->
                                QuizPreferences.setReverseQuizEnabled(applicationContext, enabled)
                                reverseQuizMode = enabled
                            },
                            onBack = { showSettings = false },
                        )
                        showMyVocabulary -> MyVocabularyScreen(
                            entryDao = db.entryDao(),
                            onBack = { showMyVocabulary = false },
                        )
                        else -> HomeScreen(
                            entryDao = db.entryDao(),
                            poolStateDao = db.poolStateDao(),
                            onPractice = { id -> quizEntryId = id },
                            onOpenSettings = { showSettings = true },
                            onOpenMyVocabulary = { showMyVocabulary = true },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        quizEntryId = extractEntryId(intent)
    }

    companion object {
        const val EXTRA_ENTRY_ID = "entry_id"

        private fun extractEntryId(intent: Intent): Long? =
            intent.getLongExtra(EXTRA_ENTRY_ID, -1L).takeIf { it >= 0 }
    }
}
