package io.github.mich8bsp.nihongovocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import io.github.mich8bsp.nihongovocab.data.AppDatabase
import io.github.mich8bsp.nihongovocab.data.AssetSeeder
import io.github.mich8bsp.nihongovocab.ui.HomeScreen

// TODO(part 7): wire the real Navigation graph (Home <-> Quiz via
// notification tap). Home is the only real entry point until then -
// per DESIGN.md, Quiz is only ever reached via a notification tap, so
// there's nothing else for MainActivity to show yet.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)

        setContent {
            var seeded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                AssetSeeder(applicationContext, db).seedIfNeeded()
                seeded = true
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (seeded) {
                        HomeScreen(entryDao = db.entryDao(), poolStateDao = db.poolStateDao())
                    } else {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Seeding...") }
                    }
                }
            }
        }
    }
}
