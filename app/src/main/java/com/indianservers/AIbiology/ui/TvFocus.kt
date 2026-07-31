package com.indianservers.AIbiology.ui

import android.view.View
import com.indianservers.AIbiology.R

object TvFocus {
    fun apply(view: View, focusedScale: Float = 1.05f) {
        view.isFocusable = true
        view.isFocusableInTouchMode = false
        view.foreground = view.context.getDrawable(R.drawable.bg_tv_focus_ring)
        view.setOnFocusChangeListener { focusedView, hasFocus ->
            focusedView.animate()
                .scaleX(if (hasFocus) focusedScale else 1f)
                .scaleY(if (hasFocus) focusedScale else 1f)
                .setDuration(120L)
                .start()
            focusedView.translationZ =
                if (hasFocus) 12f * focusedView.resources.displayMetrics.density else 0f
        }
    }
}
