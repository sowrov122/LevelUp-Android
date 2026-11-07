package com.levelup.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelup.app.domain.HeatLevel
import com.levelup.app.domain.ProgressMath
import com.levelup.app.ui.theme.AppColors
import com.levelup.app.ui.viewmodel.ChartBar
import com.levelup.app.ui.viewmodel.HeatWeek

private val PLOT_HEIGHT = 140.dp
private val TOP_PAD = 18.dp

/** Bar chart with a y-axis, faint grid lines, value labels and date labels. Every bar is a stored number. */
@Composable
fun AxisBarChart(
    bars: List<ChartBar>,
    axisMax: Int,
    barColor: Color,
    modifier: Modifier = Modifier,
    showValues: Boolean = bars.size <= 10,
    valueSuffix: String = "",
) {
    val ticks = ProgressMath.ticks(axisMax)
    val gap = if (bars.size > 10) 2.dp else 6.dp
    Row(modifier.fillMaxWidth()) {
        Column(
            Modifier.width(38.dp).padding(top = TOP_PAD - 6.dp).height(PLOT_HEIGHT + 12.dp).padding(end = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            ticks.reversed().forEach { Text("$it$valueSuffix", fontSize = 9.sp, color = AppColors.TextSecondary, maxLines = 1) }
        }
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth().height(PLOT_HEIGHT + TOP_PAD)) {
                Canvas(Modifier.fillMaxSize()) {
                    val top = TOP_PAD.toPx()
                    val plot = PLOT_HEIGHT.toPx()
                    for (i in 0..4) {
                        val y = top + plot * i / 4f
                        drawLine(AppColors.Border.copy(alpha = if (i == 4) 1f else 0.55f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                }
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
                    bars.forEach { b ->
                        val frac = (b.value.toFloat() / axisMax).coerceIn(0f, 1f)
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            if (showValues && b.value > 0) {
                                Text("${b.value}$valueSuffix", fontSize = 9.sp, color = AppColors.TextPrimary, maxLines = 1, softWrap = false, fontWeight = FontWeight.Medium)
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth(if (bars.size > 10) 0.9f else 0.62f)
                                    .height(if (b.value <= 0) 2.dp else (PLOT_HEIGHT * frac).coerceAtLeast(3.dp))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (b.value <= 0) AppColors.Border else barColor),
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                bars.forEach { b ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(b.label, fontSize = 10.sp, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                        if (b.sublabel.isNotEmpty()) Text(b.sublabel, fontSize = 8.sp, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val CELL_HEIGHT = 22.dp

fun HeatLevel?.heatColor(): Color = when (this) {
    null -> Color.Transparent
    HeatLevel.NONE -> AppColors.Elevated
    HeatLevel.LOW -> AppColors.Lavender.copy(alpha = 0.5f)
    HeatLevel.MEDIUM -> AppColors.Lavender
    HeatLevel.HIGH -> AppColors.HeatHigh
}

/** Five week-columns by seven weekday-rows. Future days stay empty; today has a blue outline. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeatmapGrid(weeks: List<HeatWeek>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(Modifier.width(30.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.height(14.dp))
                DAY_LABELS.forEach { Box(Modifier.height(CELL_HEIGHT), contentAlignment = Alignment.CenterStart) { Text(it, fontSize = 9.sp, color = AppColors.TextSecondary) } }
            }
            weeks.forEach { week ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.height(14.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(week.label, fontSize = 8.sp, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                    }
                    week.days.forEach { cell ->
                        val shape = RoundedCornerShape(5.dp)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(CELL_HEIGHT)
                                .clip(shape)
                                .background(cell.level.heatColor())
                                .border(
                                    if (cell.isToday) 1.5.dp else 1.dp,
                                    if (cell.isToday) AppColors.ProgressBlue else if (cell.level == null) AppColors.Border.copy(alpha = 0.4f) else Color.Transparent,
                                    shape,
                                ),
                        )
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                HeatLevel.NONE to "No activity", HeatLevel.LOW to "Low (1–25%)",
                HeatLevel.MEDIUM to "Medium (26–75%)", HeatLevel.HIGH to "High (76–100%)",
            ).forEach { (level, text) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(level.heatColor()))
                    Text(text, fontSize = 10.sp, color = AppColors.TextSecondary)
                }
            }
        }
    }
}
