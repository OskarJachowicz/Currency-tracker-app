package com.example.exchange_app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

interface NetworkHelper {
    fun isInternetAvailable(): Boolean
}

class NetworkHelperImpl(private val context: Context) : NetworkHelper {
    override fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}