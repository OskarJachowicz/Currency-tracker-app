package com.example.exchange_app.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exchange_app.CurrencyApplication
import com.example.exchange_app.R
import com.example.exchange_app.data.SecurityManager
import com.example.exchange_app.data.SettingsManager
import com.example.exchange_app.data.model.RateModel
import com.example.exchange_app.data.repository.CurrencyRepository
import com.example.exchange_app.ui.theme.*
import com.example.exchange_app.utils.NetworkHelperImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onCurrencyClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CurrencyApplication
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val repository = remember {
        CurrencyRepository(
            api = app.api,
            context = context,
            networkHelper = NetworkHelperImpl(context),
            securityManager = SecurityManager(context)
        )
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    

    val syncSignal by CurrencyRepository.syncSignal.collectAsState()

    val followedCurrencies = remember(refreshTrigger, syncSignal) { repository.getFollowedCurrencies() }
    val latestRates = remember(refreshTrigger, syncSignal) { repository.getLatestRates() }
    val lastSyncTime = remember(refreshTrigger, syncSignal) { repository.getLastSyncTime() }

    val networkHelper = remember { NetworkHelperImpl(context) }
    val isOnline by networkHelper.observeInternetAccessibility().collectAsState(initial = networkHelper.isInternetAvailable())

    val decimalPlaces = settingsManager.getDecimalPlaces()

    val followedRates = remember(followedCurrencies, latestRates) {
        followedCurrencies.mapNotNull { currencyCode ->
            latestRates.find { it.targetCurrency == currencyCode }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                val base = settingsManager.getDefaultCurrency()
                repository.fetchAndSaveLatestRates(base)
                refreshTrigger++
                delay(400)
                isRefreshing = false
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kursy walut",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = if (isOnline) OnlineGreen else OfflineRed,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isOnline) "online" else "offline",
                        color = if (isOnline) OnlineGreen else OfflineRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Text(
                text = "Czas ostatniej aktualizacji: $lastSyncTime",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (followedRates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Nie obserwujesz żadnych walut (pociągnij, aby odświeżyć)")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(followedRates) { rate ->
                        FollowedRateItem(
                            rate = rate,
                            decimalPlaces = decimalPlaces,
                            onClick = { onCurrencyClick(rate.targetCurrency) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FollowedRateItem(
    rate: RateModel,
    decimalPlaces: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CustomGray, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = rate.targetCurrency,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = rate.date,
                fontSize = 14.sp,
                color = Color.LightGray
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TrendIndicator(rate.trend)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = String.format("%.${decimalPlaces}f", rate.rate),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun TrendIndicator(trend: Double?) {
    val (color, icon, text) = when {
        trend == null -> Triple(TrendGray, R.drawable.ic_trending_flat, "0.00%")
        trend > 0.01 -> Triple(TrendGreen, R.drawable.ic_trending_up, String.format("+%.2f%%", trend))
        trend < -0.01 -> Triple(TrendRed, R.drawable.ic_trending_down, String.format("%.2f%%", trend))
        else -> Triple(TrendYellow, R.drawable.ic_trending_flat, "0.00%")
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
