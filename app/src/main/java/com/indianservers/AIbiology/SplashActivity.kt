package com.indianservers.AIbiology

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.indianservers.AIbiology.data.CatalogRefreshCoordinator
import com.indianservers.AIbiology.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var binding: ActivitySplashBinding
    private val openMain = Runnable {
        if (!isFinishing) {
            startActivity(
                Intent(this, MainActivity::class.java),
                ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
            )
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.splashCopyright.text = AppActions.copyrightNotice(this)
        CatalogRefreshCoordinator.refreshAll(applicationContext)
        handler.postDelayed(openMain, SPLASH_DURATION_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(openMain)
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DURATION_MS = 500L
    }
}
