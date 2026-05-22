package com.example.exchange_app.nav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.exchange_app.data.SettingsManager
import com.example.exchange_app.ui.screens.*
import com.example.exchange_app.ui.theme.CustomYellow
import android.content.res.Configuration
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo

@Composable
fun ExchangeAppNavigation() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    var navStack by rememberSaveable { mutableStateOf(listOf(AppDestinations.HOME)) }
    var selectedCurrency by rememberSaveable {
        mutableStateOf<String?>(settingsManager.getDefaultCurrency())
    }

    val currentDestination = navStack.last()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isTablet = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BackHandler(enabled = navStack.size > 1) {
        navStack = navStack.dropLast(1)
    }

    val navSuiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            indicatorColor = CustomYellow.copy(alpha = 0.25f)
        )
    )

    NavigationSuiteScaffold(
        layoutType = NavigationSuiteType.NavigationBar,
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                if (it != AppDestinations.DETAILS) {
                    item(
                        icon = { Icon(painterResource(it.icon), contentDescription = it.label) },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = {
                            if (it != currentDestination) {
                                navStack = if (navStack.contains(it)) {
                                    navStack.take(navStack.indexOf(it) + 1)
                                } else {
                                    navStack + it
                                }
                            }
                        },
                        colors = navSuiteItemColors
                    )
                }
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (isTablet && (currentDestination == AppDestinations.HOME || currentDestination == AppDestinations.FAVORITES)) {
                    TabletSplitLayout(isLandscape, currentDestination, selectedCurrency) { selectedCurrency = it }
                } else {
                    ScreenContent(
                        destination = currentDestination,
                        selectedCurrency = selectedCurrency,
                        onCurrencyClick = {
                            selectedCurrency = it
                            if (navStack.last() != AppDestinations.DETAILS) {
                                navStack = navStack + AppDestinations.DETAILS
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TabletSplitLayout(
    isLandscape: Boolean,
    currentDestination: AppDestinations,
    selectedCurrency: String?,
    onCurrencyClick: (String) -> Unit
) {
    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) { ScreenContent(currentDestination, selectedCurrency, onCurrencyClick) }
            VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color.Gray.copy(alpha = 0.5f))
            Box(modifier = Modifier.weight(1f)) { DetailsScreen(currencyCode = selectedCurrency) }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) { ScreenContent(currentDestination, selectedCurrency, onCurrencyClick) }
            HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color.Gray.copy(alpha = 0.5f))
            Box(modifier = Modifier.weight(1f)) { DetailsScreen(currencyCode = selectedCurrency) }
        }
    }
}

@Composable
fun ScreenContent(
    destination: AppDestinations,
    selectedCurrency: String?,
    onCurrencyClick: (String) -> Unit
) {
    when (destination) {
        AppDestinations.HOME -> MainScreen(onCurrencyClick = onCurrencyClick)
        AppDestinations.FAVORITES -> FavouritesScreen(onCurrencyClick = onCurrencyClick)
        AppDestinations.DETAILS -> DetailsScreen(currencyCode = selectedCurrency)
        AppDestinations.SETTINGS -> SettingScreen()
    }
}