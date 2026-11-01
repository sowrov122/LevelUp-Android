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
import com.levelup.app.domain.TimeUtils
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.EmptyState
import com.levelup.app.ui.components.NotificationPermissionCard
import com.levelup.app.ui.components.formatMinutes
import com.levelup.app.ui.viewmodel.RoutineItemUi
import com.levelup.app.ui.viewmodel.RoutineViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

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
    var editing by remember { mutableStateOf<TaskDraft?>(null) }
    var deleting by remember { mutableStateOf<RoutineTaskEntity?>(null) }

    LaunchedEffect(pendingAdd) {
        if (pendingAdd) {
            editing = TaskDraft()
            onAddConsumed()
        }
    }

    Box(Modifier.fillMaxWidth()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
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
