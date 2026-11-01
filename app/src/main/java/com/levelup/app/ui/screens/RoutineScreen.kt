package com.levelup.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.data.RoutineTaskEntity
import com.levelup.app.domain.Category
import com.levelup.app.domain.Priority
import com.levelup.app.domain.TaskDraft
import com.levelup.app.domain.TaskStatus
import com.levelup.app.domain.TimeUtils
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.EmptyState
import com.levelup.app.ui.components.NotificationPermissionCard
import com.levelup.app.ui.components.formatMinutes
import com.levelup.app.ui.viewmodel.RoutineItemUi
import com.levelup.app.ui.viewmodel.RoutineViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.levelup.app.ui.components.ProgressBar
import com.levelup.app.ui.viewmodel.RoutineDayUi
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

@Composable
fun RoutineScreen(vm: RoutineViewModel, pendingAdd: Boolean, onAddConsumed: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val day by vm.day.collectAsStateWithLifecycle()
    var manage by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskDraft?>(null) }
    var deleting by remember { mutableStateOf<RoutineTaskEntity?>(null) }

    LaunchedEffect(pendingAdd) {
        if (pendingAdd) {
            editing = TaskDraft()
            onAddConsumed()
        }
    }

    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !manage, onClick = { manage = false }, label = { Text("Daily view") })
                FilterChip(selected = manage, onClick = { manage = true }, label = { Text("Manage tasks") })
            }
            if (!manage) {
                DayView(day, onSelect = vm::selectDate, onToggle = vm::setCompleted)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.anyReminderConfigured) item { NotificationPermissionCard() }
                    if (!state.loading && state.items.isEmpty()) {
                        item { EmptyState("No tasks yet", "Tap + to add your first routine task.") }
                    }
                    items(state.items, key = { it.task.id }) { item ->
                        RoutineRow(
                            item,
                            onEdit = { editing = item.task.toDraft() },
                            onDelete = { deleting = item.task },
                            onToggle = { vm.setEnabled(item.task.id, it) },
                            onUp = { vm.move(item.task.id, -1) },
                            onDown = { vm.move(item.task.id, 1) },
                            onDuplicate = { vm.duplicate(item.task.id) },
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editing = TaskDraft() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Add task") }
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
            message = "\"${task.title}\" will be removed from your routine. Past completed days keep their history.",
            confirmLabel = "Delete",
            onConfirm = { vm.delete(task.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

private val DAY_HEADER = DateTimeFormatter.ofPattern("EEEE, d MMMM")

@Composable
private fun DayView(day: RoutineDayUi, onSelect: (LocalDate) -> Unit, onToggle: (String, Boolean) -> Unit) {
    val selected = day.selected
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                if (selected != null && selected == day.today) "Today's Routine" else selected?.format(DAY_HEADER) ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                day.weekDays.forEach { d ->
                    val isSel = d == selected
                    val isToday = d == day.today
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (isToday && !isSel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                else Modifier,
                            )
                            .clickable { onSelect(d) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        Text(d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = fg)
                        Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = fg)
                    }
                }
            }
        }
        item {
            val pct = if (day.total == 0) 0 else day.completed * 100 / day.total
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${day.completed}/${day.total} Completed", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                    Text("$pct%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                ProgressBar(if (day.total == 0) 0f else day.completed.toFloat() / day.total, height = 8.dp)
                if (!day.canToggle && selected != null) {
                    Text(
                        if (day.today != null && selected.isBefore(day.today)) "Past days are read-only history."
                        else "Upcoming day: you can tick tasks once the day arrives.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (!day.loading && day.rows.isEmpty()) {
            item {
                EmptyState(
                    if (day.selected != null && day.today != null && day.selected.isBefore(day.today)) "Nothing was recorded for this day"
                    else "Nothing scheduled",
                    "Past days only show what the app tracked. Use Manage tasks to edit your routine.",
                )
            }
        }
        items(day.rows, key = { it.instanceId }) { row -> DayRow(row, day.canToggle, onToggle) }
    }
}

@Composable
private fun DayRow(row: TaskRowUi, canToggle: Boolean, onToggle: (String, Boolean) -> Unit) {
    val (start, end) = row.timeLabel.split("–").let { it.first() to it.last() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.width(48.dp), horizontalAlignment = Alignment.End) {
            Text(start, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(end, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(
            Modifier.weight(1f),
            border = if (row.inProgress) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        ) {
            Row(Modifier.padding(start = 0.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(5.dp).height(56.dp).background(Color(row.category.colorHex)))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(row.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            row.status == TaskStatus.COMPLETED -> "Done · +${row.pointsAwarded} XP"
                            row.inProgress -> "In progress · +${row.xpReward} XP"
                            row.status == TaskStatus.MISSED -> "Missed"
                            else -> "+${row.xpReward} XP · ${formatMinutes(row.durationMinutes)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (row.status == TaskStatus.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Checkbox(
                    checked = row.status == TaskStatus.COMPLETED,
                    onCheckedChange = { onToggle(row.instanceId, it) },
                    enabled = canToggle,
                )
            }
        }
    }
}

@Composable
private fun RoutineRow(
    item: RoutineItemUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDuplicate: () -> Unit,
) {
    val t = item.task
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${TimeUtils.format(t.startMinute)}–${TimeUtils.format(t.endMinute)}" +
                            (if (t.endDayOffset == 1) " (next day)" else "") +
                            " · ${formatMinutes(t.durationMinutes)} · ${daysLabel(t.repeatDaysMask)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${t.category.label} · ${t.priority.label}" +
                            (t.reminderOffsetMinutes?.let { if (it == 0) " · reminder on time" else " · reminder $it min before" } ?: " · no reminder"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = t.isEnabled, onCheckedChange = onToggle)
            }
            if (item.conflictsWith.isNotEmpty()) {
                Text(
                    "Overlaps with: ${item.conflictsWith.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onUp) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up") }
                IconButton(onClick = onDown) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down") }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                TextButton(onClick = onDuplicate) { Text("Duplicate") }
            }
        }
    }
}

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
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                    }
                }
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.entries.forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                    }
                }
                Text("Repeat on", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_NAMES.forEachIndexed { i, name ->
                        FilterChip(
                            selected = mask and (1 shl i) != 0,
                            onClick = { mask = mask xor (1 shl i) },
                            label = { Text(name) },
                        )
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
                        FilterChip(
                            selected = (reminder.isBlank() && v == null) || reminder.trim() == v?.toString(),
                            onClick = { reminder = v?.toString() ?: "" },
                            label = { Text(when (v) { null -> "None"; 0 -> "On time"; else -> "$v min" }) },
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
