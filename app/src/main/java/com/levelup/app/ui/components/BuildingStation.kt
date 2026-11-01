package com.levelup.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * The space station that is assembled *during* a running focus session (like a tree growing in Forest).
 * [progress] is elapsed / planned time (0..1). Parts fade in at fixed progress thresholds, so the picture is
 * always a pure function of the saved session time and survives rotation or process death.
 */
@Composable
fun BuildingStation(progress: Float, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    val pulse: Float = if (reducedMotion) {
        1f
    } else {
        rememberInfiniteTransition(label = "station").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
            label = "stationPulse",
        ).value
    }
    val accent = MaterialTheme.colorScheme.primary
    val stars = remember {
        val r = java.util.Random(11L)
        List(55) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.7f + r.nextFloat() * 1.4f) }
    }

    Canvas(
        modifier.semantics { contentDescription = "Space station ${(p * 100).toInt()} percent built" },
    ) {
        val w = size.width
        val h = size.height
        drawRect(Brush.radialGradient(listOf(Color(0xFF2A1B4D), Color(0xFF0B0716)), center = Offset(w / 2, h / 2), radius = w * 0.8f))
        stars.forEachIndexed { i, s ->
            val a = if (reducedMotion) 0.8f else (if (i % 2 == 0) pulse else 1.45f - pulse).coerceIn(0.25f, 1f)
            drawCircle(Color.White.copy(alpha = a), radius = s[2] * density, center = Offset(s[0] * w, s[1] * h))
        }
        val planet = Offset(w * 0.8f, h * 0.2f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF7FB6E8), Color(0xFF2B4C7E)), center = planet, radius = w * 0.1f), radius = w * 0.1f, center = planet)

        fun part(t: Float) = ((p - t) / 0.08f).coerceIn(0f, 1f)
        val cx = w / 2
        val cy = h * 0.55f

        part(0.05f).takeIf { it > 0f }?.let { a ->
            drawRoundRect(Color(0xFF9A8878).copy(alpha = a), Offset(cx - w * 0.07f, cy - h * 0.12f), Size(w * 0.14f, h * 0.24f), CornerRadius(w * 0.03f))
        }
        part(0.25f).takeIf { it > 0f }?.let { a ->
            drawOval(Color(0xFFCFC6BB).copy(alpha = a), Offset(cx - w * 0.22f, cy - h * 0.04f), Size(w * 0.44f, h * 0.08f), style = Stroke(3.dp.toPx()))
        }
        part(0.45f).takeIf { it > 0f }?.let { a ->
            val panel = Color(0xFF6C8EBF).copy(alpha = a)
            drawRect(panel, Offset(cx - w * 0.36f, cy - h * 0.07f), Size(w * 0.12f, h * 0.14f))
            drawRect(panel, Offset(cx + w * 0.24f, cy - h * 0.07f), Size(w * 0.12f, h * 0.14f))
        }
        part(0.65f).takeIf { it > 0f }?.let { a ->
            drawArc(Color(0xFFB7A99A).copy(alpha = a), 180f, 180f, true, Offset(cx - w * 0.07f, cy - h * 0.20f), Size(w * 0.14f, h * 0.16f))
        }
        part(0.8f).takeIf { it > 0f }?.let { a ->
            drawLine(Color(0xFFCFC6BB).copy(alpha = a), Offset(cx, cy - h * 0.2f), Offset(cx, cy - h * 0.3f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(accent.copy(alpha = a * pulse), radius = 4.dp.toPx(), center = Offset(cx, cy - h * 0.3f))
        }
        part(0.95f).takeIf { it > 0f }?.let { a ->
            listOf(-0.03f, 0f, 0.03f).forEach { dx ->
                drawCircle(Color(0xFFFFCE00).copy(alpha = a * pulse), radius = 2.5.dp.toPx(), center = Offset(cx + w * dx, cy + h * 0.02f))
            }
        }
    }
}
