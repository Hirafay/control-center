package com.rafay.controlcenter

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#101012"))
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val status = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 16f
            gravity = Gravity.CENTER
        }
        root.addView(status)

        val start = Button(this).apply {
            text = "Start Control Center"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ))
                    Toast.makeText(this@MainActivity,
                        "Grant 'Display over other apps', then tap Start again",
                        Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val svc = Intent(this@MainActivity, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(svc) else startService(svc)
                Toast.makeText(this@MainActivity,
                    "Running. Swipe down from the TOP-RIGHT edge.",
                    Toast.LENGTH_LONG).show()
            }
        }
        val stop = Button(this).apply {
            text = "Stop"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        }
        root.addView(start, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(24) })
        root.addView(stop, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        setContentView(root)

        // root check on launch so you know immediately if su was denied
        Thread {
            val ok = Root.isAvailable()
            runOnUiThread {
                status.text = if (ok)
                    "Root: GRANTED ✓\nToggles will work."
                else
                    "Root: NOT granted ✗\nGrant su to this app in KernelSU,\nor toggles will do nothing."
            }
        }.start()
    }
}
