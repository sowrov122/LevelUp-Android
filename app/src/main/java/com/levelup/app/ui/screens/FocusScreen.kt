package com.levelup.app.ui.screens

import android.app.Activity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.data.FocusSessionEntity
import com.levelup.app.domain.Category
import com.levelup.app.domain.Observatory
import com.levelup.app.domain.ProtectionStatus
import com.levelup.app.domain.ScoringEngine
import com.levelup.app.domain.StationProgress
import com.levelup.app.service.UsageAccess
import com.levelup.app.ui.components.AppCard
import com.levelup.app.ui.components.AppChip
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.IconBadge
import com.levelup.app.ui.components.IconTile
import com.levelup.app.ui.components.PrimaryButton
import com.levelup.app.ui.components.ProgressBar
import com.levelup.app.ui.components.SectionHeader
import com.levelup.app.ui.components.appSwitchColors
import com.levelup.app.ui.components.formatClock
import com.levelup.app.ui.components.formatMinutes
import com.levelup.app.ui.components.icon
import com.levelup.app.ui.theme.AppColors
import com.levelup.app.ui.theme.Art
import com.levelup.app.ui.viewmodel.ActiveFocusUi
import com.levelup.app.ui.viewmodel.FocusUiState
import com.levelup.app.ui.viewmodel.FocusViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val RingViolet = AppColors.FocusViolet

// ============================================================ entry point

@Composable
fun FocusScreen(
    vm: FocusViewModel,
    onOpenBlocker: () -> Unit,
    onImmersive: (Boolean) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenProgress: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val active = state.active
    val context = LocalContext.current
    val view = LocalView.current

    var title by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var lockScreen by rememberSaveable { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var usageAccess by remember { mutableStateOf(UsageAccess.isGranted(context)) }
    val category = categoryName?.let { n -> Category.entries.firstOrNull { it.name == n } }

    // Usage Access can be changed in Android settings; re-check whenever the screen comes back.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) usageAccess = UsageAccess.isGranted(context) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // Hide the bottom bar and keep the screen on while a session runs; release screen pinning afterwards.
    LaunchedEffect(active != null) {
        onImmersive(active != null)
        if (active == null) runCatching { (context as? Activity)?.stopLockTask() }
    }
    DisposableEffect(active != null) {
        view.keepScreenOn = active != null
        onDispose {
            view.keepScreenOn = false
            onImmersive(false)
        }
    }

    if (active != null) {
        FocusRunningContent(
            state = state,
            active = active,
            category = category,
            usageAccess = usageAccess,
            onPause = { vm.pause(active.id) },
            onResume = { vm.resume(active.id) },
            onFinish = { vm.finish(active.id) },
            onCancel = { vm.cancel(active.id) },
        )
    } else {
        FocusIdleContent(
            state = state,
            title = title, onTitle = { title = it },
            minutes = minutes, onMinutes = { minutes = it },
            category = category, onCategory = { categoryName = it?.name },
            usageAccess = usageAccess,
            lockScreen = lockScreen, onLockScreen = { lockScreen = it },
            onStart = {
                if (lockScreen) runCatching { (context as? Activity)?.startLockTask() }
                vm.start(title, category, minutes)
            },
            onProtection = vm::setProtection,
            onOpenBlocker = onOpenBlocker,
            onOpenProgress = onOpenProgress,
            onOpenHistory = { showHistory = true },
            onOpenSettings = onOpenSettings,
            onEditMinutes = { showCustom = true },
            onDismissOutcome = vm::dismissOutcome,
        )
    }

    if (showHistory) HistoryDialog(state.recent) { showHistory = false }
    if (showCustom) CustomMinutesDialog(minutes, onDismiss = { showCustom = false }, onSet = { minutes = it; showCustom = false })
}

// ============================================================== idle view

/** Stateless idle screen. Everything shown comes from [state] (saved data) or the parameters. */
@Composable
fun FocusIdleContent(
    state: FocusUiState,
    title: String,
    onTitle: (String) -> Unit,
    minutes: Int,
    onMinutes: (Int) -> Unit,
    category: Category?,
    onCategory: (Category?) -> Unit,
    usageAccess: Boolean,
    lockScreen: Boolean,
    onLockScreen: (Boolean) -> Unit,
    onStart: () -> Unit,
    onProtection: (Boolean) -> Unit,
    onOpenBlocker: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditMinutes: () -> Unit,
    onDismissOutcome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(AppColors.Background)) {
        CosmicBackdrop(state.reducedMotion, Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).testTag("focus_idle")
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FocusHeader(onOpenProgress, onOpenHistory, onOpenSettings)

            FocusScene(
                stageIndex = Art.StationStages.lastIndex,   // before a session the station is shown complete
                ringFraction = (minutes / 120f).coerceIn(0.04f, 1f),
                ringDescription = "Selected focus length $minutes minutes",
                owned = state.owned,
                reducedMotion = state.reducedMotion,
            ) {
                CategorySelector(category, onCategory)
                Spacer(Modifier.height(8.dp))
                TitlePill(title, onTitle)
                Text(
                    formatClock(minutes * 60L),
                    modifier = Modifier.clickable(onClickLabel = "Set a custom length", onClick = onEditMinutes).testTag("focus_time"),
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Light,
                    color = AppColors.TextPrimary,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.HourglassEmpty, null, tint = AppColors.TextSecondary, modifier = Modifier.size(18.dp))
                    Text("Focus Session", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                listOf(15, 25, 45, 60).forEach { m -> AppChip("$m min", minutes == m, { onMinutes(m) }, selectedColor = AppColors.FocusViolet) }
            }

            PrimaryButton("Start Focus", onStart, Modifier.fillMaxWidth().testTag("start_focus"), icon = Icons.Filled.PlayArrow, containerColor = AppColors.FocusViolet)

            state.lastOutcome?.let { OutcomeCard(it, state.minutes, onDismissOutcome) }

            ProtectionCard(state, usageAccess, onProtection, onOpenBlocker)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lock phone to Level Up", color = AppColors.TextPrimary)
                    Text(
                        "Optional. Uses Android screen pinning so other apps can't be opened. Android may ask you to confirm; " +
                            "hold Back + Overview to leave. The session keeps running.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
                Switch(checked = lockScreen, onCheckedChange = onLockScreen, colors = appSwitchColors())
            }

            StationMilestoneCard(state, minutes)

            SectionHeader("Station unlocks")
            Observatory.milestones.forEach { m ->
                val done = Observatory.isUnlocked(m, state.sessions, state.minutes)
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(m.title, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                        Text(m.description, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                    Text(if (done) "Unlocked" else "Locked", color = if (done) AppColors.Success else AppColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FocusHeader(onProgress: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Filled.TrackChanges, null, tint = AppColors.TextPrimary, modifier = Modifier.size(34.dp))
        Column(Modifier.weight(1f)) {
            Text("Focus Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Text("Deep Work • No Distraction • Level Up", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 1)
        }
        IconTile(Icons.Filled.BarChart, "Open progress", onProgress)
        IconTile(Icons.Filled.History, "Focus history", onHistory)
        IconTile(Icons.Filled.Settings, "Settings", onSettings)
    }
}

@Composable
private fun CategorySelector(category: Category?, onCategory: (Category?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(AppColors.Elevated.copy(alpha = 0.85f))
                .border(1.dp, AppColors.Border, RoundedCornerShape(50))
                .clickable(onClickLabel = "Choose a category") { open = true }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("category_selector"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (category != null) Icon(category.icon(), null, tint = Color(category.colorHex), modifier = Modifier.size(18.dp))
            Text(category?.label ?: "No category", color = AppColors.TextPrimary, fontWeight = FontWeight.Medium)
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = AppColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("No category") }, onClick = { open = false; onCategory(null) })
            Category.entries.forEach { c ->
                DropdownMenuItem(text = { Text(c.label) }, onClick = { open = false; onCategory(c) })
            }
        }
    }
}

@Composable
private fun TitlePill(title: String, onTitle: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(50))
            .background(AppColors.Card.copy(alpha = 0.9f))
            .border(1.dp, AppColors.Border, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = title,
            onValueChange = { onTitle(it.take(60)) },
            singleLine = true,
            textStyle = TextStyle(color = AppColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (title.isEmpty()) Text("Add session title…", color = AppColors.TextSecondary, fontSize = 16.sp)
                    inner()
                }
            },
            modifier = Modifier.weight(1f).testTag("session_title"),
        )
        Icon(Icons.Filled.Edit, contentDescription = "Edit title", tint = AppColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun OutcomeCard(o: com.levelup.app.domain.FocusOutcome, totalMinutes: Int, onDismiss: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (o.completed) {
                Text("Session complete", style = MaterialTheme.typography.titleMedium, color = AppColors.Success, fontWeight = FontWeight.SemiBold)
                Text("${o.focusedMinutes} min focused · +${o.reward.points} points · +${o.reward.xp} XP · +${o.reward.coins} coins", color = AppColors.TextPrimary)
                val next = StationProgress.nextThreshold(totalMinutes)
                Text(
                    if (next != null) "Station: $totalMinutes / $next min to Lv. ${StationProgress.level(totalMinutes) + 1}" else "Station: maximum level reached",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
                if (o.cappedByDailyLimit) {
                    Text("Daily reward cap reached: only ${o.rewardedMinutes} min were rewarded.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            } else {
                Text("Session too short", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                Text("Sessions under ${ScoringEngine.MIN_FOCUS_MINUTES} minutes are not rewarded.", color = AppColors.TextSecondary)
            }
            TextButton(onClick = onDismiss) { Text("Dismiss", color = AppColors.ProgressBlue) }
        }
    }
}

@Composable
private fun ProtectionCard(state: FocusUiState, usageAccess: Boolean, onProtection: (Boolean) -> Unit, onOpenBlocker: () -> Unit) {
    val status = ProtectionStatus.state(state.protectionOn, usageAccess, state.strictMode)
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(icon = Icons.Filled.Shield, contentDescription = "Distraction protection")
            Column(Modifier.weight(1f)) {
                Text("Distraction Protection", fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                Text(ProtectionStatus.subtitle(status), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
            Switch(checked = state.protectionOn, onCheckedChange = onProtection, colors = appSwitchColors(), modifier = Modifier.testTag("protection_switch"))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = "Manage apps", onClick = onOpenBlocker).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Manage Apps", fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                Text("Select apps that cost points", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun StationMilestoneCard(state: FocusUiState, minutes: Int) {
    val totalMinutes = state.minutes
    val level = StationProgress.level(totalMinutes)
    val next = StationProgress.nextThreshold(totalMinutes)
    val preview = ScoringEngine.calculateFocusReward(minutes * 60L, state.dailyCapMinutes, state.rewardedMinutesToday)
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    painterResource(Art.StationStages[StationProgress.stageIndex(totalMinutes)]),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(AppColors.Elevated).border(2.dp, AppColors.Lavender, CircleShape),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Station Milestone", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text(
                            "Lv. $level",
                            fontSize = 12.sp, color = AppColors.TextPrimary,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(AppColors.Lavender).padding(horizontal = 10.dp, vertical = 3.dp).testTag("station_level"),
                        )
                    }
                    Text("Every focused minute upgrades your station", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    ProgressBar(StationProgress.fractionToNext(totalMinutes), height = 8.dp)
                    Text(
                        if (next != null) "$totalMinutes / $next min" else "$totalMinutes min · max level",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.testTag("station_sessions"),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PreviewStat(Modifier.weight(1f), { Icon(Icons.Filled.Star, null, tint = AppColors.RewardGold, modifier = Modifier.size(22.dp)) }, "+${preview.reward.xp} XP", "Per session")
                PreviewStat(Modifier.weight(1f), { Image(painterResource(Art.Coin), null, Modifier.size(24.dp)) }, "+${preview.reward.coins} Coins", "Per session")
                PreviewStat(Modifier.weight(1f), { Icon(Icons.Filled.Schedule, null, tint = AppColors.TextSecondary, modifier = Modifier.size(22.dp)) }, "+$minutes min", "Focus time")
                PreviewStat(Modifier.weight(1f), { Icon(Icons.Filled.BarChart, null, tint = AppColors.ProgressBlue, modifier = Modifier.size(22.dp)) }, "+${StationProgress.percentForSession(totalMinutes, minutes)}%", "Station")
            }
            val note = when {
                minutes * 60L < ScoringEngine.MIN_FOCUS_SECONDS -> "Sessions under ${ScoringEngine.MIN_FOCUS_MINUTES} minutes earn nothing."
                preview.rewardedMinutes < minutes ->
                    if (preview.rewardedMinutes == 0) "Daily reward cap reached: this session would earn nothing." else "Daily reward cap: only ${preview.rewardedMinutes} min would be rewarded."
                else -> null
            }
            if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, modifier = Modifier.testTag("reward_note"))
        }
    }
}

@Composable
private fun PreviewStat(modifier: Modifier, icon: @Composable () -> Unit, value: String, label: String) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, maxLines = 1)
        Text(label, fontSize = 10.sp, color = AppColors.TextSecondary, maxLines = 1)
    }
}

// ============================================================ running view

@Composable
fun FocusRunningContent(
    state: FocusUiState,
    active: ActiveFocusUi,
    category: Category?,
    usageAccess: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEnd by remember { mutableStateOf(false) }
    val progress = if (active.plannedSeconds <= 0) 0f else (active.elapsedSeconds.toFloat() / active.plannedSeconds).coerceIn(0f, 1f)
    val status = ProtectionStatus.state(state.protectionOn, usageAccess, state.strictMode)

    Box(modifier.fillMaxSize().background(AppColors.Background)) {
        CosmicBackdrop(state.reducedMotion, Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(16.dp).testTag("focus_running"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(active.title, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            if (category != null) Text(category.label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            FocusScene(
                stageIndex = StationProgress.runningStageIndex(state.minutes, active.elapsedSeconds),
                ringFraction = progress,
                ringDescription = "Session ${(progress * 100).toInt()} percent complete",
                owned = state.owned,
                reducedMotion = state.reducedMotion,
            ) {
                Spacer(Modifier.height(36.dp))
                Text(formatClock(active.remainingSeconds), fontSize = 68.sp, fontWeight = FontWeight.Light, color = AppColors.TextPrimary, modifier = Modifier.testTag("countdown"))
                Text(
                    if (active.running) "Building your station · ${(progress * 100).toInt()}%" else "Paused",
                    color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                ProtectionStatus.subtitle(status),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            if (active.interrupted) {
                Text(
                    "The phone restarted, so the timer was paused instead of guessing the time.",
                    style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                CircleButton(onClick = { showEnd = true }, description = "End session") {
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
                }
                CircleButton(onClick = { if (active.running) onPause() else onResume() }, description = if (active.running) "Pause" else "Resume") {
                    if (active.running) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.size(width = 6.dp, height = 22.dp).background(Color.White))
                            Box(Modifier.size(width = 6.dp, height = 22.dp).background(Color.White))
                        }
                    } else {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }

    if (showEnd) {
        val canCollect = active.elapsedSeconds >= ScoringEngine.MIN_FOCUS_SECONDS
        AlertDialog(
            onDismissRequest = { showEnd = false },
            containerColor = AppColors.Elevated,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("End this session?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (canCollect) "Finish now to collect rewards for the minutes you focused, or give up and earn nothing."
                        else "Less than ${ScoringEngine.MIN_FOCUS_MINUTES} minutes are done, so finishing now earns nothing. Keep going or give up.",
                    )
                    TextButton(onClick = { showEnd = false; onCancel() }) { Text("Give up (no reward)", color = AppColors.Danger) }
                }
            },
            confirmButton = { TextButton(onClick = { showEnd = false; onFinish() }, enabled = canCollect) { Text("Finish & collect", color = AppColors.ProgressBlue) } },
            dismissButton = { TextButton(onClick = { showEnd = false }) { Text("Keep going", color = AppColors.TextSecondary) } },
        )
    }
}

@Composable
private fun CircleButton(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .border(2.dp, Color(0x66F5EBDD), CircleShape)
            .clickable(onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ============================================================ dialogs

@Composable
private fun CustomMinutesDialog(current: Int, onDismiss: () -> Unit, onSet: (Int) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    val value = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Elevated,
        title = { Text("Focus length") },
        text = {
            OutlinedTextField(
                text, { text = it.filter(Char::isDigit).take(3) },
                label = { Text("Minutes (${ScoringEngine.MIN_FOCUS_MINUTES}–180)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = value == null || value !in ScoringEngine.MIN_FOCUS_MINUTES..180,
            )
        },
        confirmButton = { TextButton(onClick = { value?.let(onSet) }, enabled = value != null && value in ScoringEngine.MIN_FOCUS_MINUTES..180) { Text("Set", color = AppColors.ProgressBlue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } },
    )
}

private val HISTORY_FORMAT = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@Composable
private fun HistoryDialog(sessions: List<FocusSessionEntity>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Elevated,
        title = { Text("Focus history") },
        text = {
            if (sessions.isEmpty()) {
                Text("No completed sessions yet. Your first one will show up here.", color = AppColors.TextSecondary)
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sessions.forEach { s ->
                        Column {
                            Text(s.title, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                            val whenText = s.endedAt?.let { HISTORY_FORMAT.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) } ?: s.localDate
                            Text(
                                "${formatMinutes((s.accumulatedSeconds / 60).toInt())} · +${s.xpAwarded} XP · +${s.coinsAwarded} coins · $whenText",
                                style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = AppColors.ProgressBlue) } },
    )
}

// ============================================================ the scene

@Composable
private fun sceneMotion(reduced: Boolean): Pair<Float, Float> {
    return if (reduced) {
        0f to 0.9f
    } else {
        val t = rememberInfiniteTransition(label = "scene")
        val bob by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
        val orbit by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "orbit")
        bob to orbit
    }
}

/** Full-screen night sky: the supplied artwork plus a light layer of twinkling stars. */
@Composable
private fun CosmicBackdrop(reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val twinkle = if (reducedMotion) 1f else {
        rememberInfiniteTransition(label = "stars").animateFloat(0.4f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "twinkle").value
    }
    val stars = remember {
        val r = java.util.Random(5L)
        List(34) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.7f, 0.5f + r.nextFloat() * 1.1f) }
    }
    Box(modifier) {
        Image(
            painterResource(Art.FocusBackground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Canvas(Modifier.fillMaxSize()) {
            stars.forEachIndexed { i, s ->
                val a = (if (i % 2 == 0) twinkle else 1.4f - twinkle).coerceIn(0.15f, 1f)
                drawCircle(Color.White.copy(alpha = a * 0.7f), radius = s[2] * density, center = Offset(s[0] * size.width, s[1] * size.height))
            }
        }
    }
}

/**
 * The living centrepiece: progress ring, floating station, an orbiting satellite and a planet.
 * Owned shop items replace the simple defaults with their illustrations. Motion stops with "reduce motion".
 */
@Composable
private fun FocusScene(
    stageIndex: Int,
    ringFraction: Float,
    ringDescription: String,
    owned: Set<String>,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    overlay: @Composable ColumnScope.() -> Unit,
) {
    val (bob, orbit) = sceneMotion(reducedMotion)
    BoxWithConstraints(modifier.fillMaxWidth().height(500.dp)) {
        val w = maxWidth
        if ("obs_nebula" in owned) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width * 0.3f, size.height * 0.25f)
                drawCircle(Brush.radialGradient(listOf(Color(0x66A070FF), Color.Transparent), center = c, radius = size.width * 0.5f), radius = size.width * 0.5f, center = c)
            }
        }
        // progress ring
        Canvas(Modifier.size(300.dp).align(Alignment.TopCenter).semantics { contentDescription = ringDescription }) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val inset = 8.dp.toPx()
            val arc = Size(size.width - 2 * inset, size.height - 2 * inset)
            drawArc(AppColors.Lavender.copy(alpha = 0.35f), 0f, 360f, false, Offset(inset, inset), arc, style = stroke)
            drawArc(RingViolet, -90f, 360f * ringFraction.coerceIn(0f, 1f), false, Offset(inset, inset), arc, style = stroke)
        }
        // orbit path
        Canvas(Modifier.fillMaxSize()) {
            val ry = 80.dp.toPx()
            drawOval(
                Color.White.copy(alpha = 0.14f), Offset(size.width * 0.04f, 150.dp.toPx() - ry), Size(size.width * 0.92f, 2 * ry),
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f))),
            )
        }
        // station (stages cross-fade as a session progresses or the station levels up)
        Crossfade(
            targetState = stageIndex.coerceIn(0, Art.StationStages.lastIndex),
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 178.dp + (bob * 5).dp).size(310.dp),
            animationSpec = tween(900),
            label = "station",
        ) { idx ->
            Image(painterResource(Art.StationStages[idx]), contentDescription = "Your space station", modifier = Modifier.fillMaxSize())
        }
        // planet
        Box(Modifier.align(Alignment.TopEnd).padding(top = 36.dp, end = 2.dp).size(76.dp).offset(y = (bob * 3).dp)) {
            val planet = when {
                "obs_ringed_planet" in owned -> Art.shopItem("obs_ringed_planet")
                "obs_new_planet" in owned -> Art.shopItem("obs_new_planet")
                else -> null
            }
            if (planet != null) Image(painterResource(planet), null, Modifier.fillMaxSize())
            else Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.width * 0.42f
                drawCircle(Brush.radialGradient(listOf(Color(0xFFB07A8C), Color(0xFF4A2A44)), center = c, radius = r), radius = r, center = c)
            }
        }
        // satellite on its orbit
        val sat = 44.dp
        val sx = w / 2 + (w * 0.46f) * cos(orbit)
        val sy = 150.dp + 80.dp * sin(orbit)
        Box(Modifier.align(Alignment.TopStart).offset(sx - sat / 2, sy - sat / 2).size(sat)) {
            val art = if ("obs_satellite" in owned) Art.shopItem("obs_satellite") else null
            if (art != null) Image(painterResource(art), null, Modifier.fillMaxSize())
            else Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFFCFC6BB), Offset(size.width * 0.38f, size.height * 0.38f), Size(size.width * 0.24f, size.height * 0.24f))
                drawRect(Color(0xFF6C8EBF), Offset(size.width * 0.02f, size.height * 0.44f), Size(size.width * 0.32f, size.height * 0.12f))
                drawRect(Color(0xFF6C8EBF), Offset(size.width * 0.66f, size.height * 0.44f), Size(size.width * 0.32f, size.height * 0.12f))
            }
        }
        // other owned sky objects and station extensions
        if ("obs_moon" in owned) Image(painterResource(requireNotNull(Art.shopItem("obs_moon"))), null, Modifier.align(Alignment.TopStart).padding(top = 20.dp, start = 4.dp).size(38.dp))
        if ("obs_comet" in owned) Image(painterResource(requireNotNull(Art.shopItem("obs_comet"))), null, Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 90.dp).size(56.dp))
        if ("obs_shuttle" in owned) Image(painterResource(requireNotNull(Art.shopItem("obs_shuttle"))), null, Modifier.align(Alignment.CenterStart).offset(y = 60.dp + (bob * 4).dp).size(52.dp))
        if ("obs_starship" in owned) Image(painterResource(requireNotNull(Art.shopItem("obs_starship"))), null, Modifier.align(Alignment.CenterEnd).offset(y = 70.dp - (bob * 4).dp).size(64.dp))
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
            listOf("obs_solar", "obs_garden", "obs_antenna").filter { it in owned }.forEach { id ->
                Image(painterResource(requireNotNull(Art.shopItem(id))), null, Modifier.size(46.dp))
            }
        }
        // text and controls inside the ring
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 34.dp).width(260.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = overlay,
        )
    }
}
