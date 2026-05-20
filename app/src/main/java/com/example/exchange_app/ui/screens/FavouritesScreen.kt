package com.example.exchange_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exchange_app.CurrencyApplication
import com.example.exchange_app.R
import com.example.exchange_app.data.SecurityManager
import com.example.exchange_app.data.repository.CurrencyRepository
import com.example.exchange_app.utils.NetworkHelperImpl
import com.example.exchange_app.ui.theme.CustomGray
import com.example.exchange_app.ui.theme.CustomYellow

@Composable
fun FavouritesScreen(
    modifier: Modifier = Modifier,
    onCurrencyClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CurrencyApplication
    
    val repository = remember {
        CurrencyRepository(
            api = app.api,
            context = context,
            networkHelper = NetworkHelperImpl(context),
            securityManager = SecurityManager(context)
        )
    }

    var followedCurrencies by remember { mutableStateOf(repository.getFollowedCurrencies()) }
    var searchQuery by remember { mutableStateOf("") }

    val availableCurrencies = remember {
        val fromHistory = repository.getAllAvailableCurrencies()
        fromHistory.ifEmpty { listOf("USD", "EUR", "GBP", "CHF", "JPY", "AUD", "CAD", "PLN") }
    }

    val filteredCurrencies = remember(searchQuery, availableCurrencies) {
        availableCurrencies.filter { 
            it.contains(searchQuery, ignoreCase = true) 
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Obserwuj waluty",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = {
                Text(
                    text = "Szukaj waluty (np. EUR, PLN)",
                    modifier = Modifier.alpha(0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Search",
                    modifier = Modifier.size(24.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.LightGray
            )
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCurrencies) { currencyCode ->
                CurrencyItem(
                    currencyCode = currencyCode,
                    isFollowed = followedCurrencies.contains(currencyCode),
                    onToggleFollow = {
                        repository.toggleFollowedCurrency(currencyCode)
                        followedCurrencies = repository.getFollowedCurrencies()
                    },
                    onClick = { onCurrencyClick(currencyCode) }
                )
            }
        }
    }
}

@Composable
fun CurrencyItem(
    currencyCode: String,
    isFollowed: Boolean,
    onToggleFollow: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(CustomGray, RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        if (isFollowed) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(CustomYellow, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currencyCode,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium
            )
            
            Icon(
                painter = if (isFollowed) painterResource(id = R.drawable.ic_star_followed) else painterResource(id = R.drawable.ic_favourite),
                contentDescription = "Follow $currencyCode",
                tint = if (isFollowed) CustomYellow else Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onToggleFollow()
                    }
            )
        }
    }
}
