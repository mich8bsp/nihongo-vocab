package io.github.mich8bsp.nihongovocab.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import io.github.mich8bsp.nihongovocab.data.EntryDao
import io.github.mich8bsp.nihongovocab.data.Level
import io.github.mich8bsp.nihongovocab.data.LevelStats
import io.github.mich8bsp.nihongovocab.data.PoolStateDao
import io.github.mich8bsp.nihongovocab.data.pickRandomActiveEntry
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    entryDao: EntryDao,
    poolStateDao: PoolStateDao,
    onPractice: (Long) -> Unit,
) {
    var statsByLevel by remember { mutableStateOf<Map<Level, LevelStats>>(emptyMap()) }
    var enabledByLevel by remember { mutableStateOf<Map<Level, Boolean>>(emptyMap()) }
    var loaded by remember { mutableStateOf(false) }
    var practiceMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        statsByLevel = entryDao.getStatsByLevel().associateBy { it.level }
        enabledByLevel = poolStateDao.getAll().associateBy({ it.level }, { it.enabled })
        loaded = true
    }

    LaunchedEffect(Unit) { reload() }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Nihongo Vocab", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                val entry = pickRandomActiveEntry(entryDao, poolStateDao)
                if (entry != null) {
                    onPractice(entry.id)
                } else {
                    practiceMessage = "Nothing to practice - enable a pool, or you've mastered everything in it"
                }
            }
        }) {
            Text("Practice")
        }
        practiceMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(Level.entries) { level ->
                val stats = statsByLevel[level]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(level.name, style = MaterialTheme.typography.titleMedium)
                        if (stats != null) {
                            Text(
                                "${stats.masteredCount}/${stats.totalCount} mastered · " +
                                    "${stats.totalCorrect} correct / ${stats.totalWrong} wrong",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Switch(
                        checked = enabledByLevel[level] ?: false,
                        onCheckedChange = { checked ->
                            scope.launch {
                                poolStateDao.setEnabled(level, checked)
                                reload()
                            }
                        },
                    )
                }
            }
        }
    }
}
