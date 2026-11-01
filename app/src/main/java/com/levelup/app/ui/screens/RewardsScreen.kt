package com.levelup.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.BuildConfig
import com.levelup.app.domain.RewardCatalog
import com.levelup.app.domain.ShopCategory
import com.levelup.app.domain.ShopItem
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.ProgressBar
import com.levelup.app.ui.components.SectionTitle
import com.levelup.app.ui.viewmodel.RewardsViewModel
import java.text.NumberFormat

private fun coins(n: Int): String = NumberFormat.getIntegerInstance().format(n)

@Composable
fun RewardsScreen(vm: RewardsViewModel, onOpenPremium: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<ShopItem?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val category = ShopCategory.entries[tab]
    val rows = RewardCatalog.inCategory(category).chunked(2)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        toast?.let {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(it, Modifier.weight(1f).padding(vertical = 12.dp))
                        TextButton(onClick = { vm.consumeToast() }) { Text("OK") }
                    }
                }
            }
        }

        // Header: title + coin balance
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Reward Shop", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Card(shape = RoundedCornerShape(50)) {
                    Text(
                        "🪙 ${coins(s.coins)}",
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        if (BuildConfig.DEBUG) {
            item {
                Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))) {
                    Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                            Text("Testing only", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Debug builds can add free coins to try the shop. They appear in the ledger as test coins.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { vm.grantTestCoins() }) { Text("+10,000") }
                    }
                }
            }
        }

        // Level
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Level ${s.level.level}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ProgressBar(s.level.fraction)
                    Text(
                        "${s.level.xpIntoLevel} / ${s.level.xpForNextLevel} XP · total ${s.level.totalXp} XP. " +
                            "XP to advance from level L = 100 + L × 25.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Tabs
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShopCategory.entries.forEachIndexed { i, c ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(c.label) })
                }
            }
        }

        // Theme tab also offers the free default
        if (category == ShopCategory.THEME) {
            item {
                ItemCard(
                    title = "Ember (default)", description = "Warm amber accent.", price = 0,
                    owned = true, equippable = true, equipped = s.equippedTheme == RewardCatalog.DEFAULT_THEME_ID,
                    affordable = true, preview = { ThemeSwatch(Color(RewardCatalog.DEFAULT_THEME_COLOR)) },
                    onEquip = { vm.equip(RewardCatalog.DEFAULT_THEME_ID) }, onBuy = {}, modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        items(rows, key = { r -> r.first().id }) { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { item ->
                    ItemCard(
                        title = item.title, description = item.description, price = item.priceCoins,
                        owned = item.id in s.owned,
                        equippable = item.category == ShopCategory.THEME,
                        equipped = item.id == s.equippedTheme,
                        affordable = s.coins >= item.priceCoins,
                        preview = {
                            if (item.category == ShopCategory.THEME) ThemeSwatch(Color(item.colorHex ?: RewardCatalog.DEFAULT_THEME_COLOR))
                            else ItemPreview(item.id)
                        },
                        onEquip = { vm.equip(item.id) },
                        onBuy = { confirm = item },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        Text("Plan: Free", fontWeight = FontWeight.SemiBold)
                        Text("Premium is not available yet; nothing is locked.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onOpenPremium) { Text("Details") }
                }
            }
        }

        item { SectionTitle("Recent ledger") }
        if (s.recent.isEmpty()) {
            item { Text("Nothing earned yet. Complete a task to start your ledger.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(s.recent, key = { it.id }) { e ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(e.note ?: e.referenceId, style = MaterialTheme.typography.bodyMedium)
                    Text(e.localDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${signed(e.pointsDelta)} pts · ${signed(e.xpDelta)} XP · ${signed(e.coinsDelta)} c", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    confirm?.let { item ->
        ConfirmDialog(
            "Buy ${item.title}?",
            "This costs ${coins(item.priceCoins)} coins. You have ${coins(s.coins)}.",
            "Buy",
            onConfirm = { vm.buy(item.id); confirm = null },
            onDismiss = { confirm = null },
        )
    }
}

private fun signed(v: Int) = if (v > 0) "+$v" else v.toString()

@Composable
private fun ItemCard(
    title: String,
    description: String,
    price: Int,
    owned: Boolean,
    equippable: Boolean,
    equipped: Boolean,
    affordable: Boolean,
    preview: @Composable () -> Unit,
    onEquip: () -> Unit,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier,
        border = if (equipped || (owned && !equippable)) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) { preview() }
            Text(title, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
            )
            when {
                owned && equippable && equipped -> Text("Equipped", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                owned && equippable -> OutlinedButton(onClick = onEquip) { Text("Equip") }
                owned -> Text("Owned · on your station", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                else -> Button(onClick = onBuy) { Text(if (affordable) "🪙 ${coins(price)}" else "🔒 ${coins(price)}") }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(color: Color) {
    Canvas(Modifier.size(64.dp)) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.4f))))
        drawCircle(Color.White.copy(alpha = 0.35f), style = Stroke(2.dp.toPx()))
    }
}

/** Small picture for each shop item. Purely decorative; the real effect is drawn on the Focus station. */
@Composable
private fun ItemPreview(id: String) {
    Canvas(Modifier.size(80.dp)) {
        drawRoundRect(Color(0xFF140E0A), cornerRadius = CornerRadius(16.dp.toPx()))
        previewShape(id)
    }
}

private fun DrawScope.previewShape(id: String) {
    val w = size.width
    val h = size.height
    when (id) {
        "obs_satellite" -> {
            drawRect(Color(0xFFCFC6BB), Offset(w * 0.4f, h * 0.4f), Size(w * 0.2f, h * 0.2f))
            drawRect(Color(0xFF6C8EBF), Offset(w * 0.1f, h * 0.45f), Size(w * 0.26f, h * 0.1f))
            drawRect(Color(0xFF6C8EBF), Offset(w * 0.64f, h * 0.45f), Size(w * 0.26f, h * 0.1f))
        }
        "obs_garden" -> {
            drawArc(Color(0xAA4CAF50), 180f, 180f, true, Offset(w * 0.15f, h * 0.35f), Size(w * 0.7f, h * 0.6f))
            drawCircle(Color(0xFFE8F5E9), radius = w * 0.04f, center = Offset(w * 0.5f, h * 0.55f))
        }
        "obs_antenna" -> {
            drawLine(Color(0xFFCFC6BB), Offset(w * 0.5f, h * 0.9f), Offset(w * 0.5f, h * 0.3f), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Color(0xFFCFC6BB), 200f, 140f, false, Offset(w * 0.28f, h * 0.12f), Size(w * 0.44f, h * 0.36f), style = Stroke(3.dp.toPx()))
        }
        "obs_solar" -> {
            listOf(0.1f, 0.54f).forEach { x ->
                drawRect(Color(0xFF3D6FB6), Offset(w * x, h * 0.3f), Size(w * 0.36f, h * 0.4f))
                drawLine(Color(0x88FFFFFF), Offset(w * (x + 0.18f), h * 0.3f), Offset(w * (x + 0.18f), h * 0.7f), strokeWidth = 1.dp.toPx())
            }
        }
        "obs_shuttle" -> {
            drawRoundRect(Color(0xFFEDEDED), Offset(w * 0.15f, h * 0.4f), Size(w * 0.7f, h * 0.2f), CornerRadius(16f))
            drawRect(Color(0xFFD9534F), Offset(w * 0.15f, h * 0.3f), Size(w * 0.15f, h * 0.12f))
            drawCircle(Color(0xFF7FB6E8), radius = w * 0.04f, center = Offset(w * 0.72f, h * 0.5f))
        }
        "obs_starship" -> {
            val ship = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.1f, h * 0.5f); lineTo(w * 0.8f, h * 0.25f); lineTo(w * 0.92f, h * 0.5f); lineTo(w * 0.8f, h * 0.75f); close()
            }
            drawPath(ship, Color(0xFFCFC6BB))
            drawCircle(Color(0xFFF4A34B), radius = w * 0.05f, center = Offset(w * 0.78f, h * 0.5f))
        }
        "obs_comet" -> {
            drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), start = Offset(w * 0.1f, h * 0.1f), end = Offset(w * 0.7f, h * 0.6f)),
                Offset(w * 0.1f, h * 0.1f), Offset(w * 0.7f, h * 0.6f), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Color.White, radius = w * 0.07f, center = Offset(w * 0.7f, h * 0.6f))
        }
        "obs_nebula" -> drawCircle(Brush.radialGradient(listOf(Color(0xAAA070FF), Color.Transparent)), radius = w * 0.45f)
        "obs_ringed_planet" -> {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE0A3), Color(0xFFB5802B))), radius = w * 0.22f)
            drawOval(Color(0xAAFFE6B0), Offset(w * 0.1f, h * 0.42f), Size(w * 0.8f, h * 0.16f), style = Stroke(3.dp.toPx()))
        }
        "obs_moon" -> {
            drawCircle(Color(0xFFBDBDBD), radius = w * 0.26f)
            drawCircle(Color(0xFF9E9E9E), radius = w * 0.05f, center = Offset(w * 0.42f, h * 0.42f))
        }
        "obs_new_planet" -> drawCircle(Brush.radialGradient(listOf(Color(0xFFFFB86B), Color(0xFFB5502B))), radius = w * 0.3f)
        else -> drawCircle(Color(0xFF5A4A3C), radius = w * 0.25f)
    }
}
