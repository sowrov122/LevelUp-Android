package com.levelup.app.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.data.RoutineTaskEntity
import com.levelup.app.domain.Category
import com.levelup.app.domain.CategoryGroup
import com.levelup.app.domain.Priority
import com.levelup.app.domain.TaskDraft
import com.levelup.app.domain.TaskStatus
import com.levelup.app.domain.TimeUtils
import com.levelup.app.domain.group
import com.levelup.app.ui.components.AppCard
import com.levelup.app.ui.components.AppChip
import com.levelup.app.ui.components.CategoryIcon
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.EmptyState
import com.levelup.app.ui.components.FlameIcon
import com.levelup.app.ui.components.LinkButton
import com.levelup.app.ui.components.NotificationPermissionCard
import com.levelup.app.ui.components.PrimaryButton
import com.levelup.app.ui.components.ScreenHeader
import com.levelup.app.ui.components.SecondaryButton
import com.levelup.app.ui.components.appSwitchColors
import com.levelup.app.ui.components.formatMinutes
import com.levelup.app.ui.components.icon
import com.levelup.app.ui.theme.AppColors
import com.levelup.app.ui.viewmodel.RoutineDayUi
import com.levelup.app.ui.viewmodel.RoutineItemUi
import com.levelup.app.ui.viewmodel.RoutineViewModel
import com.levelup.app.ui.viewmodel.TaskRowUi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun daysLabel(mask: Int): String = when (mask and TimeUtils.ALL_DAYS) {
    TimeUtils.ALL_DAYS -> "Every day"
    TimeUtils.WEEKDAYS -> "Mon–Fri"
    TimeUtils.WEEKEND -> "Sat–Sun"
    else -> DAY_NAMES.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString(", ")
}

private fun RoutineTaskEntity.toDraft() = TaskDraft(
    id = id, title = title, description = description, category = category,
    startMinute = startMinute, endMinute = endMinute, priority = priority,
    reminderOffsetMinutes = reminderOffsetMinutes, isEnabled = isEnabled, repeatDaysMask = repeatDaysMask,
)

private enum class Slot { DONE, ACTIVE, UPCOMING, MISSED }

private fun TaskRowUi.slot(): Slot = when {
    status == TaskStatus.COMPLETED -> Slot.DONE
    inProgress -> Slot.ACTIVE
    status == TaskStatus.MISSED -> Slot.MISSED
    else -> Slot.UPCOMING
}

// ============================================================ screen

@Composable
fun RoutineScreen(vm: RoutineViewModel, pendingAdd: Boolean, onAddConsumed: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val day by vm.day.collectAsStateWithLifecycle()
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    var customize by rememberSaveable { mutableStateOf(false) }
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<TaskDraft?>(null) }
    var deleting by remember { mutableStateOf<RoutineTaskEntity?>(null) }

    fun taskById(id: String): RoutineTaskEntity? = state.items.firstOrNull { it.task.id == id }?.task

    LaunchedEffect(pendingAdd) {
        if (pendingAdd) {
            editing = TaskDraft()
            onAddConsumed()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "My Routine",
                subtitle = "Plan • Schedule • Focus • Repeat",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderIcon(Icons.Filled.CalendarMonth, "Jump to today", false) { day.today?.let(vm::selectDate); customize = false }
                        HeaderIcon(Icons.Filled.Tune, "Customize routine", customize) { customize = !customize }
                    }
                },
            )
            if (!customize) {
                ScheduleView(
                    day = day,
                    filter = CategoryGroup.entries[filterIndex],
                    is24 = is24,
                    onFilter = { filterIndex = it.ordinal },
                    onSelectDate = vm::selectDate,
                    onToggle = vm::setCompleted,
                    onEdit = { id -> taskById(id)?.let { editing = it.toDraft() } },
                    onDuplicate = vm::duplicate,
                    onDelete = { id -> deleting = taskById(id) },
                    onCustomize = { customize = true },
                    modifier = Modifier.weight(1f),
                )
            } else {
                CustomizeView(
                    state.items, state.anyReminderConfigured, is24,
                    onDone = { customize = false },
                    onSortByTime = vm::sortByTime,
                    onEnable = vm::setEnabled,
                    onUp = { vm.move(it, -1) },
                    onDown = { vm.move(it, 1) },
                    onEdit = { editing = it.toDraft() },
                    onDuplicate = vm::duplicate,
                    onDelete = { deleting = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        PrimaryButton(
            "+  Add Task",
            onClick = { editing = TaskDraft() },
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
        )
    }

    editing?.let { draft ->
        TaskEditorDialog(
            initial = draft,
            onDismiss = { editing = null },
            onSave = {
                vm.save(it)
                editing = null
            },
        )
    }
    deleting?.let { task ->
        ConfirmDialog(
            title = "Delete task?",
            message = "\"${task.title}\" will be removed from your routine. Days you already completed keep their history.",
            confirmLabel = "Delete",
            onConfirm = { vm.delete(task.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .background(if (active) AppColors.Lavender else AppColors.Card)
            .border(1.dp, if (active) AppColors.Lavender else AppColors.Border, shape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = description, tint = AppColors.TextPrimary, modifier = Modifier.size(22.dp)) }
}

// ======================================================= daily view

/** Stateless daily schedule: everything shown is passed in, so it can be tested without a database. */
@Composable
fun ScheduleView(
    day: RoutineDayUi,
    filter: CategoryGroup,
    is24: Boolean,
    onFilter: (CategoryGroup) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = day.rows.filter { filter == CategoryGroup.ALL || it.category.group == filter }
    val selected = day.selected
    LazyColumn(
        modifier = modifier.fillMaxWidth().testTag("schedule_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
    ) {
        item { DateStrip(day, onSelectDate); Spacer(Modifier.height(12.dp)) }
        item { SummaryCard(day); Spacer(Modifier.height(12.dp)) }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryGroup.entries.forEach { g -> AppChip(g.label, filter == g, { onFilter(g) }, icon = g.icon()) }
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (selected != null && selected == day.today) "Today's Schedule" else "Schedule",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                )
                LinkButton("Customize", onCustomize)
            }
        }
        if (!day.loading && day.rows.isEmpty()) {
            item {
                EmptyState(
                    if (day.selected != null && day.today != null && day.selected.isBefore(day.today)) "Nothing was recorded for this day"
                    else "Nothing scheduled",
                    "Tap Add Task to plan something, or open Customize to edit your routine.",
                )
            }
        } else if (rows.isEmpty()) {
            item { EmptyState("No ${filter.label.lowercase()} tasks", "Choose another category or All.") }
        }
        itemsIndexed(rows, key = { _, r -> r.instanceId }) { index, row ->
            TimelineRow(
                row = row,
                first = index == 0,
                last = index == rows.lastIndex,
                canToggle = day.canToggle,
                is24 = is24,
                onToggle = { onToggle(row.instanceId, it) },
                onEdit = { onEdit(row.taskId) },
                onDuplicate = { onDuplicate(row.taskId) },
                onDelete = { onDelete(row.taskId) },
            )
        }
    }
}

@Composable
private fun DateStrip(day: RoutineDayUi, onSelect: (LocalDate) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        day.weekDays.forEach { d ->
            val isSelected = d == day.selected
            val isToday = d == day.today
            Column(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (isSelected) AppColors.Lavender else AppColors.Card)
                    .border(1.dp, if (isSelected) AppColors.Lavender else AppColors.Border, shape)
                    .clickable(role = Role.Button) { onSelect(d) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fg = if (isSelected || isToday) AppColors.TextPrimary else AppColors.TextSecondary
                Text(
                    if (isToday) "Today" else d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    fontSize = 11.sp, color = fg, maxLines = 1, fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(d.format(DateTimeFormatter.ofPattern("dd MMM")), fontSize = 11.sp, color = fg, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SummaryCard(day: RoutineDayUi) {
    val selected = day.selected
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniRing(if (day.total == 0) 0f else day.completed.toFloat() / day.total, "${day.completed}/${day.total}")
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    if (selected != null && selected == day.today) "Today's Routine"
                    else selected?.format(DateTimeFormatter.ofPattern("EEE, d MMM")) ?: "Routine",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                )
                Text("${day.completed} of ${day.total} tasks completed", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
            Box(Modifier.width(1.dp).height(40.dp).background(AppColors.Border))
            SmallStat({ FlameIcon(Modifier.size(22.dp)) }, "${day.streak}", "Streak")
            Box(Modifier.width(1.dp).height(40.dp).background(AppColors.Border))
            SmallStat({ Icon(Icons.Filled.Star, null, tint = AppColors.RewardGold, modifier = Modifier.size(22.dp)) }, "${day.points}", "Points")
        }
    }
}

@Composable
private fun MiniRing(fraction: Float, label: String) {
    val color = MaterialTheme.colorScheme.primary
    Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            drawArc(AppColors.Border, 0f, 360f, false, style = stroke)
            if (fraction > 0f) drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, style = stroke)
        }
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
    }
}

@Composable
private fun SmallStat(icon: @Composable () -> Unit, value: String, label: String) {
    Column(Modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
        Text(label, fontSize = 10.sp, color = AppColors.TextSecondary, maxLines = 1)
    }
}

@Composable
private fun TimelineRow(
    row: TaskRowUi,
    first: Boolean,
    last: Boolean,
    canToggle: Boolean,
    is24: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val slot = row.slot()
    val gap = 8.dp
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                // One continuous vertical line through all markers, broken only at the first and last row.
                val x = 13.dp.toPx()
                val mid = (size.height - gap.toPx()) / 2f
                if (!first) drawLine(AppColors.Border, Offset(x, 0f), Offset(x, mid), strokeWidth = 2.dp.toPx())
                if (!last) drawLine(AppColors.Border, Offset(x, mid), Offset(x, size.height), strokeWidth = 2.dp.toPx())
            }
            .padding(bottom = gap)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Marker(slot, canToggle, row.title, onToggle)
        TimeBox(row, is24)
        TaskBody(row, slot, Modifier.weight(1f), onEdit, onDuplicate, onDelete)
    }
}

@Composable
private fun Marker(slot: Slot, canToggle: Boolean, title: String, onToggle: (Boolean) -> Unit) {
    val done = slot == Slot.DONE
    Box(
        Modifier
            .width(26.dp)
            .fillMaxHeight()
            .then(
                if (canToggle) Modifier.clickable(role = Role.Checkbox) { onToggle(!done) }
                    .semantics { contentDescription = if (done) "Mark $title not done" else "Mark $title done" }
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (slot) {
            Slot.DONE -> Box(Modifier.size(24.dp).background(AppColors.Background, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CheckCircle, null, tint = AppColors.Success, modifier = Modifier.size(24.dp))
            }
            Slot.ACTIVE -> Box(Modifier.size(22.dp).background(AppColors.Background, CircleShape).border(2.dp, AppColors.Lavender, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(10.dp).background(AppColors.Lavender, CircleShape))
            }
            Slot.UPCOMING -> Box(Modifier.size(20.dp).background(AppColors.Background, CircleShape).border(2.dp, AppColors.TextSecondary, CircleShape))
            Slot.MISSED -> Box(Modifier.size(22.dp).background(AppColors.Background, CircleShape).border(2.dp, AppColors.Danger, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, null, tint = AppColors.Danger, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun TimeBox(row: TaskRowUi, is24: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .width(72.dp)
            .fillMaxHeight()
            .clip(shape)
            .background(AppColors.Card)
            .border(1.dp, AppColors.Border, shape)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(TimeUtils.formatForDisplay(row.startMinute, is24), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, maxLines = 1)
        Text("– " + TimeUtils.formatForDisplay(row.startMinute + row.durationMinutes, is24), fontSize = 11.sp, color = AppColors.TextSecondary, maxLines = 1)
    }
}

@Composable
private fun TaskBody(
    row: TaskRowUi,
    slot: Slot,
    modifier: Modifier,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    AppCard(
        modifier,
        border = androidx.compose.foundation.BorderStroke(if (slot == Slot.ACTIVE) 1.5.dp else 1.dp, if (slot == Slot.ACTIVE) AppColors.Lavender else AppColors.Border),
        containerColor = if (slot == Slot.ACTIVE) AppColors.LavenderSoft else null,
    ) {
        Row(Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryIcon(row.category, size = 36.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        row.category.group.label,
                        fontSize = 10.sp,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.Elevated).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Icon(Icons.Filled.Schedule, null, tint = AppColors.TextSecondary, modifier = Modifier.size(12.dp))
                    Text(formatMinutes(row.durationMinutes), fontSize = 11.sp, color = AppColors.TextSecondary, maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusPill(slot)
                Text(
                    "+${if (slot == Slot.DONE) row.pointsAwarded else row.xpReward} XP",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.RewardGold,
                )
            }
            Box {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More actions for ${row.title}",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable(role = Role.Button) { menu = true }.padding(8.dp),
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun StatusPill(slot: Slot) {
    val (label, color, icon) = when (slot) {
        Slot.DONE -> Triple("Completed", AppColors.Success, Icons.Filled.Check)
        Slot.ACTIVE -> Triple("Active Now", AppColors.TextPrimary, Icons.Filled.PlayArrow)
        Slot.UPCOMING -> Triple("Upcoming", AppColors.TextSecondary, Icons.Filled.Schedule)
        Slot.MISSED -> Triple("Missed", AppColors.Danger, Icons.Filled.Close)
    }
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(if (slot == Slot.ACTIVE) AppColors.Lavender else Color.Transparent)
            .border(1.dp, if (slot == Slot.ACTIVE) AppColors.Lavender else color.copy(alpha = 0.7f), shape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
        Text(label, fontSize = 10.sp, color = color, maxLines = 1)
    }
}

// =================================================== customize view

@Composable
private fun CustomizeView(
    items: List<RoutineItemUi>,
    anyReminder: Boolean,
    is24: Boolean,
    onDone: () -> Unit,
    onSortByTime: () -> Unit,
    onEnable: (String, Boolean) -> Unit,
    onUp: (String) -> Unit,
    onDown: (String) -> Unit,
    onEdit: (RoutineTaskEntity) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (RoutineTaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().testTag("customize_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Customize routine", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                LinkButton("Done", onDone)
            }
            Text(
                "Turn tasks on or off, change their order, reminders and repeat days. The daily view always shows tasks by start time.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            SecondaryButton("Sort list by time", onSortByTime, Modifier.fillMaxWidth())
        }
        if (anyReminder) item { NotificationPermissionCard() }
        if (items.isEmpty()) item { EmptyState("No tasks yet", "Tap Add Task to create your first one.") }
        items(items, key = { it.task.id }) { item ->
            ManageRow(item, is24, onEnable, onUp, onDown, onEdit, onDuplicate, onDelete)
        }
    }
}

@Composable
private fun ManageRow(
    item: RoutineItemUi,
    is24: Boolean,
    onEnable: (String, Boolean) -> Unit,
    onUp: (String) -> Unit,
    onDown: (String) -> Unit,
    onEdit: (RoutineTaskEntity) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (RoutineTaskEntity) -> Unit,
) {
    val t = item.task
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryIcon(t.category, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(t.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                    Text(
                        "${TimeUtils.formatForDisplay(t.startMinute, is24)} – ${TimeUtils.formatForDisplay(t.endMinute, is24)}" +
                            (if (t.endDayOffset == 1) " (next day)" else "") +
                            " · ${formatMinutes(t.durationMinutes)} · ${daysLabel(t.repeatDaysMask)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                    Text(
                        "${t.priority.label} priority" +
                            (t.reminderOffsetMinutes?.let { if (it == 0) " · reminder on time" else " · reminder $it min before" } ?: " · no reminder"),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
                Switch(checked = t.isEnabled, onCheckedChange = { onEnable(t.id, it) }, colors = appSwitchColors())
            }
            if (item.conflictsWith.isNotEmpty()) {
                Text("Overlaps with: ${item.conflictsWith.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onUp(t.id) }) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move ${t.title} up", tint = AppColors.TextPrimary) }
                IconButton(onClick = { onDown(t.id) }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move ${t.title} down", tint = AppColors.TextPrimary) }
                IconButton(onClick = { onEdit(t) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit ${t.title}", tint = AppColors.TextPrimary) }
                IconButton(onClick = { onDuplicate(t.id) }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate ${t.title}", tint = AppColors.TextPrimary) }
                IconButton(onClick = { onDelete(t) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete ${t.title}", tint = AppColors.Danger) }
            }
        }
    }
}

// ================================================== task editor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorDialog(initial: TaskDraft, onDismiss: () -> Unit, onSave: (TaskDraft) -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var category by remember { mutableStateOf(initial.category) }
    var start by remember { mutableStateOf(TimeUtils.format(initial.startMinute)) }
    var end by remember { mutableStateOf(TimeUtils.format(initial.endMinute)) }
    var priority by remember { mutableStateOf(initial.priority) }
    var reminder by remember { mutableStateOf(initial.reminderOffsetMinutes?.toString() ?: "") }
    var mask by remember { mutableIntStateOf(initial.repeatDaysMask) }
    var error by remember { mutableStateOf<String?>(null) }

    val s = TimeUtils.parseHhMm(start)
    val e = TimeUtils.parseHhMm(end)
    val overnightNote = if (s != null && e != null && s != e) {
        val off = TimeUtils.resolveEndDayOffset(s, e)
        val dur = TimeUtils.durationMinutes(s, e, off)
        if (off == 1) "Ends the next day (overnight) · ${formatMinutes(dur)}" else "Duration ${formatMinutes(dur)}"
    } else null

    fun attemptSave() {
        if (s == null || e == null) {
            error = "Use 24-hour times such as 07:30."
            return
        }
        val rem = if (reminder.isBlank()) null else reminder.trim().toIntOrNull()
        if (reminder.isNotBlank() && rem == null) {
            error = "Reminder must be a whole number of minutes."
            return
        }
        val draft = initial.copy(
            title = title, category = category, startMinute = s, endMinute = e,
            priority = priority, reminderOffsetMinutes = rem, repeatDaysMask = mask,
        )
        val problem = TimeUtils.validate(draft)
        if (problem != null) {
            error = problem
            return
        }
        onSave(draft)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == null) "Add task" else "Edit task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        start, { start = it }, label = { Text("Start (HH:MM)") }, singleLine = true,
                        isError = s == null, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        end, { end = it }, label = { Text("End (HH:MM)") }, singleLine = true,
                        isError = e == null, modifier = Modifier.weight(1f),
                    )
                }
                if (overnightNote != null) Text(overnightNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Category.entries.forEach { c ->
                        AppChip(c.label, category == c, { category = c })
                    }
                }
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.entries.forEach { p ->
                        AppChip(p.label, priority == p, { priority = p })
                    }
                }
                Text("Repeat on", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_NAMES.forEachIndexed { i, name ->
                        AppChip(name, mask and (1 shl i) != 0, { mask = mask xor (1 shl i) })
                    }
                }
                Row {
                    TextButton(onClick = { mask = TimeUtils.WEEKDAYS }) { Text("Weekdays") }
                    TextButton(onClick = { mask = TimeUtils.WEEKEND }) { Text("Weekend") }
                    TextButton(onClick = { mask = TimeUtils.ALL_DAYS }) { Text("Every day") }
                }
                OutlinedTextField(
                    reminder, { reminder = it },
                    label = { Text("Reminder, minutes before (blank = none, 0 = on time)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf<Int?>(null, 0, 5, 10, 15, 30).forEach { v ->
                        AppChip(
                            when (v) { null -> "None"; 0 -> "On time"; else -> "$v min" },
                            (reminder.isBlank() && v == null) || reminder.trim() == v?.toString(),
                            { reminder = v?.toString() ?: "" },
                        )
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = { attemptSave() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
