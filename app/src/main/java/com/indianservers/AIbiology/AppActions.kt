package com.indianservers.AIbiology

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppActions {
    fun shareApp(context: Context) {
        val playStoreUrl =
            "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AI Explorer STEM - Biology")
            putExtra(
                Intent.EXTRA_TEXT,
                "Explore interactive biology, human anatomy, and virtual microscopy. " +
                    "Download AI Explorer STEM - Biology: $playStoreUrl"
            )
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Share AI Explorer Biology")
        )
    }

    fun openIndianServers(context: Context) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.indianservers.com"))
        )
    }
}
