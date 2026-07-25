package io.github.mich8bsp.nihongovocab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import io.github.mich8bsp.nihongovocab.data.AnswerService
import io.github.mich8bsp.nihongovocab.data.AppDatabase
import io.github.mich8bsp.nihongovocab.data.AssetSeeder
import io.github.mich8bsp.nihongovocab.notification.QuizNotificationWorker
import io.github.mich8bsp.nihongovocab.ui.HomeScreen
import io.github.mich8bsp.nihongovocab.ui.QuizScreen

class MainActivity : ComponentActivity() {
    private var quizEntryId by mutableStateOf<Long?>(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        quizEntryId = extractEntryId(intent)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        QuizNotificationWorker.ensureScheduled(applicationContext)

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
                            onBack = { quizEntryId = null },
                            onNext = { id -> quizEntryId = id },
                        )
                        else -> HomeScreen(
                            entryDao = db.entryDao(),
                            poolStateDao = db.poolStateDao(),
                            onPractice = { id -> quizEntryId = id },
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
