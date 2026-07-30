package com.indianservers.biology

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.indianservers.biology.ui.DeviceProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceProfileTest {
    @Test
    fun televisionModeEnablesTvLayoutWithoutLeanbackHardwareFlag() {
        assertTrue(
            DeviceProfile.isTelevision(
                Configuration.UI_MODE_TYPE_TELEVISION,
                hasLeanbackFeature = false
            )
        )
    }

    @Test
    fun phoneModeKeepsTouchLayout() {
        assertFalse(
            DeviceProfile.isTelevision(
                Configuration.UI_MODE_TYPE_NORMAL,
                hasLeanbackFeature = false
            )
        )
    }
}
