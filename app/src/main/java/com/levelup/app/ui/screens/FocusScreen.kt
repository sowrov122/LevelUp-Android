package com.levelup.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.domain.Category
import com.levelup.app.domain.Observatory
import com.levelup.app.ui.components.BuildingStation
import com.levelup.app.ui.components.ObservatoryScene
import com.levelup.app.ui.components.SectionTitle
import com.levelup.app.ui.components.formatClock
import com.levelup.app.ui.components.formatMinutes
import com.levelup.app.ui.viewmodel.FocusViewModel

private val SpaceBackground = Brush.verticalGradient(listOf(Color(0xFF0B0716), Color(0xFF1B1030), Color(0xFF140E0A)))
private val SoftWhite = Color(0xFFFFF4E6)
private val GlobeBorder = Color(0x66FFF4E6)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(vm: FocusViewModel, onOpenBlocker: () -> Unit, onImmersive: (Boolean) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val active = state.active
    val context = LocalContext.current
    val view = LocalView.current

    var title by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var category by remember { mutableStateOf<Category?>(null) }
    var lockScreen by rememberSaveable { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    // Hide the app bars while a session runs, keep the screen on, and release screen pinning at the end.
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

    Box(Modifier.fillMaxSize().background(SpaceBackground)) {
        if (active != null) {
            val progress = if (active.plannedSeconds <= 0) 0f else active.elapsedSeconds.toFloat() / active.plannedSeconds
            Column(
                Modifier.fillMaxSize().systemBarsPadding().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(active.title, color = SoftWhite, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Globe(310) { BuildingStation(progress, state.reducedMotion, Modifier.fillMaxSize()) }
                }
                Text(
                    "Building your station… ${(progress * 100).toInt()}%",
                    color = SoftWhite.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(formatClock(active.remainingSeconds), color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light)
                if (!active.running) Text("Paused", color = SoftWhite.copy(alpha = 0.8f))
                if (state.strictMode) {
                    Text(
                        "Strict mode: opening other apps costs you.",
                        color = SoftWhite.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (state.protectionOn) {
                    Text(
                        "Protection on: your selected apps cost you.",
                        color = SoftWhite.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (active.interrupted) {
                    Text(
                        "The phone restarted, so the timer was paused instead of guessing the time.",
                        color = SoftWhite.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(Modifier.padding(top = 20.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    CircleButton(onClick = { showEnd = true }, description = "End session") {
                        Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
                    }
                    CircleButton(
                        onClick = { if (active.running) vm.pause(active.id) else vm.resume(active.id) },
                        description = if (active.running) "Pause" else "Resume",
                    ) {
                        if (active.running) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.size(width = 6.dp, height = 22.dp).background(Color.White))
                                Box(Modifier.size(width = 6.dp, height = 22.dp).background(Color.White))
                            }
                        } else {
                            Text("▶", color = Color.White, fontSize = 22.sp)
                        }
                    }
                }
            }

            if (showEnd) {
                val canCollect = active.elapsedSeconds >= 300
                AlertDialog(
                    onDismissRequest = { showEnd = false },
                    title = { Text("End this session?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (canCollect) "Finish now to collect rewards for the minutes you focused, or give up and earn nothing."
                                else "Less than 5 minutes are done, so finishing now earns nothing. Keep going or give up.",
                            )
                            TextButton(onClick = { showEnd = false; vm.cancel(active.id) }) { Text("Give up (no reward)") }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showEnd = false; vm.finish(active.id) }, enabled = canCollect) { Text("Finish & collect") }
                    },
                    dismissButton = { TextButton(onClick = { showEnd = false }) { Text("Keep going") } },
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            category?.label?.let { "● $it" } ?: "● Choose a tag",
                            color = SoftWhite.copy(alpha = 0.8f),
                            modifier = Modifier.clickable {
                                val all = Category.entries
                                category = if (category == null) all.first() else all.getOrNull(all.indexOf(category) + 1)
                            },
                        )
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it.take(60) },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                            decorationBox = { inner ->
                                if (title.isEmpty()) Text("Add session title…", color = SoftWhite.copy(alpha = 0.5f), fontSize = 20.sp)
                                inner()
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        )
                    }
                }

                Globe(280) { ObservatoryScene(state.sessions, state.minutes, state.owned, state.reducedMotion, circular = true) }
                Text(
                    "${state.sessions} sessions · ${formatMinutes(state.minutes)} focused · " +
                        "${Observatory.starCount(state.sessions, state.minutes)} stars",
                    color = SoftWhite.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { minutes = (minutes - 5).coerceAtLeast(5) }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "5 minutes less", tint = Color.White)
                    }
                    Text(formatClock(minutes * 60L), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Light)
                    IconButton(onClick = { minutes = (minutes + 5).coerceAtMost(180) }) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "5 minutes more", tint = Color.White)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 25, 45, 60).forEach { m ->
                        FilterChip(selected = minutes == m, onClick = { minutes = m }, label = { Text("$m min") })
                    }
                }

                Button(
                    onClick = {
                        if (lockScreen) runCatching { (context as? Activity)?.startLockTask() }
                        vm.start(title, category, minutes)
                    },
                    modifier = Modifier.fillMaxWidth(0.85f).height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF140E0A)),
                ) { Text("Start Focus", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Lock phone to Level Up", color = SoftWhite)
                        Text(
                            "Uses Android screen pinning, so other apps can't be opened during the session. Android may ask you to confirm. " +
                                "Hold Back + Overview (or swipe up and hold) to leave; the session keeps running.",
                            color = SoftWhite.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = lockScreen, onCheckedChange = { lockScreen = it })
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            when {
                                state.strictMode -> "Strict mode is ON: opening other apps during focus costs points."
                                state.protectionOn -> "Protection is ON for your selected apps."
                                else -> "Distraction penalties are OFF."
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = onOpenBlocker) { Text("Distraction protection settings") }
                    }
                }

                state.lastOutcome?.let { o ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            if (o.completed) {
                                Text("Session complete", style = MaterialTheme.typography.titleMedium)
                                Text("${o.focusedMinutes} min focused · +${o.reward.points} points · +${o.reward.xp} XP · +${o.reward.coins} coins")
                                if (o.cappedByDailyLimit) {
                                    Text("Daily focus reward cap reached: only ${o.rewardedMinutes} min were rewarded.", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text("Session too short", style = MaterialTheme.typography.titleMedium)
                                Text("Sessions under 5 minutes are not rewarded.")
                            }
                            TextButton(onClick = { vm.dismissOutcome() }) { Text("Dismiss") }
                        }
                    }
                }

                SectionTitle("Station milestones")
                Observatory.milestones.forEach { m ->
                    val done = Observatory.isUnlocked(m, state.sessions, state.minutes)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(m.title, fontWeight = FontWeight.SemiBold, color = SoftWhite)
                            Text(m.description, style = MaterialTheme.typography.bodySmall, color = SoftWhite.copy(alpha = 0.6f))
                        }
                        Text(if (done) "Unlocked" else "Locked", color = if (done) MaterialTheme.colorScheme.tertiary else SoftWhite.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Globe(sizeDp: Int, content: @Composable () -> Unit) {
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape).border(2.dp, GlobeBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun CircleButton(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .border(2.dp, GlobeBorder, CircleShape)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
