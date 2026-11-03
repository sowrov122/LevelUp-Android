package com.levelup.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.levelup.app.AppContainer
import com.levelup.app.data.UserSettings
import com.levelup.app.domain.RewardCatalog
import com.levelup.app.ui.screens.BlockerScreen
import com.levelup.app.ui.screens.FocusScreen
import com.levelup.app.ui.screens.OnboardingScreen
import com.levelup.app.ui.screens.PremiumScreen
import com.levelup.app.ui.screens.ProgressScreen
import com.levelup.app.ui.screens.RewardsScreen
import com.levelup.app.ui.screens.RoutineScreen
import com.levelup.app.ui.screens.SettingsScreen
import com.levelup.app.ui.screens.TodayScreen
import com.levelup.app.ui.theme.LevelUpTheme
import com.levelup.app.ui.theme.AppColors
import com.levelup.app.ui.components.AppBottomBar
import com.levelup.app.ui.components.AppTopBar
import com.levelup.app.ui.components.NavItem
import com.levelup.app.ui.viewmodel.BlockerViewModel
import com.levelup.app.ui.viewmodel.FocusViewModel
import com.levelup.app.ui.viewmodel.OnboardingViewModel
import com.levelup.app.ui.viewmodel.PremiumViewModel
import com.levelup.app.ui.viewmodel.ProgressViewModel
import com.levelup.app.ui.viewmodel.RewardsViewModel
import com.levelup.app.ui.viewmodel.RoutineViewModel
import com.levelup.app.ui.viewmodel.SettingsViewModel
import com.levelup.app.ui.viewmodel.TodayViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Filled.Home),
    ROUTINE("routine", "Routine", Icons.Filled.Checklist),
    FOCUS("focus", "Focus", Icons.Filled.Timer),
    PROGRESS("progress", "Progress", Icons.Filled.BarChart),
    REWARDS("rewards", "Rewards", Icons.Filled.EmojiEvents),
}

private val NAV_ITEMS = Dest.entries.map { NavItem(it.route, it.label, it.icon) }

private const val SETTINGS_ROUTE = "settings"
private const val BLOCKER_ROUTE = "blocker"
private const val PREMIUM_ROUTE = "premium"

@Composable
private inline fun <reified VM : ViewModel> rememberVm(crossinline create: () -> VM): VM =
    viewModel(factory = viewModelFactory { initializer { create() } })

@Composable
fun AppRoot(
    container: AppContainer,
    openTaskId: String?,
    onOpenTaskConsumed: () -> Unit,
    openDest: String?,
    onOpenDestConsumed: () -> Unit,
) {
    val settings by container.settings.flow
        .map<UserSettings, UserSettings?> { it }
        .collectAsStateWithLifecycle(initialValue = null)
    val s = settings
    val accent = Color(RewardCatalog.themeColor(s?.accentThemeId ?: RewardCatalog.DEFAULT_THEME_ID))

    LevelUpTheme(accent) {
        when {
            s == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            !s.onboardingDone -> OnboardingScreen(rememberVm { OnboardingViewModel(container.repository) })
            else -> MainScaffold(container, openTaskId, onOpenTaskConsumed, openDest, onOpenDestConsumed)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    container: AppContainer,
    openTaskId: String?,
    onOpenTaskConsumed: () -> Unit,
    openDest: String?,
    onOpenDestConsumed: () -> Unit,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val onSettings = destination?.route in setOf(SETTINGS_ROUTE, BLOCKER_ROUTE, PREMIUM_ROUTE)
    val subTitle = when (destination?.route) {
        BLOCKER_ROUTE -> "Distraction protection"
        PREMIUM_ROUTE -> "Premium"
        else -> "Settings"
    }
    var pendingAdd by remember { mutableStateOf(false) }
    var immersive by remember { mutableStateOf(false) }
    val hideBars = immersive && destination?.route == Dest.FOCUS.route

    // Tapping a focus-interruption notification opens the Focus tab.
    LaunchedEffect(openDest) {
        if (openDest == "focus") {
            nav.navigate(Dest.FOCUS.route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
        }
        if (openDest != null) onOpenDestConsumed()
    }

    // Tapping a reminder opens Today with that task highlighted for a few seconds.
    LaunchedEffect(openTaskId) {
        if (openTaskId != null) {
            nav.navigate(Dest.TODAY.route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
            delay(6_000)
            onOpenTaskConsumed()
        }
    }

    fun go(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            if (!hideBars) AppTopBar(
                title = if (onSettings) subTitle else Dest.entries.firstOrNull { it.route == destination?.route }?.label ?: "Level Up",
                onBack = if (onSettings) fun() { nav.popBackStack() } else null,
                actions = {
                    if (!onSettings) IconButton(onClick = { nav.navigate(SETTINGS_ROUTE) { launchSingleTop = true } }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            if (!onSettings && !hideBars) AppBottomBar(
                items = NAV_ITEMS,
                isSelected = { item -> destination?.hierarchy?.any { it.route == item.route } == true },
                onSelect = { go(it.route) },
            )
        },
    ) { inner ->
        NavHost(nav, startDestination = Dest.TODAY.route, modifier = Modifier.padding(inner)) {
            composable(Dest.TODAY.route) {
                TodayScreen(
                    vm = rememberVm { TodayViewModel(container.repository) },
                    highlightTaskId = openTaskId,
                    onStartFocus = { go(Dest.FOCUS.route) },
                    onAddTask = { pendingAdd = true; go(Dest.ROUTINE.route) },
                    onEditRoutine = { go(Dest.ROUTINE.route) },
                )
            }
            composable(Dest.ROUTINE.route) {
                RoutineScreen(rememberVm { RoutineViewModel(container.repository) }, pendingAdd) { pendingAdd = false }
            }
            composable(Dest.FOCUS.route) {
                FocusScreen(
                    rememberVm { FocusViewModel(container.repository) },
                    onOpenBlocker = { nav.navigate(BLOCKER_ROUTE) { launchSingleTop = true } },
                    onImmersive = { immersive = it },
                )
            }
            composable(Dest.PROGRESS.route) { ProgressScreen(rememberVm { ProgressViewModel(container.repository) }) }
            composable(Dest.REWARDS.route) {
                RewardsScreen(rememberVm { RewardsViewModel(container.repository) }, onOpenPremium = { nav.navigate(PREMIUM_ROUTE) { launchSingleTop = true } })
            }
            composable(PREMIUM_ROUTE) { PremiumScreen(rememberVm { PremiumViewModel(container) }) }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(rememberVm { SettingsViewModel(container) }, onOpenBlocker = { nav.navigate(BLOCKER_ROUTE) { launchSingleTop = true } })
            }
            composable(BLOCKER_ROUTE) {
                BlockerScreen(rememberVm { BlockerViewModel(container) }, onStartFocus = { go(Dest.FOCUS.route) })
            }
        }
    }
}
