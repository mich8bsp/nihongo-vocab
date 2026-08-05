package io.github.mich8bsp.nihongovocab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    multipleChoiceMode: Boolean,
    onMultipleChoiceModeChange: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    kanaHintEnabled: Boolean,
    onKanaHintEnabledChange: (Boolean) -> Unit,
    reverseQuizEnabled: Boolean,
    onReverseQuizEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Multiple choice quiz", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = multipleChoiceMode, onCheckedChange = onMultipleChoiceModeChange)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Notifications", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsEnabledChange)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Kana reading hint",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = kanaHintEnabled, onCheckedChange = onKanaHintEnabledChange)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Reverse quiz (English → Japanese)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = reverseQuizEnabled, onCheckedChange = onReverseQuizEnabledChange)
        }
    }
}
