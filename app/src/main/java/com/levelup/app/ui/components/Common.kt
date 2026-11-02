package com.levelup.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.levelup.app.ui.theme.AppColors
import com.levelup.app.ui.theme.Radii

/** Kept for existing call sites; delegates to the design-system header. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) = SectionHeader(text, modifier)

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null) {
    AppCard(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = AppColors.TextPrimary)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Elevated,
        titleContentColor = AppColors.TextPrimary,
        textContentColor = AppColors.TextSecondary,
        shape = RoundedCornerShape(Radii.dialog),
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = AppColors.ProgressBlue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } },
    )
}

/** Circular progress in the progress accent on a quiet track. */
@Composable
fun ProgressRing(
    fraction: Float,
    centerText: String,
    subText: String,
    modifier: Modifier = Modifier.size(120.dp),
) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier.semantics { contentDescription = "Progress $centerText $subText" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            drawArc(AppColors.Border, 0f, 360f, false, style = stroke)
            if (fraction > 0f) drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Text(subText, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
    }
}

/** Horizontal progress bar. Default fill is the progress accent; pass AppColors.RewardGold for rewards. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, height: Dp = 8.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(AppColors.Border)) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

/** Simple bar chart. Labels may be blank to avoid crowding. Values are raw saved data. */
@Composable
fun BarChart(
    values: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    maxValue: Int = (values.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1),
) {
    Row(
        modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { (label, value) ->
            Column(
                Modifier.weight(1f).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val h = if (value <= 0) 2f else (100f * value / maxValue).coerceAtLeast(3f)
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(h.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (value <= 0) AppColors.Border else barColor),
                )
                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = AppColors.TextSecondary)
            }
        }
    }
}

fun formatClock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

fun formatMinutes(minutes: Int): String =
    if (minutes < 60) "${minutes}m" else if (minutes % 60 == 0) "${minutes / 60}h" else "${minutes / 60}h ${minutes % 60}m"
