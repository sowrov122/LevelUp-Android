package com.levelup.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.domain.CORE_FREE_FEATURES
import com.levelup.app.domain.EntitlementState
import com.levelup.app.domain.PremiumFeature
import com.levelup.app.ui.components.SectionTitle
import com.levelup.app.ui.viewmodel.PremiumViewModel

@Composable
fun PremiumScreen(vm: PremiumViewModel) {
    val e by vm.entitlement.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (e.state == EntitlementState.PREMIUM) "Plan: Premium" else "Plan: Free",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (e.billingAvailable) "Purchases are available."
                    else "Premium cannot be purchased in this version, so nothing is locked. " +
                        "Everything listed on this screen is available to you for free.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionTitle("Always free")
        CORE_FREE_FEATURES.forEach { Text("• $it") }

        SectionTitle("Possible premium features later")
        PremiumFeature.entries.forEach { f ->
            Column {
                Text(f.title, fontWeight = FontWeight.SemiBold)
                Text(f.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            "Billing is not implemented yet. When it is, purchases will be verified with Google Play before anything unlocks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
