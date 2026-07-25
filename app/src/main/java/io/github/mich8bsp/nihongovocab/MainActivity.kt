package io.github.mich8bsp.nihongovocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.mich8bsp.nihongovocab.data.AppDatabase
import io.github.mich8bsp.nihongovocab.data.AssetSeeder
import io.github.mich8bsp.nihongovocab.data.Level

// TODO(part 5): replace this whole screen with the real Home screen.
// This is a temporary Part-2 verification stub - it just proves seeding
// populated Room correctly, visible without any UI having been built yet.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)

        setContent {
            var status by remember { mutableStateOf("Seeding...") }

            LaunchedEffect(Unit) {
                AssetSeeder(applicationContext, db).seedIfNeeded()
                val perLevel = Level.entries.map { level ->
                    "$level: ${db.entryDao().countUnmastered(level)} unmastered"
                }
                status = "DB entries: ${db.entryDao().count()}\n${perLevel.joinToString("\n")}"
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(status)
                }
            }
        }
    }
}
