package io.github.mich8bsp.nihongovocab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mich8bsp.nihongovocab.data.AnswerResult
import io.github.mich8bsp.nihongovocab.data.AnswerService
import io.github.mich8bsp.nihongovocab.data.Entry
import io.github.mich8bsp.nihongovocab.data.Level
import kotlinx.coroutines.launch

@Composable
fun QuizScreen(
    entryId: Long,
    answerService: AnswerService,
    onBack: () -> Unit,
    onNext: (Long) -> Unit,
) {
    var entry by remember(entryId) { mutableStateOf<Entry?>(null) }
    var answerText by remember(entryId) { mutableStateOf("") }
    var result by remember(entryId) { mutableStateOf<AnswerResult?>(null) }
    var nextMessage by remember(entryId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    LaunchedEffect(entryId) {
        entry = answerService.getEntry(entryId)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val currentEntry = entry
            if (currentEntry == null) {
                CircularProgressIndicator()
                return@Column
            }

            Text(currentEntry.text, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(24.dp))

            val currentResult = result
            if (currentResult == null) {
                OutlinedTextField(
                    value = answerText,
                    onValueChange = { answerText = it },
                    label = { Text("Your answer") },
                    singleLine = true,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                result = answerService.submitAnswer(entryId, answerText)
                            }
                        },
                        enabled = answerText.isNotBlank(),
                    ) {
                        Text("Submit")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                result = answerService.giveUp(entryId)
                            }
                        },
                    ) {
                        Text("Give Up")
                    }
                }
            } else {
                Text(if (currentResult.correct) "Correct!" else "Incorrect")
                Spacer(Modifier.height(8.dp))
                val romajiSuffix = if (currentEntry.level != Level.KANA && currentEntry.romaji.isNotBlank()) {
                    " (${currentEntry.romaji})"
                } else {
                    ""
                }
                Text("Correct answer: ${currentResult.meanings.joinToString(", ")}$romajiSuffix")
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    scope.launch {
                        val next = answerService.pickNext()
                        if (next != null) {
                            onNext(next.id)
                        } else {
                            nextMessage = "Nothing to practice - enable a pool, or you've mastered everything in it"
                        }
                    }
                }) {
                    Text("Next")
                }
                nextMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
