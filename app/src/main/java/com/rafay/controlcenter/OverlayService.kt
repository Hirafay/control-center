package com.rafay.controlcenter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.abs

/**
 * Runs as a foreground service so the overlay survives.
 * - Trigger zone is pinned to the TOP-RIGHT edge only -> swipe down there opens
 *   the panel. Top-left / center stays free for Samsung's notification shade.
 *   That split is exactly how a notched iPhone behaves, and it kills the
 *   "fights with Samsung's panel" problem.
 */
class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var toggles: Toggles
    private var triggerView: View? = null
    private var panelRoot: View? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        toggles = Toggles(this)
        startAsForeground()
        addTrigger()
    }

    // ---- foreground notification (required to keep service alive) ----
    private fun startAsForeground() {
        val channelId = "rafaycc"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Rafay CC", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val n: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Control Center active")
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .build()
        startForeground(1, n)
    }

    // ---- the edge trigger ----
    private fun addTrigger() {
        val view = View(this)
        val lp = WindowManager.LayoutParams(
            dp(48), dp(140),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0; y = dp(8)
        }

        var downY = 0f
        var downX = 0f
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; downX = e.rawX; true }
                MotionEvent.ACTION_UP -> {
                    val dy = e.rawY - downY
                    val dx = abs(e.rawX - downX)
                    if (dy > dp(40) && dy > dx) showPanel()  // downward swipe
                    true
                }
                else -> true
            }
        }
        triggerView = view
        wm.addView(view, lp)
    }

    // ---- the panel ----
    private fun showPanel() {
        if (panelRoot != null) return

        // FULL-SCREEN frosted layer. Tap any empty space (or back) = close, like iOS.
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(56), dp(20), dp(24))   // top pad clears the notch/status bar
            setBackgroundColor(Color.parseColor("#E6000000"))  // full-screen frosted dark
            setOnClickListener { hidePanel() }
        }

        // big iOS title
        root.addView(TextView(this).apply {
            text = "Control Center"
            setTextColor(Color.WHITE)
            textSize = 26f
            setPadding(dp(4), 0, 0, dp(20))
        })

        // ---- toggles module (rounded iOS-style group) ----
        val togglesModule = roundedModule()
        var row: LinearLayout? = null
        toggles.all().forEachIndexed { i, c ->
            if (i % 3 == 0) {
                row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                togglesModule.addView(row, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = if (i == 0) 0 else dp(18) })
            }
            row!!.addView(makeToggle(c))
        }
        root.addView(togglesModule, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // ---- brightness module ----
        val brightModule = roundedModule()
        brightModule.addView(TextView(this).apply {
            text = "Brightness"
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 14f
            setPadding(dp(2), 0, 0, dp(8))
        })
        brightModule.addView(SeekBar(this).apply {
            max = 255
            progress = toggles.getBrightness()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) toggles.setBrightness(p)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        root.addView(brightModule, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) })

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0, // flags=0 -> focusable, so back key + tap-outside close the panel
            PixelFormat.TRANSLUCENT
        )
        panelRoot = root
        wm.addView(root, lp)
    }

    private fun roundedModule(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(16), dp(14), dp(16))
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#1AFFFFFF"))  // translucent white, iOS module look
            cornerRadius = dp(24).toFloat()
        }
    }

    private fun makeToggle(c: Toggles.Control): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val circle = View(this)
        val size = dp(56)

        fun paint() {
            val on = runCatching { c.isOn() }.getOrDefault(false)
            circle.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (on) Color.parseColor("#0A84FF") else Color.parseColor("#3A3A3C"))
            }
        }
        paint()

        circle.setOnClickListener {
            runCatching { c.toggle() }
            circle.postDelayed({ paint() }, 250) // let the system settle, then re-read
        }
        container.addView(circle, LinearLayout.LayoutParams(size, size))
        container.addView(TextView(this).apply {
            text = c.label
            setTextColor(Color.parseColor("#DDDDDD"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
        return container
    }

    private fun hidePanel() {
        panelRoot?.let { runCatching { wm.removeView(it) } }
        panelRoot = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        triggerView?.let { runCatching { wm.removeView(it) } }
        hidePanel()
    }
}
