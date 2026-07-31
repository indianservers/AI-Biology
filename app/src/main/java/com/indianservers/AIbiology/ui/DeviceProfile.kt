package com.indianservers.AIbiology.ui

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageManager

object DeviceProfile {
    private var debugTelevisionOverride = false

    fun setDebugTelevisionOverride(enabled: Boolean) {
        debugTelevisionOverride = enabled
    }

    fun isTelevision(context: Context): Boolean {
        if (debugTelevisionOverride) return true
        val uiModeManager = context.getSystemService(UiModeManager::class.java)
        return isTelevision(
            uiModeManager?.currentModeType ?: Configuration.UI_MODE_TYPE_UNDEFINED,
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        )
    }

    fun isTelevision(modeType: Int, hasLeanbackFeature: Boolean): Boolean =
        modeType == Configuration.UI_MODE_TYPE_TELEVISION || hasLeanbackFeature
}
