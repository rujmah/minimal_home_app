package com.minimal.home

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var backgroundColorButton: Button
    private lateinit var clockAppButton: Button
    private lateinit var calendarAppButton: Button
    private lateinit var notionApiKeyInput: EditText
    private lateinit var notionDatabaseIdInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Enable edge-to-edge display with system bars visible
        WindowCompat.setDecorFitsSystemWindows(window, true)

        prefs = getSharedPreferences("MinimalHomePrefs", MODE_PRIVATE)

        // Initialize views
        backgroundColorButton = findViewById(R.id.backgroundColorButton)
        clockAppButton = findViewById(R.id.clockAppButton)
        calendarAppButton = findViewById(R.id.calendarAppButton)
        notionApiKeyInput = findViewById(R.id.notionApiKeyInput)
        notionDatabaseIdInput = findViewById(R.id.notionDatabaseIdInput)

        // Load saved values
        loadSettings()

        // Set click listeners
        backgroundColorButton.setOnClickListener { showColorPicker() }
        clockAppButton.setOnClickListener { showAppPicker(true) }
        calendarAppButton.setOnClickListener { showAppPicker(false) }

        // Save Notion credentials on text change
        findViewById<Button>(R.id.saveNotionButton).setOnClickListener {
            saveNotionSettings()
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        // Load background color
        val bgColor = prefs.getInt("background_color", Color.BLACK)
        updateBackgroundColorButton(bgColor)

        // Load selected apps
        val clockApp = prefs.getString("clock_app", null)
        val calendarApp = prefs.getString("calendar_app", null)

        clockAppButton.text = if (clockApp != null) {
            getAppName(clockApp) ?: "Default"
        } else {
            "Default"
        }

        calendarAppButton.text = if (calendarApp != null) {
            getAppName(calendarApp) ?: "Default"
        } else {
            "Default"
        }

        // Load Notion settings
        notionApiKeyInput.setText(prefs.getString("notion_api_key", ""))
        notionDatabaseIdInput.setText(prefs.getString("notion_database_id", ""))
    }

    private fun showColorPicker() {
        val colors = arrayOf("Black", "Dark Blue", "Dark Gray", "Custom")
        val colorValues = intArrayOf(
            Color.BLACK,
            Color.parseColor("#001F3F"),
            Color.parseColor("#1A1A1A"),
            Color.DKGRAY
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.choose_color)
            .setItems(colors) { _, which ->
                val selectedColor = colorValues[which]
                prefs.edit().putInt("background_color", selectedColor).apply()
                updateBackgroundColorButton(selectedColor)
            }
            .show()
    }

    private fun updateBackgroundColorButton(color: Int) {
        backgroundColorButton.setBackgroundColor(color)
        backgroundColorButton.text = String.format("#%06X", 0xFFFFFF and color)
    }

    private fun showAppPicker(isClockApp: Boolean) {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val allApps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val appNames = allApps.map { it.loadLabel(pm).toString() }.toTypedArray()
        val packageNames = allApps.map { it.activityInfo.packageName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.select_app)
            .setItems(appNames) { _, which ->
                val selectedPackage = packageNames[which]
                val prefKey = if (isClockApp) "clock_app" else "calendar_app"
                prefs.edit().putString(prefKey, selectedPackage).apply()

                if (isClockApp) {
                    clockAppButton.text = appNames[which]
                } else {
                    calendarAppButton.text = appNames[which]
                }
            }
            .setNegativeButton("Use Default") { _, _ ->
                val prefKey = if (isClockApp) "clock_app" else "calendar_app"
                prefs.edit().remove(prefKey).apply()
                if (isClockApp) {
                    clockAppButton.text = "Default"
                } else {
                    calendarAppButton.text = "Default"
                }
            }
            .show()
    }

    private fun saveNotionSettings() {
        val apiKey = notionApiKeyInput.text.toString()
        val databaseId = notionDatabaseIdInput.text.toString()

        prefs.edit()
            .putString("notion_api_key", apiKey)
            .putString("notion_database_id", databaseId)
            .apply()

        AlertDialog.Builder(this)
            .setTitle("Saved")
            .setMessage("Notion settings saved successfully")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun getAppName(packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            null
        }
    }
}
