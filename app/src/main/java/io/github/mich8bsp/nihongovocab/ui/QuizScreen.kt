package io.github.mich8bsp.nihongovocab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.mich8bsp.nihongovocab.data.AnswerResult
import io.github.mich8bsp.nihongovocab.data.AnswerService
import io.github.mich8bsp.nihongovocab.data.Entry
import io.github.mich8bsp.nihongovocab.data.Level
import io.github.mich8bsp.nihongovocab.data.hasKanji
import io.github.mich8bsp.nihongovocab.data.isCorrectAnswer
import io.github.mich8bsp.nihongovocab.data.isRomajiAnswer
import io.github.mich8bsp.nihongovocab.data.meaningsWithRomaji
import io.github.mich8bsp.nihongovocab.data.romajiToKana
import kotlinx.coroutines.launch

private const val DISABLED_ALPHA = 0.4f

@Composable
fun QuizScreen(
    entryId: Long,
    answerService: AnswerService,
    multipleChoice: Boolean,
    kanaHintEnabled: Boolean,
    onBack: () -> Unit,
    onNext: (Long) -> Unit,
) {
    var entry by remember(entryId) { mutableStateOf<Entry?>(null) }
    var options by remember(entryId) { mutableStateOf<List<String>>(emptyList()) }
    var stage1Passed by remember(entryId) { mutableStateOf(false) }
    var kanaRevealed by remember(entryId) { mutableStateOf(false) }
    var readingText by remember(entryId) { mutableStateOf("") }
    var readingIncorrect by remember(entryId) { mutableStateOf(false) }
    var answerText by remember(entryId) { mutableStateOf("") }
    var result by remember(entryId) { mutableStateOf<AnswerResult?>(null) }
    var nextMessage by remember(entryId) { mutableStateOf<String?>(null) }
    var romajiHint by remember(entryId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val readingFocusRequester = remember(entryId) { FocusRequester() }

    BackHandler(onBack = onBack)

    LaunchedEffect(entryId) {
        val loaded = answerService.getEntry(entryId)
        entry = loaded
        if (loaded != null) {
            // KANA, and any word with no kanji, has no separate reading stage.
            if (loaded.level == Level.KANA || !loaded.hasKanji()) stage1Passed = true
            if (multipleChoice) options = answerService.buildQuizOptions(loaded)
        }
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

            val currentResult = result
            val showKanaHint = kanaHintEnabled && currentEntry.level != Level.KANA && currentEntry.hasKanji() && currentResult == null
            Text(currentEntry.text, style = MaterialTheme.typography.displayLarge)
            if (showKanaHint && kanaRevealed) {
                Spacer(Modifier.height(4.dp))
                Text(romajiToKana(currentEntry.romaji), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(24.dp))

            if (currentResult == null) {
                val twoStage = currentEntry.level != Level.KANA && currentEntry.hasKanji()
                if (twoStage) {
                    LaunchedEffect(currentEntry.id) { readingFocusRequester.requestFocus() }
                    Column(
                        modifier = Modifier.alpha(if (stage1Passed) DISABLED_ALPHA else 1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        OutlinedTextField(
                            value = readingText,
                            onValueChange = {
                                readingText = it
                                readingIncorrect = false
                            },
                            label = { Text("Reading (romaji)") },
                            singleLine = true,
                            enabled = !stage1Passed,
                            modifier = Modifier.focusRequester(readingFocusRequester),
                        )
                        if (readingIncorrect) {
                            Spacer(Modifier.height(8.dp))
                            Text("Incorrect - try again", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    if (isRomajiAnswer(currentEntry, readingText)) {
                                        stage1Passed = true
                                    } else {
                                        readingIncorrect = true
                                    }
                                },
                                enabled = !stage1Passed && readingText.isNotBlank(),
                            ) {
                                Text("Submit")
                            }
                            if (showKanaHint && !kanaRevealed) {
                                OutlinedButton(onClick = { kanaRevealed = true }) {
                                    Text("Hint")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        result = answerService.giveUp(entryId)
                                    }
                                },
                                enabled = !stage1Passed,
                            ) {
                                Text("Give Up")
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                Column(
                    modifier = Modifier.alpha(if (stage1Passed) 1f else DISABLED_ALPHA),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (multipleChoice) {
                        options.forEach { option ->
                            Button(
                                onClick = {
                                    scope.launch {
                                        result = answerService.submitAnswer(entryId, option)
                                    }
                                },
                                enabled = stage1Passed,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Text(option)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = answerText,
                            onValueChange = {
                                answerText = it
                                romajiHint = false
                            },
                            label = { Text("Your answer") },
                            singleLine = true,
                            enabled = stage1Passed,
                        )
                        if (romajiHint) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "That's the romaji reading - try the English meaning",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    if (!isCorrectAnswer(currentEntry, answerText) &&
                                        isRomajiAnswer(currentEntry, answerText)
                                    ) {
                                        romajiHint = true
                                    } else {
                                        scope.launch {
                                            result = answerService.submitAnswer(entryId, answerText)
                                        }
                                    }
                                },
                                enabled = stage1Passed && answerText.isNotBlank(),
                            ) {
                                Text("Submit")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        result = answerService.giveUp(entryId)
                                    }
                                },
                                enabled = stage1Passed,
                            ) {
                                Text("Give Up")
                            }
                        }
                    }
                }
            } else {
                Text(if (currentResult.correct) "Correct!" else "Incorrect")
                Spacer(Modifier.height(8.dp))
                Text("Correct answer: ${currentEntry.meaningsWithRomaji()}")
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
