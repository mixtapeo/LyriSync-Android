package com.example.lyrisync

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Using SwitchCompat here
        val switchAutoScroll = findViewById<SwitchCompat>(R.id.switchAutoScroll)
        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)

        val sharedPrefs = getSharedPreferences("LyriSyncPrefs", Context.MODE_PRIVATE)

        // Load saved state
        switchAutoScroll.isChecked = sharedPrefs.getBoolean("AUTO_SYNC", true)

        // Save state on toggle
        switchAutoScroll.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("AUTO_SYNC", isChecked).apply()
        }

        // Handle the Clear action
        btnClearHistory.setOnClickListener {
            sharedPrefs.edit().putBoolean("WIPE_REQUESTED", true).apply()
            Toast.makeText(this, "History cleared!", Toast.LENGTH_SHORT).show()
        }
    }
}