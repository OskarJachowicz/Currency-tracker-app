package com.example.exchange_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.lifecycleScope
import com.example.exchange_app.ui.theme.Exchange_appTheme

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.exchange_app.data.SecurityManager
import com.example.exchange_app.data.SettingsManager
import com.example.exchange_app.data.repository.CurrencyRepository
import com.example.exchange_app.ui.screens.DetailsScreen
import com.example.exchange_app.ui.screens.FavouritesScreen
import com.example.exchange_app.ui.screens.MainScreen
import com.example.exchange_app.ui.screens.SettingScreen
import com.example.exchange_app.ui.theme.CustomYellow
import com.example.exchange_app.ui.theme.LightNavBarGreen
import com.example.exchange_app.utils.NetworkHelperImpl
import com.example.exchange_app.worker.DailyRateWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.content.res.Configuration
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val securityManager = SecurityManager(this)
        val settingsManager = SettingsManager(this)
        setupWorkManager(settingsManager)

        val repository = CurrencyRepository(
            (applicationContext as CurrencyApplication).api,
            this,
            NetworkHelperImpl(this),
            securityManager
        )
        lifecycleScope.launch {
            if (repository.isOnline() && securityManager.getApiKey() != null) {
                repository.fetchAndSaveLatestRates(settingsManager.getDefaultCurrency())
            }
        }

        enableEdgeToEdge()
        setContent {
            Exchange_appTheme {
                Exchange_appApp()
            }
        }
    }

    private fun setupWorkManager(settings: SettingsManager) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val intervalValue = settings.getSyncIntervalValue()
        val intervalUnitStr = settings.getSyncIntervalUnit()
        
        val timeUnit = when (intervalUnitStr) {
            "MINUTES" -> TimeUnit.MINUTES
            "HOURS" -> TimeUnit.HOURS
            "DAYS" -> TimeUnit.DAYS
            else -> TimeUnit.HOURS
        }

        val finalValue = if (timeUnit == TimeUnit.MINUTES && intervalValue < 15) 15L else intervalValue.toLong()

        val syncRequest = PeriodicWorkRequestBuilder<DailyRateWorker>(finalValue, timeUnit)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyCurrencyFetch",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }
}

@PreviewScreenSizes
@Composable
fun Exchange_appApp() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    var navStack by rememberSaveable { mutableStateOf(listOf(AppDestinations.HOME)) }
    var selectedCurrency by rememberSaveable { 
        mutableStateOf<String?>(settingsManager.getDefaultCurrency()) 
    }

    val currentDestination = navStack.last()
    val configuration = LocalConfiguration.current
    
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BackHandler(enabled = navStack.size > 1) {
        navStack = navStack.dropLast(1)
    }

    val navSuiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            indicatorColor = CustomYellow.copy(alpha = 0.8f)
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            indicatorColor = CustomYellow.copy(alpha = 0.8f)
        )
    )

    NavigationSuiteScaffold(
        layoutType = NavigationSuiteType.NavigationBar,
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                if (it != AppDestinations.DETAILS) {
                    item(
                        icon = {
                            Icon(
                                painterResource(it.icon),
                                contentDescription = it.label
                            )
                        },
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
            Box(modifier = Modifier.padding(innerPadding)){
                if (isTablet && (currentDestination == AppDestinations.HOME || currentDestination == AppDestinations.FAVORITES)) {
                    if (isLandscape) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                ScreenContent(
                                    destination = currentDestination,
                                    selectedCurrency = selectedCurrency,
                                    onCurrencyClick = { selectedCurrency = it }
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color.Gray.copy(alpha = 0.5f)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                DetailsScreen(currencyCode = selectedCurrency)
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                ScreenContent(
                                    destination = currentDestination,
                                    selectedCurrency = selectedCurrency,
                                    onCurrencyClick = { selectedCurrency = it }
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth().height(1.dp),
                                color = Color.Gray.copy(alpha = 0.5f)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                DetailsScreen(currencyCode = selectedCurrency)
                            }
                        }
                    }
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

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Dom", R.drawable.ic_home),
    FAVORITES("Obserwowane", R.drawable.ic_favourite),
    DETAILS("Szczegóły", R.drawable.ic_details),
    SETTINGS("Ustawienia", R.drawable.ic_settings),
}
