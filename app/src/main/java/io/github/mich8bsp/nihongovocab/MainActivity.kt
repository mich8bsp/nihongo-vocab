package io.github.mich8bsp.nihongovocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
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
import io.github.mich8bsp.nihongovocab.data.AnswerService
import io.github.mich8bsp.nihongovocab.data.AppDatabase
import io.github.mich8bsp.nihongovocab.data.AssetSeeder
import io.github.mich8bsp.nihongovocab.ui.QuizScreen

// TODO(part 5/7): replace this whole screen with the real Home screen +
// Navigation graph. This is a temporary verification stub - it seeds the
// DB, then shows the QuizScreen for a real seeded entry (id 1, the first
// kana entry) so it's visible/testable before Home/nav exist.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val answerService = AnswerService(db.entryDao(), db.poolStateDao())

        setContent {
            var seeded by remember { mutableStateOf(false) }
            var showQuiz by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                AssetSeeder(applicationContext, db).seedIfNeeded()
                seeded = true
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        !seeded -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Seeding...") }
                        showQuiz -> QuizScreen(
                            entryId = 1L,
                            answerService = answerService,
                            onBack = { showQuiz = false },
                        )
                        else -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Button(onClick = { showQuiz = true }) {
                                Text("Home placeholder (Part 5) - tap to quiz again")
                            }
                        }
                    }
                }
            }
        }
    }
}
