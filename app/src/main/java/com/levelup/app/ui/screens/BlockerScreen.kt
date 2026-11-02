package com.levelup.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.levelup.app.ui.components.AppCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.domain.InterruptionAction
import com.levelup.app.domain.PopularApps
import com.levelup.app.service.UsageAccess
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.PrimaryButton
import com.levelup.app.ui.components.NotificationPermissionCard
import com.levelup.app.ui.components.SectionTitle
import com.levelup.app.ui.viewmodel.BlockerViewModel
import com.levelup.app.ui.viewmodel.InstalledApp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HISTORY_FORMAT = DateTimeFormatter.ofPattern("d MMM HH:mm")
private val Gold = Color(0xFFFFCE00)

@Composable
fun BlockerScreen(vm: BlockerViewModel, onStartFocus: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var explain by remember { mutableStateOf(false) }
    var confirmPause by remember { mutableStateOf<Int?>(null) }
    var penaltyText by remember(s.settings.blockerPenaltyPoints) { mutableStateOf(s.settings.blockerPenaltyPoints.toString()) }
    var cooldownText by remember(s.settings.blockerCooldownSeconds) { mutableStateOf(s.settings.blockerCooldownSeconds.toString()) }
    var coinsText by remember(s.settings.blockerPenaltyCoins) { mutableStateOf(s.settings.blockerPenaltyCoins.toString()) }
    var xpText by remember(s.settings.blockerPenaltyXp) { mutableStateOf(s.settings.blockerPenaltyXp.toString()) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.refreshAccess() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val pausedActive = s.settings.blockerPausedUntilMs > s.nowMs
    val q = s.query.trim().lowercase()
    val selectedPackages = s.selected.filter { it.isEnabled }.map { it.packageName }.toSet()
    val popular = s.installed.filter { PopularApps.isPopular(it.packageName) }
    val others = s.installed
        .filter { !PopularApps.isPopular(it.packageName) }
        .filter { q.isBlank() || it.label.lowercase().contains(q) }
        .sortedWith(compareByDescending<InstalledApp> { it.packageName in selectedPackages }.thenBy { it.label.lowercase() })

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Hero
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Shield(Modifier.size(84.dp))
                        Text("Stay Focused", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Choose the apps that pull you away. During a focus session, opening them costs you points.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Distraction protection", fontWeight = FontWeight.SemiBold)
                                Text(
                                    when {
                                        !s.settings.blockerEnabled -> "Off. Nothing is being observed."
                                        !s.usageAccess -> "On, waiting for Usage Access."
                                        pausedActive -> "Paused. No penalties for now."
                                        else -> "On. Active during focus sessions."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = s.settings.blockerEnabled,
                                onCheckedChange = { on -> if (on && !s.usageAccess) explain = true else vm.setEnabled(on) },
                            )
                        }
                    }
                }
            }

            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Usage Access", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (s.usageAccess) "Allowed in Android settings" else "Not allowed yet",
                                    color = if (s.usageAccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                )
                            }
                            OutlinedButton(onClick = { UsageAccess.open(context) }) { Text("Open settings") }
                        }
                        if (!s.usageAccess) {
                            Text(
                                "Can't turn it on? Installed from a file? Open App info, tap ⋮ and choose \"Allow restricted settings\", " +
                                    "then Settings → Apps → Special app access → Usage access → Level Up. On Xiaomi, Oppo, Vivo or Realme " +
                                    "it can also be under Privacy / Permissions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = { UsageAccess.openAppInfo(context) }) { Text("Open App info") }
                        }
                    }
                }
            }

            item { NotificationPermissionCard() }

            // Popular distractions
            if (popular.isNotEmpty()) {
                item { SectionTitle("Popular distractions on your phone") }
                items(popular, key = { "p-" + it.packageName }) { app ->
                    AppToggleRow(app, app.packageName in selectedPackages) { vm.toggleApp(app, it) }
                }
            }

            // Honest note instead of an "adult content" switch
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Adult content (18+)", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Android does not let any app see or recognise content inside other apps, so Level Up can't promise to detect it. " +
                                "What works: select your browsers and social apps here, and for websites use a content-filtering Private DNS " +
                                "(Settings → Network → Private DNS) or your browser's own safe-search controls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // All other apps
            item {
                SectionTitle("All apps (${selectedPackages.size} selected)")
                OutlinedTextField(
                    s.query, { vm.setQuery(it) }, label = { Text("Search apps") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                if (s.loadingApps) Text("Loading installed apps…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(others, key = { "a-" + it.packageName }) { app ->
                AppToggleRow(app, app.packageName in selectedPackages) { vm.toggleApp(app, it) }
            }

            // Strict mode
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Strict mode", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Switch(checked = s.settings.blockerStrict, onCheckedChange = { vm.setStrict(it) })
                        }
                        Text(
                            "On: opening ANY app other than Level Up, the home screen, phone, messages, maps, clock and system settings " +
                                "during a focus session costs you. Off: only the apps selected above count. " +
                                "Android does not let one app lock other apps, so this is a penalty, not a wall. " +
                                "For a harder lock use \"Lock phone to Level Up\" on the Focus screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Rules
            item {
                SectionTitle("Penalty rules")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        penaltyText, { penaltyText = it.filter(Char::isDigit).take(2) },
                        label = { Text("−Points") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        coinsText, { coinsText = it.filter(Char::isDigit).take(3) },
                        label = { Text("−Coins") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        xpText, { xpText = it.filter(Char::isDigit).take(3) },
                        label = { Text("−XP") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    cooldownText, { cooldownText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Cooldown between penalties (seconds)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    "Coins and XP never go below zero. Deducting XP can lower your level, so it is 0 by default. " +
                        "One launch costs at most one penalty; the same app is only logged until the cooldown ends (30–3600 s).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                OutlinedButton(onClick = {
                    vm.saveRules(
                        penaltyText.toIntOrNull() ?: 0, cooldownText.toIntOrNull() ?: 300,
                        coinsText.toIntOrNull() ?: 0, xpText.toIntOrNull() ?: 0,
                    )
                }) { Text("Save rules") }
            }

            // Pause
            item {
                SectionTitle("Pause / emergency")
                if (pausedActive) {
                    Text("Paused until ${HISTORY_FORMAT.format(Instant.ofEpochMilli(s.settings.blockerPausedUntilMs).atZone(ZoneId.systemDefault()))}")
                    OutlinedButton(onClick = { vm.resume() }) { Text("Resume protection now") }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 60).forEach { m ->
                            FilterChip(selected = false, onClick = { confirmPause = m }, label = { Text("Pause $m min") })
                        }
                    }
                    Text(
                        "Phone, messages, maps, clock, the home screen and system settings are never restricted, so you can always reach them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // History
            item { SectionTitle("Interruption history") }
            if (s.interruptions.isEmpty()) {
                item { Text("No interruptions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(s.interruptions, key = { it.id }) { e ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(e.displayName)
                        Text(
                            HISTORY_FORMAT.format(Instant.ofEpochMilli(e.detectedAt).atZone(ZoneId.systemDefault())),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        when (e.actionTaken) {
                            InterruptionAction.PENALIZED -> "−${e.pointsPenalty} pts"
                            InterruptionAction.LOGGED_ONLY -> "logged"
                            InterruptionAction.OVERRIDE -> "override"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        PrimaryButton(
            "Start Focus & Block",
            onClick = {
                if (!s.settings.blockerEnabled) {
                    if (!s.usageAccess) explain = true else { vm.setEnabled(true); onStartFocus() }
                } else {
                    onStartFocus()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
        )
    }

    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text("Allow Usage Access?") },
            text = {
                Text(
                    "To notice when a distracting app opens, Android requires you to allow \"Usage access\" for Level Up in its settings. " +
                        "Level Up reads only which app came to the front, only during a focus session. " +
                        "It does not read screen content, keystrokes or messages, and nothing is uploaded. " +
                        "You can revoke it anytime in Android settings.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    explain = false
                    vm.setEnabled(true)
                    UsageAccess.open(context)
                }) { Text("Continue to settings") }
            },
            dismissButton = { TextButton(onClick = { explain = false }) { Text("Not now") } },
        )
    }

    confirmPause?.let { minutes ->
        ConfirmDialog(
            "Pause protection for $minutes minutes?",
            "No points will be deducted while paused. The pause is recorded in your interruption history.",
            "Pause",
            onConfirm = { vm.pause(minutes); confirmPause = null },
            onDismiss = { confirmPause = null },
        )
    }
}

@Composable
private fun AppToggleRow(app: InstalledApp, checked: Boolean, onChange: (Boolean) -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val icon = app.icon
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
            } else {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.outline))
            }
            Text(app.label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun Shield(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier.semantics { contentDescription = "Shield" }) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.04f)
            lineTo(w * 0.9f, h * 0.2f)
            lineTo(w * 0.9f, h * 0.55f)
            lineTo(w * 0.5f, h * 0.97f)
            lineTo(w * 0.1f, h * 0.55f)
            lineTo(w * 0.1f, h * 0.2f)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(Gold, accent)), style = Stroke(width = 4.dp.toPx()))
        drawCircle(Gold, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.42f))
        drawRect(Gold, topLeft = Offset(w * 0.47f, h * 0.45f), size = androidx.compose.ui.geometry.Size(w * 0.06f, h * 0.2f))
    }
}
