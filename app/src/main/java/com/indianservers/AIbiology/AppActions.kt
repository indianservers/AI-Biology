package com.indianservers.AIbiology

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.time.Year

object AppActions {
    private const val PLAY_STORE_URL =
        "https://play.google.com/store/apps/details?id=com.indianservers.AIbiology"

    fun copyrightNotice(context: Context): String =
        context.getString(R.string.indian_servers_copyright, Year.now().value)

    fun shareApp(context: Context) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app_subject))
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(R.string.share_app_description, PLAY_STORE_URL)
            )
        }
        context.startActivity(
            Intent.createChooser(shareIntent, context.getString(R.string.share_app_chooser))
        )
    }

    fun openIndianServers(context: Context) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.indianservers.com"))
        )
    }
}
