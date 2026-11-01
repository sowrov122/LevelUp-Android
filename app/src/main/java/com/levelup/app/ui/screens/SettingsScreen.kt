package com.levelup.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.domain.TimeUtils
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.NotificationPermissionCard
import com.levelup.app.ui.components.SectionTitle
import com.levelup.app.ui.viewmodel.SettingsViewModel
import java.time.LocalDate

@Composable
fun TimeField(label: String, minute: Int, modifier: Modifier = Modifier, onValid: (Int) -> Unit) {
    var text by remember(minute) { mutableStateOf(TimeUtils.format(minute)) }
    val parsed = TimeUtils.parseHhMm(text)
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            TimeUtils.parseHhMm(it)?.let(onValid)
        },
        label = { Text(label) },
        singleLine = true,
        isError = parsed == null,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val status by vm.statusMessage.collectAsStateWithLifecycle()
    var name by remember(s.displayName) { mutableStateOf(s.displayName) }
    var cap by remember(s.focusDailyCapMinutes) { mutableStateOf(s.focusDailyCapMinutes.toString()) }
    var confirmClear by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(uri)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        status?.let {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(it, Modifier.weight(1f).padding(vertical = 12.dp))
                    TextButton(onClick = { vm.consumeStatus() }) { Text("OK") }
                }
            }
        }

        SectionTitle("Profile")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = { vm.update { it.copy(displayName = name.trim()) } }) { Text("Save") }
        }

        SectionTitle("Reminders")
        NotificationPermissionCard()
        SwitchRow("Routine reminders", s.notificationsEnabled) { v -> vm.update { it.copy(notificationsEnabled = v) } }
        SwitchRow("Quiet hours (no reminders)", s.quietEnabled) { v -> vm.update { it.copy(quietEnabled = v) } }
        if (s.quietEnabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("From", s.quietStartMinute, Modifier.weight(1f)) { m -> vm.update { it.copy(quietStartMinute = m) } }
                TimeField("Until", s.quietEndMinute, Modifier.weight(1f)) { m -> vm.update { it.copy(quietEndMinute = m) } }
            }
        }
        Text(
            "Reminders use inexact alarms (no exact-alarm permission), so they can arrive a few minutes late when " +
                "battery saving is on. Reminders are rebuilt after reboot, time-zone or date changes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("Focus & visuals")
        SwitchRow("Reduced motion / low-power visuals", s.reducedMotion) { v -> vm.update { it.copy(reducedMotion = v) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                cap, { cap = it.filter(Char::isDigit).take(4) },
                label = { Text("Daily focus reward cap (minutes)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { cap.toIntOrNull()?.let { c -> vm.update { it.copy(focusDailyCapMinutes = c.coerceIn(0, 1440)) } } }) { Text("Save") }
        }

        SectionTitle("Your data")
        Text(
            "Everything stays on this device. No account, no analytics, no network permission.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { exportLauncher.launch("level-up-export-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) {
            Text("Export data (JSON)")
        }
        OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) { Text("Clear all data") }

        SectionTitle("Not included yet")
        Text(
            "• Selected-app blocking is not implemented. Android cannot reliably detect content inside other apps, " +
                "and it needs Usage Access / Accessibility permissions that this version does not request.\n" +
                "• Premium and Google Play Billing are not implemented; nothing here is locked or for sale.\n" +
                "• Sleep and break blocks are ordinary tasks for now.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            "Clear all data?",
            "This permanently deletes your routine, history, score ledger, rewards and settings, then restores the sample routine. Export first if you want a copy.",
            "Delete everything",
            onConfirm = { confirmClear = false; vm.clearAll() },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
