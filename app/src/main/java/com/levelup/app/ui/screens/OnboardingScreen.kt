package com.levelup.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.systemBarsPadding
import com.levelup.app.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(vm: OnboardingViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var quiet by rememberSaveable { mutableStateOf(false) }
    var bed by rememberSaveable { mutableIntStateOf(23 * 60 + 30) }
    var wake by rememberSaveable { mutableIntStateOf(6 * 60 + 30) }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Welcome to Level Up", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Build a daily routine, earn fair points and XP, and protect your focus. Everything works offline and " +
                "stays on this device. A sample routine is included and fully editable.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(name, { name = it }, label = { Text("What should we call you? (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mute reminders while I sleep", Modifier.weight(1f))
            Switch(checked = quiet, onCheckedChange = { quiet = it })
        }
        if (quiet) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("Bedtime", bed, Modifier.weight(1f)) { bed = it }
                TimeField("Wake-up", wake, Modifier.weight(1f)) { wake = it }
            }
        }
        Text(
            "Reminders need notification permission. We will only ask when you set up a reminder, and explain why first.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = { vm.finish(name, quiet, bed, wake) }, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
        TextButton(onClick = { vm.finish("", false, 23 * 60 + 30, 6 * 60 + 30) }, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }
    }
}
