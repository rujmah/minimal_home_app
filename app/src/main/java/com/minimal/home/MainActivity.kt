package com.minimal.home

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var appDrawer: ConstraintLayout
    private lateinit var clockContainer: View
    private lateinit var searchEditText: EditText
    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var calendarEventsRecyclerView: RecyclerView
    private lateinit var settingsIcon: ImageView
    private lateinit var appsAdapter: AppsAdapter
    private lateinit var calendarEventAdapter: CalendarEventAdapter
    private lateinit var gestureDetector: GestureDetector
    private lateinit var prefs: SharedPreferences

    private var isDrawerOpen = false
    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enable edge-to-edge display with system bars visible
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setupSystemBars()

        // Initialize preferences
        prefs = getSharedPreferences("MinimalHomePrefs", MODE_PRIVATE)

        // Initialize views
        rootLayout = findViewById(R.id.rootLayout)
        appDrawer = findViewById(R.id.appDrawer)
        clockContainer = findViewById(R.id.clockContainer)
        searchEditText = findViewById(R.id.searchEditText)
        appsRecyclerView = findViewById(R.id.appsRecyclerView)
        calendarEventsRecyclerView = findViewById(R.id.calendarEventsRecyclerView)
        settingsIcon = findViewById(R.id.settingsIcon)

        // Apply background color from preferences
        val bgColor = prefs.getInt("background_color", Color.BLACK)
        rootLayout.setBackgroundColor(bgColor)
        appDrawer.setBackgroundColor(bgColor)

        // Setup RecyclerView for apps
        appsAdapter = AppsAdapter(emptyList())
        appsRecyclerView.layoutManager = GridLayoutManager(this, 4)
        appsRecyclerView.adapter = appsAdapter

        // Setup RecyclerView for calendar events
        calendarEventAdapter = CalendarEventAdapter(emptyList())
        calendarEventsRecyclerView.layoutManager = LinearLayoutManager(this)
        calendarEventsRecyclerView.adapter = calendarEventAdapter

        // Setup gesture detector for swipe up
        setupGestureDetector()

        // Setup search functionality
        setupSearch()

        // Setup clock click listener
        setupClockClickListener()

        // Setup settings click listener
        settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Setup back button handling
        setupBackPressHandler()

        // Load apps
        loadInstalledApps()

        // Load calendar events
        loadCalendarEvents()
    }

    private fun setupSystemBars() {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        // Show the system bars (status bar and navigation bar)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        // Make the system bars visible and persistent
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                try {
                    val diffY = e2.y - (e1?.y ?: 0f)
                    val diffX = e2.x - (e1?.x ?: 0f)

                    if (abs(diffY) > abs(diffX)) {
                        if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY < 0) {
                                // Swipe up
                                if (!isDrawerOpen) {
                                    openDrawer()
                                }
                            } else {
                                // Swipe down
                                if (isDrawerOpen) {
                                    closeDrawer()
                                }
                            }
                            return true
                        }
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
                return false
            }
        })

        rootLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                appsAdapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isDrawerOpen) {
                openDrawer()
            }
        }
    }

    private fun setupClockClickListener() {
        clockContainer.setOnClickListener {
            openClockApp()
        }
    }

    private fun openClockApp() {
        val selectedClockApp = prefs.getString("clock_app", null)

        try {
            if (selectedClockApp != null) {
                // Try to launch the user-selected clock app
                val pm = packageManager
                val launchIntent = pm.getLaunchIntentForPackage(selectedClockApp)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                    return
                }
            }

            // Fallback: Try to open the clock app using the standard intent
            val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            // If the standard intent doesn't work, try to find and launch the clock app
            try {
                val pm = packageManager
                // Common clock package names
                val clockPackages = listOf(
                    "com.google.android.deskclock",  // Google Clock
                    "com.android.deskclock",         // AOSP Clock
                    "com.sec.android.app.clockpackage", // Samsung Clock
                )

                for (packageName in clockPackages) {
                    try {
                        val launchIntent = pm.getLaunchIntentForPackage(packageName)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                            return
                        }
                    } catch (ex: Exception) {
                        continue
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadCalendarEvents() {
        val apiKey = prefs.getString("notion_api_key", null)
        val databaseId = prefs.getString("notion_database_id", null)

        if (apiKey.isNullOrEmpty() || databaseId.isNullOrEmpty()) {
            // No Notion credentials configured
            calendarEventsRecyclerView.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            val notionClient = NotionApiClient(apiKey, databaseId)
            val events = notionClient.fetchUpcomingEvents()

            if (events.isNotEmpty()) {
                calendarEventsRecyclerView.visibility = View.VISIBLE
                calendarEventAdapter.updateEvents(events)
            } else {
                calendarEventsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val intent = Intent(Intent.ACTION_MAIN, null)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)

                val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

                resolveInfoList.map { resolveInfo ->
                    AppInfo(
                        label = resolveInfo.loadLabel(pm).toString(),
                        packageName = resolveInfo.activityInfo.packageName,
                        icon = resolveInfo.loadIcon(pm)
                    )
                }.sortedBy { it.label.lowercase() }
            }

            appsAdapter.updateApps(apps)
        }
    }

    private fun openDrawer() {
        if (isDrawerOpen) return

        appDrawer.visibility = View.VISIBLE
        appDrawer.translationY = appDrawer.height.toFloat()

        ObjectAnimator.ofFloat(appDrawer, "translationY", 0f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }

        isDrawerOpen = true
    }

    private fun closeDrawer() {
        if (!isDrawerOpen) return

        ObjectAnimator.ofFloat(appDrawer, "translationY", appDrawer.height.toFloat()).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    appDrawer.visibility = View.GONE
                }
            })
            start()
        }

        isDrawerOpen = false
        searchEditText.clearFocus()
        searchEditText.setText("")
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isDrawerOpen) {
                    closeDrawer()
                }
                // Do nothing when drawer is closed - prevent exiting the launcher
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Reload apps when returning to launcher
        loadInstalledApps()

        // Apply background color from preferences (in case it changed in settings)
        val bgColor = prefs.getInt("background_color", Color.BLACK)
        rootLayout.setBackgroundColor(bgColor)
        appDrawer.setBackgroundColor(bgColor)

        // Reload calendar events
        loadCalendarEvents()
    }
}
