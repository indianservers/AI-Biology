package com.indianservers.AIbiology

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.indianservers.AIbiology.databinding.ActivityMainBinding
import com.indianservers.AIbiology.ui.DeviceProfile

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        DeviceProfile.setDebugTelevisionOverride(
            BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_FORCE_TV_LAYOUT, false)
        )
        if (DeviceProfile.isTelevision(this)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.contentMain.poweredByLink.text = AppActions.copyrightNotice(this)
        binding.contentMain.poweredByLink.setOnClickListener {
            AppActions.openIndianServers(this)
        }
        binding.contentMain.globalShareButton.setOnClickListener {
            AppActions.shareApp(this)
        }
    }

    private companion object {
        const val EXTRA_FORCE_TV_LAYOUT = "force_tv_layout"
    }
}
