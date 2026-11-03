package com.levelup.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.levelup.app.domain.Category
import com.levelup.app.domain.CategoryGroup

fun Category.icon(): ImageVector = when (this) {
    Category.AI_CODING -> Icons.Filled.Laptop
    Category.STUDY -> Icons.Filled.MenuBook
    Category.TRADING -> Icons.Filled.MonetizationOn
    Category.FITNESS -> Icons.Filled.FitnessCenter
    Category.WALKING -> Icons.Filled.Eco
    Category.MEALS -> Icons.Filled.Restaurant
}

fun CategoryGroup.icon(): ImageVector = when (this) {
    CategoryGroup.ALL -> Icons.Filled.GridView
    CategoryGroup.STUDY -> Icons.Filled.MenuBook
    CategoryGroup.WORK -> Icons.Filled.Laptop
    CategoryGroup.HEALTH -> Icons.Filled.FitnessCenter
    CategoryGroup.PERSONAL -> Icons.Filled.Person
    CategoryGroup.MEAL -> Icons.Filled.Restaurant
}

/** Small rounded square holding the category icon, tinted with the category colour. */
@Composable
fun CategoryIcon(category: Category, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val tint = Color(category.colorHex)
    Box(
        modifier.size(size).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.icon(), contentDescription = category.label, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}
