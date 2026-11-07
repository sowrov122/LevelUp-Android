package com.levelup.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.levelup.app.ui.theme.AppColors
import com.levelup.app.ui.theme.Radii
import com.levelup.app.ui.theme.Sizes
import com.levelup.app.ui.theme.Spacing

// ---------------------------------------------------------------- cards

/** Standard card: #302117 surface, 1dp subtle border, no shadow. [elevated] uses the raised #3A2A20 surface. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    shape: Shape = RoundedCornerShape(Radii.card),
    border: BorderStroke? = BorderStroke(1.dp, AppColors.Border),
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor ?: if (elevated) AppColors.Elevated else AppColors.Card,
            contentColor = AppColors.TextPrimary,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = border,
        content = content,
    )
}

// -------------------------------------------------------------- buttons

/** Main action on a screen: solid lavender, off-white label. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color = AppColors.Lavender,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(Sizes.buttonHeight),
        shape = RoundedCornerShape(Radii.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = AppColors.TextPrimary,
            disabledContainerColor = AppColors.Elevated,
            disabledContentColor = AppColors.TextSecondary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

/** Secondary action: outlined with the subtle border. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(Sizes.buttonHeight),
        shape = RoundedCornerShape(Radii.button),
        border = BorderStroke(1.dp, AppColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextPrimary, disabledContentColor = AppColors.TextSecondary),
    ) { Text(text, fontWeight = FontWeight.Medium) }
}

/** Low-emphasis text action, coloured with the progress accent so it stays readable on dark surfaces. */
@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) { Text(text, color = AppColors.ProgressBlue) }
}

// ---------------------------------------------------------------- chips

@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selectedColor: Color = AppColors.Lavender,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (icon != null) ({ Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
        modifier = modifier,
        shape = RoundedCornerShape(Radii.chip),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = AppColors.Card,
            labelColor = AppColors.TextSecondary,
            selectedContainerColor = selectedColor,
            selectedLabelColor = AppColors.TextPrimary,
        ),
    )
}

/** Switch with lavender "on" state. */
@Composable
fun appSwitchColors() = androidx.compose.material3.SwitchDefaults.colors(
    checkedThumbColor = AppColors.TextPrimary,
    checkedTrackColor = AppColors.Lavender,
    uncheckedThumbColor = AppColors.TextSecondary,
    uncheckedTrackColor = AppColors.Elevated,
    uncheckedBorderColor = AppColors.Border,
)

/** Square icon button with the subtle border, used in screen headers. */
@Composable
fun IconTile(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, active: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .size(44.dp)
            .clip(shape)
            .background(if (active) AppColors.Lavender else AppColors.Card)
            .border(1.dp, if (active) AppColors.Lavender else AppColors.Border, shape)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = description, tint = AppColors.TextPrimary, modifier = Modifier.size(22.dp)) }
}

// ---------------------------------------------------------------- icons

/** Round badge holding a vector icon or a short text/emoji glyph. */
@Composable
fun IconBadge(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    glyph: String? = null,
    contentDescription: String? = null,
    tint: Color = AppColors.TextPrimary,
    background: Color = AppColors.Elevated,
    size: Dp = Sizes.badge,
) {
    Box(modifier.size(size).background(background, CircleShape), contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.55f))
        else if (glyph != null) Text(glyph, color = tint, fontWeight = FontWeight.Bold)
    }
}

/** Illustration from [com.levelup.app.ui.theme.Art]. Pass a description only when the picture carries meaning. */
@Composable
fun ArtImage(
    @androidx.annotation.DrawableRes id: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
) {
    Image(painterResource(id), contentDescription = contentDescription, modifier = modifier, contentScale = contentScale, alignment = alignment)
}

/** Streak flame illustration. */
@Composable
fun FlameIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(com.levelup.app.ui.theme.Art.StreakFlame), contentDescription = null, modifier = modifier)
}

// -------------------------------------------------------------- headers

/** Big heading at the top of a screen's content. */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        }
        trailing?.invoke()
    }
}

/** Heading above a group of cards, with an optional text action on the right. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
        )
        if (actionLabel != null && onAction != null) LinkButton(actionLabel, onAction)
    }
}

// ----------------------------------------------------------- navigation

data class NavItem(val route: String, val label: String, val icon: ImageVector)

/** Top bar: #21140F with off-white title, optional back arrow and action slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.Header,
            titleContentColor = AppColors.TextPrimary,
            navigationIconContentColor = AppColors.TextPrimary,
            actionIconContentColor = AppColors.TextPrimary,
        ),
    )
}

/** Bottom navigation: #21140F, lavender indicator behind the selected item. */
@Composable
fun AppBottomBar(items: List<NavItem>, isSelected: (NavItem) -> Boolean, onSelect: (NavItem) -> Unit) {
    NavigationBar(containerColor = AppColors.Header, contentColor = AppColors.TextPrimary) {
        items.forEach { item ->
            NavigationBarItem(
                selected = isSelected(item),
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.TextPrimary,
                    selectedTextColor = AppColors.TextPrimary,
                    indicatorColor = AppColors.Lavender,
                    unselectedIconColor = AppColors.TextSecondary,
                    unselectedTextColor = AppColors.TextSecondary,
                ),
            )
        }
    }
}
