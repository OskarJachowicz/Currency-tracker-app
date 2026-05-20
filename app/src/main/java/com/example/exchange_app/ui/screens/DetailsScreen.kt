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
        "JPY" to "Jen japoński",
        "PLN" to "Złoty polski"
    )

    var selectedRange by remember { mutableIntStateOf(1) } // 1, 7, 30 dni
    val latestRate = remember(currencyCode) { 
        repository.getLatestRates().find { it.targetCurrency == currencyCode } 
    }
    val lastSyncTime = remember { repository.getLastSyncTime() }
    val decimalPlaces = settingsManager.getDecimalPlaces()
    
    // Pobieramy walutę bazową z danych (domyślnie PLN jeśli brak danych)
    val baseCurrency = latestRate?.baseCurrency ?: "PLN"

    // Ładowanie danych historycznych w tłe (poprawia stabilność przy przełączaniu zakresów)
    val historyData by produceState<List<RateModel>>(initialValue = emptyList(), currencyCode, selectedRange, baseCurrency) {
        value = withContext(Dispatchers.IO) {
            if (currencyCode != null) {
                val history = repository.getHistoryForCurrency(baseCurrency, currencyCode, selectedRange)
                val latest = latestRate
                
                // Jeśli w historii brakuje dzisiejszego wpisu (bo WorkManager jeszcze nie ruszył),
                // a mamy go w latestRate, to doklejamy go do wykresu
                if (latest != null && (history.isEmpty() || history.last().date != latest.date)) {
                    history + latest
                } else {
                    history
                }
            } else emptyList()
        }
    }

    // Obliczanie zmiany wartości względem poprzedniego dnia
    val valueChange = remember(currencyCode) {
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
        // Nagłówek: Para walutowa i Nazwa
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

        // Kurs i Data
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CustomGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (latestRate != null) String.format("%.${decimalPlaces}f", latestRate.rate) else "---",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // Wskaźnik zmiany wartości
                ValueTrendIndicator(valueChange, decimalPlaces)
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ostatnia aktualizacja: $lastSyncTime",
                    fontSize = 12.sp,
                    color = Color.Gray
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

        // Wykres
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp) // Zwiększona wysokość dla osi
                .background(CustomGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (historyData.size >= 2) {
                VicoChart(historyData, decimalPlaces)
            } else if (historyData.isEmpty() && selectedRange > 0) {
                // Pokazujemy loader podczas ładowania z tła
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CustomYellow
                )
            } else {
                Text(
                    text = "Zbyt mało danych do wykresu",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Źródło danych
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
    // 1. Tworzymy Model Producer, który zarządza danymi wykresu
    val modelProducer = remember { CartesianChartModelProducer() }

    // 2. Ładujemy dane za każdym razem, gdy lista 'data' się zmieni
    LaunchedEffect(data) {
        modelProducer.runTransaction {
            lineSeries {
                // Wyciągamy same wartości 'rate' z Twojego modelu
                series(data.map { it.rate })
            }
        }
    }

    // 3. Nowy sposób formatowania osi X (daty) z zabezpieczeniem indeksu
    val bottomAxisFormatter = remember(data) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt()
            if (index in data.indices) {
                data[index].date.takeLast(5)
            } else ""
        }
    }

    // 4. Nowy sposób formatowania osi Y (kursy)
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

    // 6. Rysowanie wykresu nowym silnikiem Cartesian
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                rangeProvider = rangeProvider
            ), // Rysuje linię
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
        modifier = modifier.height(48.dp), // Powiększona wysokość
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) CustomYellow else CustomGray,
            contentColor = if (isSelected) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
