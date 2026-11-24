package com.example.screentimer

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimerService : AccessibilityService() {
    
    private val handler = Handler(Looper.getMainLooper())
    private var box: TextView? = null
    private var attached = false
    private var running = false
    private var last = 0L      // last tick time (elapsedRealtime)
    private var total = 0L     // screen-on milliseconds today
    private var day = ""       // date the total belongs to
    
    private fun prefs() = getSharedPreferences("st", MODE_PRIVATE)
    
    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        
    private fun load() {
        day = prefs().getString("day", "") ?: ""
        total = prefs().getLong("ms", 0L)
        if (day != today()) {
            day = today()
            total = 0L
            save()
        }
    }
    
    private fun save() {
        prefs().edit().putString("day", day).putLong("ms", total).apply()
    }
    
    private fun format(ms: Long): String {
        val s = ms / 1000
        return String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }
    
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val now = SystemClock.elapsedRealtime()
            if (day != today()) {   // midnight passed: start a new day
                day = today()
                total = 0L
            }
            total += now - last
            last = now
            save()
            box?.text = format(total)
            handler.postDelayed(this, 1000)
        }
    }
    
    private fun start() {
        if (running) return
        running = true
        last = SystemClock.elapsedRealtime()
        handler.post(tick)
    }
    
    private fun stop() {
        if (!running) return
        total += SystemClock.elapsedRealtime() - last
        running = false
        handler.removeCallbacks(tick)
        save()
        box?.text = format(total)
    }
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == Intent.ACTION_SCREEN_ON) start() else stop()
        }
    }
    
    override fun onServiceConnected() {
        load()
        
        val tv = TextView(this)
        tv.setTextColor(Color.CYAN)
        tv.setBackgroundColor(0xB3000000.toInt())
        tv.textSize = 14f
        tv.setPadding(16, 8, 16, 8)
        tv.text = format(total)
        box = tv
        
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.END
        p.y = 320   // sits below the YT Counter box if both are on
        (getSystemService(WINDOW_SERVICE) as WindowManager).addView(tv, p)
        attached = true
        
        val f = IntentFilter()
        f.addAction(Intent.ACTION_SCREEN_ON)
        f.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(receiver, f)
        
        if ((getSystemService(POWER_SERVICE) as PowerManager).isInteractive) start()
    }
    
    override fun onAccessibilityEvent(e: AccessibilityEvent) {}
    
    override fun onInterrupt() {}
    
    override fun onDestroy() {
        stop()
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        if (attached) {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(box)
            attached = false
        }
        super.onDestroy()
    }
}

