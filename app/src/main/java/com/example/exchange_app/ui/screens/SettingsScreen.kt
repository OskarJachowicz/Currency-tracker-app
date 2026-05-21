package com.example.exchange_app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exchange_app.CurrencyApplication
import com.example.exchange_app.R
import com.example.exchange_app.data.SecurityManager
import com.example.exchange_app.data.SettingsManager
import com.example.exchange_app.data.repository.CurrencyRepository
import com.example.exchange_app.ui.theme.*
import com.example.exchange_app.utils.NetworkHelperImpl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val securityManager = remember { SecurityManager(context) }
    val app = context.applicationContext as CurrencyApplication
    val scope = rememberCoroutineScope()
    
    val repository = remember {
        CurrencyRepository(
            api = app.api,
            context = context,
            networkHelper = NetworkHelperImpl(context),
            securityManager = securityManager
        )
    }

    var apiKey by remember { mutableStateOf(securityManager.getApiKey() ?: "") }
    var baseCurrency by remember { mutableStateOf(settingsManager.getDefaultCurrency()) }
    var syncValue by remember { mutableStateOf(settingsManager.getSyncIntervalValue().toString()) }
    var syncUnit by remember { mutableStateOf(settingsManager.getSyncIntervalUnit()) }
    var decimalPlaces by remember { mutableStateOf(settingsManager.getDecimalPlaces().toString()) }


    val networkHelper = remember { NetworkHelperImpl(context) }
    val isOnline by networkHelper.observeInternetAccessibility().collectAsState(initial = networkHelper.isInternetAvailable())
    
    val currencies = listOf("PLN", "EUR", "USD")
    val units = mapOf("MINUTES" to "Minuta", "HOURS" to "Godzina", "DAYS" to "Dzień")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Ustawienia", fontSize = 42.sp, fontWeight = FontWeight.Bold)
            
            Box(
                modifier = Modifier
                    .border(
                        2.dp, 
                        if (isOnline) OnlineGreen else OfflineRed, 
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isOnline) "online" else "offline",
                    color = if (isOnline) OnlineGreen else OfflineRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        SettingsSection(title = "Waluta bazowa", showDivider = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                currencies.forEach { curr ->
                    Button(
                        onClick = {
                            baseCurrency = curr
                            settingsManager.saveDefaultCurrency(curr)
                            scope.launch {
                                repository.fetchAndSaveLatestRates(curr)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (baseCurrency == curr) CustomYellow else CustomGray,
                            contentColor = if (baseCurrency == curr) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = curr, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        SettingsSection(title = "Częstotliwość aktualizacji historii", showDivider = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = syncValue,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            syncValue = it
                            it.toIntOrNull()?.let { v -> settingsManager.saveSyncInterval(v, syncUnit) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Ilość") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1.5f)
                ) {
                    OutlinedTextField(
                        value = units[syncUnit] ?: syncUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Jednostka") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        units.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    syncUnit = key
                                    expanded = false
                                    syncValue.toIntOrNull()?.let { v -> 
                                        settingsManager.saveSyncInterval(v, key)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Text(
                text = "Wymagany restart aplikacji (min. 15 minut)",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        SettingsSection(title = "Liczba miejsc po przecinku", showDivider = true) {
            OutlinedTextField(
                value = decimalPlaces,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        val num = it.toIntOrNull() ?: 0
                        if (num <= 4) {
                            decimalPlaces = it
                            settingsManager.saveDecimalPlaces(num)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Miejsca po przecinku (maks. 4)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        Button(
            onClick = {
                scope.launch {
                    val success = repository.fetchAndSaveLatestRates(baseCurrency)
                    if (success) {
                        Toast.makeText(context, "Dane zostały odświeżone", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Wystąpił błąd przy odświeżaniu", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CustomGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_refresh),
                contentDescription = null,
                tint = CustomYellow,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Odśwież kursy teraz",
                color = CustomYellow,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

        SettingsSection(title = "Klucz API", showDivider = false) {
            var isFocused by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = apiKey,
                onValueChange = { 
                    apiKey = it
                    securityManager.saveApiKey(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                label = { Text("Wpisz swój klucz API") },
                singleLine = true,
                placeholder = { Text("Klucz z exchangerate-api.com", modifier = Modifier.alpha(0.5f)) },
                visualTransformation = if (isFocused) VisualTransformation.None else PasswordVisualTransformation()
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingsSection(
    title: String, 
    showDivider: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.LightGray)
        content()
        if (showDivider) {
            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
        }
    }
}
