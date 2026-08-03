package com.indianservers.AIbiology.data

import android.app.ActivityManager
import android.content.Context

enum class RenderQualityProfile(val queryValue: String) {
    LOW("low"),
    BALANCED("balanced"),
    HIGH("high");

    companion object {
        fun forDevice(context: Context): RenderQualityProfile {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryClass = activityManager.memoryClass
            val processors = Runtime.getRuntime().availableProcessors()
            return when {
                activityManager.isLowRamDevice || memoryClass <= 192 || processors <= 4 -> LOW
                memoryClass >= 384 && processors >= 8 -> HIGH
                else -> BALANCED
            }
        }
    }
}
