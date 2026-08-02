package com.indianservers.AIbiology.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkAvailability {
    const val MODEL_DOWNLOAD_WARNING =
        "No internet connection. Connect to the internet to download this model. " +
            "Copied and already-downloaded models remain available offline."

    const val CATALOG_WARNING =
        "No internet connection. Showing the saved catalogue and downloaded content."

    const val CONTENT_DOWNLOAD_WARNING =
        "No internet connection. Connect to the internet to save this content offline."

    fun isInternetAvailable(context: Context): Boolean {
        val connectivity =
            context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
