package com.levelup.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.domain.RewardCatalog
import com.levelup.app.domain.ShopCategory
import com.levelup.app.domain.ShopItem
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.ProgressBar
import com.levelup.app.ui.components.SectionTitle
import com.levelup.app.ui.viewmodel.RewardsViewModel

@Composable
fun RewardsScreen(vm: RewardsViewModel, onOpenPremium: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<ShopItem?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        toast?.let {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it, Modifier.weight(1f).padding(vertical = 12.dp))
                    TextButton(onClick = { vm.consumeToast() }) { Text("OK") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Level ${s.level.level}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                ProgressBar(s.level.fraction)
                Text("${s.level.xpIntoLevel} / ${s.level.xpForNextLevel} XP · total ${s.level.totalXp} XP")
                Text(
                    "XP needed to advance from level L = 100 + L × 25. Level is always derived from your total XP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Coins: ${s.coins}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    Text("Plan: Free", fontWeight = FontWeight.SemiBold)
                    Text("Premium is not available yet; nothing is locked.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenPremium) { Text("Details") }
            }
        }

        SectionTitle("Themes")
        ThemeRow("Ember (default)", "Warm amber accent.", s.equippedTheme == RewardCatalog.DEFAULT_THEME_ID, owned = true,
            price = 0, onEquip = { vm.equip(RewardCatalog.DEFAULT_THEME_ID) }, onBuy = {})
        RewardCatalog.items.filter { it.category == ShopCategory.THEME }.forEach { item ->
            ThemeRow(item.title, item.description, s.equippedTheme == item.id, item.id in s.owned, item.priceCoins,
                onEquip = { vm.equip(item.id) }, onBuy = { confirm = item })
        }

        SectionTitle("Observatory decorations")
        RewardCatalog.items.filter { it.category == ShopCategory.OBSERVATORY }.forEach { item ->
            ThemeRow(item.title, item.description, equipped = false, owned = item.id in s.owned, price = item.priceCoins,
                onEquip = {}, onBuy = { confirm = item }, showEquip = false)
        }

        SectionTitle("Recent ledger")
        if (s.recent.isEmpty()) {
            Text("Nothing earned yet. Complete a task to start your ledger.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        s.recent.forEach { e ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(e.note ?: e.referenceId, style = MaterialTheme.typography.bodyMedium)
                    Text(e.localDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "${signed(e.pointsDelta)} pts · ${signed(e.xpDelta)} XP · ${signed(e.coinsDelta)} c",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    confirm?.let { item ->
        ConfirmDialog(
            "Buy ${item.title}?",
            "This costs ${item.priceCoins} coins. You have ${s.coins}.",
            "Buy",
            onConfirm = { vm.buy(item.id); confirm = null },
            onDismiss = { confirm = null },
        )
    }
}

private fun signed(v: Int) = if (v > 0) "+$v" else v.toString()

@Composable
private fun ThemeRow(
    title: String,
    description: String,
    equipped: Boolean,
    owned: Boolean,
    price: Int,
    onEquip: () -> Unit,
    onBuy: () -> Unit,
    showEquip: Boolean = true,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                owned && showEquip && equipped -> Text("Equipped", color = MaterialTheme.colorScheme.tertiary)
                owned && showEquip -> OutlinedButton(onClick = onEquip) { Text("Equip") }
                owned -> Text("Owned", color = MaterialTheme.colorScheme.tertiary)
                else -> Button(onClick = onBuy) { Text("$price coins") }
            }
        }
    }
}
