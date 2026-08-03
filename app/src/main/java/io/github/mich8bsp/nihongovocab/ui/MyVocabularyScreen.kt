package io.github.mich8bsp.nihongovocab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.mich8bsp.nihongovocab.data.Entry
import io.github.mich8bsp.nihongovocab.data.EntryDao
import io.github.mich8bsp.nihongovocab.data.Level
import kotlinx.coroutines.launch

@Composable
fun MyVocabularyScreen(entryDao: EntryDao, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    var entries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var expandedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        entries = entryDao.getAllForLevel(Level.CUSTOM)
    }

    fun edit(index: Int, updated: Entry) {
        entries = entries.toMutableList().also { it[index] = updated }
        scope.launch { entryDao.update(updated) }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("My Vocabulary", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                scope.launch {
                    val id = entryDao.insert(Entry(text = "", meanings = listOf(""), romaji = "", level = Level.CUSTOM))
                    entries = entries + Entry(id = id, text = "", meanings = listOf(""), romaji = "", level = Level.CUSTOM)
                    expandedIds = expandedIds + id
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add word")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        val filteredEntries = entries.filter { entry ->
            query.isBlank() ||
                entry.romaji.contains(query, ignoreCase = true) ||
                entry.meanings.any { it.contains(query, ignoreCase = true) }
        }

        LazyColumn(Modifier.weight(1f).padding(top = 8.dp).imePadding()) {
            items(filteredEntries, key = { it.id }) { entry ->
                val index = entries.indexOf(entry)
                val expanded = entry.id in expandedIds

                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedIds = if (expanded) expandedIds - entry.id else expandedIds + entry.id
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            entry.romaji.ifBlank { "(new word)" },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                        )
                    }

                    if (expanded) {
                        OutlinedTextField(
                            value = entry.text,
                            onValueChange = { edit(index, entry.copy(text = it)) },
                            label = { Text("Word") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = entry.romaji,
                            onValueChange = { edit(index, entry.copy(romaji = it)) },
                            label = { Text("Romaji") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = entry.meanings.firstOrNull() ?: "",
                            onValueChange = { edit(index, entry.copy(meanings = listOf(it))) },
                            label = { Text("Meaning") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = entry.comment,
                            onValueChange = { edit(index, entry.copy(comment = it)) },
                            label = { Text("Comment") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        )
                        TextButton(
                            onClick = {
                                entries = entries.filterNot { it.id == entry.id }
                                expandedIds = expandedIds - entry.id
                                scope.launch { entryDao.delete(entry) }
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text("Delete")
                        }
                    }

                    HorizontalDivider()
                }
            }
        }
    }
}
