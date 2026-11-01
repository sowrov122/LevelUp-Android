package com.levelup.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.levelup.app.domain.Observatory

/**
 * Cosmic Observatory. Everything drawn comes from saved focus totals and owned cosmetics.
 * With reduced motion enabled the scene is completely static (also the low-power mode).
 */
@Composable
fun ObservatoryScene(
    sessions: Int,
    minutes: Int,
    owned: Set<String>,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
) {
    val unlocked = remember(sessions, minutes) { Observatory.unlockedIds(sessions, minutes) }
    val starCount = Observatory.starCount(sessions, minutes)
    val stars = remember {
        val r = java.util.Random(7L)
        List(80) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.62f, 0.8f + r.nextFloat() * 1.6f) }
    }
    val twinkle: Float = if (reducedMotion) {
        1f
    } else {
        val t = rememberInfiniteTransition(label = "twinkle")
        t.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
            label = "twinkleAlpha",
        ).value
    }
    val accent = MaterialTheme.colorScheme.primary

    Canvas(
        modifier
            .then(
                if (circular) Modifier.fillMaxSize()
                else Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
            )
            .semantics {
                contentDescription = "Your observatory: $starCount stars and ${unlocked.size} of " +
                    "${Observatory.milestones.size} modules unlocked"
            },
    ) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0B0716), Color(0xFF1B1030), Color(0xFF2A1A22))))

        if ("obs_nebula" in owned) {
            val c = Offset(w * 0.3f, h * 0.3f)
            drawCircle(Brush.radialGradient(listOf(Color(0x66A070FF), Color.Transparent), center = c, radius = w * 0.35f), radius = w * 0.35f, center = c)
        }

        for (i in 0 until starCount.coerceAtMost(stars.size)) {
            val s = stars[i]
            val a = (if (i % 2 == 0) twinkle else 1.55f - twinkle).coerceIn(0.25f, 1f)
            drawCircle(Color.White.copy(alpha = a), radius = s[2] * density, center = Offset(s[0] * w, s[1] * h))
        }
        if ("constellation" in unlocked && starCount >= 6) {
            for (k in 0 until 5) {
                val a = stars[k]
                val b = stars[k + 1]
                drawLine(accent.copy(alpha = 0.55f), Offset(a[0] * w, a[1] * h), Offset(b[0] * w, b[1] * h), strokeWidth = 1.dp.toPx())
            }
        }

        if ("planet" in unlocked) {
            val c = Offset(w * 0.82f, h * 0.28f)
            val r = h * 0.12f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF7FB6E8), Color(0xFF2B4C7E)), center = c, radius = r), radius = r, center = c)
            if ("obs_ringed_planet" in owned) {
                drawOval(Color(0xAAFFE6B0), Offset(c.x - r * 1.6f, c.y - r * 0.35f), Size(r * 3.2f, r * 0.7f), style = Stroke(2.dp.toPx()))
            }
        }
        if ("obs_comet" in owned) {
            val from = Offset(w * 0.52f, h * 0.08f)
            val to = Offset(w * 0.68f, h * 0.2f)
            drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), start = from, end = to), from, to, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Color.White, radius = 3.dp.toPx(), center = to)
        }
        if ("obs_satellite" in owned) {
            val c = Offset(w * 0.5f, h * 0.3f)
            drawRect(Color(0xFFCFC6BB), Offset(c.x - 6.dp.toPx(), c.y - 4.dp.toPx()), Size(12.dp.toPx(), 8.dp.toPx()))
            drawRect(Color(0xFF6C8EBF), Offset(c.x - 20.dp.toPx(), c.y - 2.dp.toPx()), Size(12.dp.toPx(), 4.dp.toPx()))
            drawRect(Color(0xFF6C8EBF), Offset(c.x + 8.dp.toPx(), c.y - 2.dp.toPx()), Size(12.dp.toPx(), 4.dp.toPx()))
        }
        if ("starship" in unlocked) {
            val hull = Path().apply {
                moveTo(w * 0.60f, h * 0.50f)
                lineTo(w * 0.74f, h * 0.56f)
                lineTo(w * 0.60f, h * 0.62f)
                close()
            }
            drawPath(hull, accent)
        }

        // ---- shop items (drawn only when owned) ----
        if ("obs_moon" in owned) {
            drawCircle(Color(0xFFBDBDBD), radius = h * 0.05f, center = Offset(w * 0.62f, h * 0.1f))
            drawCircle(Color(0xFF9E9E9E), radius = h * 0.012f, center = Offset(w * 0.61f, h * 0.09f))
        }
        if ("obs_new_planet" in owned) {
            val c = Offset(w * 0.28f, h * 0.3f)
            val r = h * 0.09f
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFB86B), Color(0xFFB5502B)), center = c, radius = r), radius = r, center = c)
        }
        if ("obs_shuttle" in owned) {
            drawRoundRect(Color(0xFFEDEDED), Offset(w * 0.18f, h * 0.14f), Size(w * 0.1f, h * 0.035f), CornerRadius(8f))
            drawRect(Color(0xFFD9534F), Offset(w * 0.18f, h * 0.125f), Size(w * 0.02f, h * 0.015f))
            drawCircle(Color(0xFF7FB6E8), radius = 2.dp.toPx(), center = Offset(w * 0.26f, h * 0.157f))
        }
        if ("obs_starship" in owned) {
            val ship = Path().apply {
                moveTo(w * 0.08f, h * 0.2f)
                lineTo(w * 0.3f, h * 0.12f)
                lineTo(w * 0.36f, h * 0.2f)
                lineTo(w * 0.3f, h * 0.28f)
                close()
            }
            drawPath(ship, Color(0xFFCFC6BB))
            drawCircle(accent, radius = 3.dp.toPx(), center = Offset(w * 0.28f, h * 0.2f))
            drawLine(accent.copy(alpha = 0.6f), Offset(w * 0.08f, h * 0.2f), Offset(w * 0.02f, h * 0.2f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        if ("obs_solar" in owned) {
            listOf(0.58f, 0.67f).forEach { x ->
                drawRect(Color(0xFF3D6FB6), Offset(w * x, h * 0.72f), Size(w * 0.08f, h * 0.09f))
                drawLine(Color(0x88FFFFFF), Offset(w * (x + 0.04f), h * 0.72f), Offset(w * (x + 0.04f), h * 0.81f), strokeWidth = 1.dp.toPx())
            }
        }
        if ("obs_garden" in owned) {
            drawArc(Color(0xAA4CAF50), 180f, 180f, true, Offset(w * 0.78f, h * 0.7f), Size(w * 0.16f, h * 0.24f))
            drawCircle(Color(0xFFE8F5E9), radius = 2.dp.toPx(), center = Offset(w * 0.86f, h * 0.77f))
        }
        if ("obs_antenna" in owned) {
            drawLine(Color(0xFFCFC6BB), Offset(w * 0.06f, h * 0.82f), Offset(w * 0.06f, h * 0.52f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Color(0xFFCFC6BB), 200f, 140f, false, Offset(w * 0.02f, h * 0.47f), Size(w * 0.08f, h * 0.08f), style = Stroke(2.dp.toPx()))
            drawCircle(accent, radius = 2.dp.toPx(), center = Offset(w * 0.06f, h * 0.5f))
        }

        if ("dome" in unlocked) {
            drawArc(Color(0xFF3A2C22), 180f, 180f, true, Offset(w * 0.12f, h * 0.62f), Size(w * 0.26f, h * 0.4f))
            drawRect(Color(0xFF0B0716), Offset(w * 0.245f, h * 0.63f), Size(w * 0.03f, h * 0.1f))
        }
        drawRect(Color(0xFF1A110C), Offset(0f, h * 0.82f), Size(w, h * 0.18f))
        if ("telescope" in unlocked) {
            drawLine(accent, Offset(w * 0.255f, h * 0.7f), Offset(w * 0.36f, h * 0.5f), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        }
        if ("module" in unlocked) {
            drawRect(Color(0xFF5A4636), Offset(w * 0.42f, h * 0.74f), Size(w * 0.12f, h * 0.08f))
            drawRect(accent.copy(alpha = 0.8f), Offset(w * 0.44f, h * 0.76f), Size(w * 0.03f, h * 0.025f))
        }
    }
}
