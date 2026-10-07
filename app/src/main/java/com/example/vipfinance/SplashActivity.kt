package com.example.vipfinance

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(4, 48, 18)
        window.navigationBarColor = Color.rgb(4, 48, 18)

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(3, 35, 13), Color.rgb(8, 104, 35), Color.rgb(35, 218, 88))
            )
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.vip_finance_icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        root.addView(icon, FrameLayout.LayoutParams(dp(230), dp(230), Gravity.CENTER).apply {
            bottomMargin = dp(55)
        })

        val title = TextView(this).apply {
            text = "VIP Finance"
            setTextColor(Color.WHITE)
            textSize = 28f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(title, FrameLayout.LayoutParams(-1, dp(48), Gravity.CENTER).apply {
            topMargin = dp(255)
            leftMargin = dp(24)
            rightMargin = dp(24)
        })

        val subtitle = TextView(this).apply {
            text = "Ваши финансы под контролем"
            setTextColor(Color.argb(220, 235, 255, 238))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, FrameLayout.LayoutParams(-1, dp(32), Gravity.CENTER).apply {
            topMargin = dp(310)
            leftMargin = dp(24)
            rightMargin = dp(24)
        })

        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2500L)
    }
}