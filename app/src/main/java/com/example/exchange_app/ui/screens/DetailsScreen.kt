package com.example.exchange_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.exchange_app.CurrencyApplication
import com.example.exchange_app.R
import com.example.exchange_app.data.SecurityManager
import com.example.exchange_app.data.SettingsManager
import com.example.exchange_app.data.model.RateModel
import com.example.exchange_app.data.repository.CurrencyRepository
import com.example.exchange_app.ui.theme.*
import com.example.exchange_app.utils.NetworkHelperImpl
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier,
    currencyCode: String? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val app = context.applicationContext as CurrencyApplication
    val settingsManager = remember { SettingsManager(context) }
    
    val repository = remember {
        CurrencyRepository(
            app.api, context, NetworkHelperImpl(context), SecurityManager(context)
        )
    }

    val currencyNames = mapOf(
        "USD" to "Dolar amerykański",
        "EUR" to "Euro",
        "GBP" to "Funt brytyjski",
        "PLN" to "Złoty polski"
    )

    var selectedRange by rememberSaveable { mutableIntStateOf(1) }

    val syncSignal by CurrencyRepository.syncSignal.collectAsState()

    val latestRate = remember(currencyCode, syncSignal) { 
        repository.getLatestRates().find { it.targetCurrency == currencyCode } 
    }
    val lastSyncTime = remember(syncSignal) { repository.getLastSyncTime() }
    val decimalPlaces = settingsManager.getDecimalPlaces()

    val baseCurrency = latestRate?.baseCurrency ?: "PLN"

    var historyData by remember { mutableStateOf<List<RateModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(currencyCode, selectedRange, baseCurrency, latestRate, syncSignal) {
        isLoading = true
        val newData = withContext(Dispatchers.IO) {
            if (currencyCode != null) {
                val history = repository.getHistoryForCurrency(baseCurrency, currencyCode, selectedRange)
                val latest = latestRate

                if (latest != null && (history.isEmpty() || history.last().date != latest.date)) {
                    history + latest
                } else {
                    history
                }
            } else emptyList()
        }
        historyData = newData
        isLoading = false
    }

    val valueChange = remember(currencyCode, syncSignal) {
        val historicalRates = repository.getHistoricalRates()
        val latest = latestRate
        if (latest != null) {
            val previousRate = historicalRates
                .filter { 
                    it.baseCurrency == latest.baseCurrency && 
                    it.targetCurrency == latest.targetCurrency && 
                    it.date < latest.date 
                }
                .maxByOrNull { it.date }
            
            if (previousRate != null) latest.rate - previousRate.rate else null
        } else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (currencyCode != null) "$currencyCode/$baseCurrency" else "???",
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = currencyNames[currencyCode] ?: "Waluta zagraniczna",
            fontSize = 18.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isLandscape) Modifier.heightIn(min = 180.dp) else Modifier),
            colors = CardDefaults.cardColors(containerColor = CustomGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (latestRate != null) String.format("%.${decimalPlaces}f", latestRate.rate) else "---",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                ValueTrendIndicator(valueChange, decimalPlaces)
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ostatnia aktualizacja: $lastSyncTime",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RangeButton(
                label = "Dzień", 
                isSelected = selectedRange == 1, 
                modifier = Modifier.weight(1f)
            ) { selectedRange = 1 }
            
            RangeButton(
                label = "Tydzień", 
                isSelected = selectedRange == 7, 
                modifier = Modifier.weight(1f)
            ) { selectedRange = 7 }
            
            RangeButton(
                label = "Miesiąc", 
                isSelected = selectedRange == 30, 
                modifier = Modifier.weight(1f)
            ) { selectedRange = 30 }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(CustomGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (historyData.size >= 2) {
                key(historyData.size) {
                    VicoChart(historyData, decimalPlaces)
                }
            } else if (!isLoading) {
                Text(
                    text = "Zbyt mało danych do wykresu",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            }

            if (isLoading && historyData.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CustomYellow
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Źródło danych: exchangerate-api.com",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun VicoChart(data: List<RateModel>, decimalPlaces: Int) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            lineSeries {
                series(data.map { it.rate })
            }
        }
    }

    val bottomAxisFormatter = remember(data) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt()
            if (index in data.indices) {
                data[index].date.takeLast(5)
            } else ""
        }
    }

    val startAxisFormatter = remember(decimalPlaces) {
        CartesianValueFormatter { _, value, _ ->
            String.format("%.${decimalPlaces}f", value)
        }
    }

    val rates = data.map { it.rate }
    val minVal = rates.minOrNull() ?: 0.0
    val maxVal = rates.maxOrNull() ?: 1.0

    val rangeProvider = remember(minVal, maxVal) {
        val mid = (minVal + maxVal) / 2.0
        CartesianLayerRangeProvider.fixed(
            minY = mid - 1.0,
            maxY = mid + 1.0
        )
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                rangeProvider = rangeProvider
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = startAxisFormatter
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = bottomAxisFormatter
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ValueTrendIndicator(change: Double?, decimalPlaces: Int) {
    val (color, icon, text) = when {
        change == null -> Triple(TrendGray, R.drawable.ic_trending_flat, String.format("%.${decimalPlaces}f", 0.0))
        change > 0.0001 -> Triple(TrendGreen, R.drawable.ic_trending_up, String.format("+%.${decimalPlaces}f", change))
        change < -0.0001 -> Triple(TrendRed, R.drawable.ic_trending_down, String.format("%.${decimalPlaces}f", change))
        else -> Triple(TrendYellow, R.drawable.ic_trending_flat, String.format("%.${decimalPlaces}f", 0.0))
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
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

@Composable
fun RangeButton(
    label: String, 
    isSelected: Boolean, 
    modifier: Modifier = Modifier, 
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) CustomYellow else CustomGray,
            contentColor = if (isSelected) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
