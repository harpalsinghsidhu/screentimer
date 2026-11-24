package com.example.screentimer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(48, 48, 48, 48)
        
        val info = TextView(this)
        info.text = "Tap the button, find Screen Timer, and turn it on."
        col.addView(info)
        
        val open = Button(this)
        open.text = "Open accessibility settings"
        open.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        col.addView(open)
        
        setContentView(col)
    }
}
