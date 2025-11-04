package com.minimal.home

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var appDrawer: ConstraintLayout
    private lateinit var searchEditText: EditText
    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var appsAdapter: AppsAdapter
    private lateinit var gestureDetector: GestureDetector

    private var isDrawerOpen = false
    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupSystemBars()

        // Initialize views
        rootLayout = findViewById(R.id.rootLayout)
        appDrawer = findViewById(R.id.appDrawer)
        searchEditText = findViewById(R.id.searchEditText)
        appsRecyclerView = findViewById(R.id.appsRecyclerView)

        // Setup RecyclerView
        appsAdapter = AppsAdapter(emptyList())
        appsRecyclerView.layoutManager = GridLayoutManager(this, 4)
        appsRecyclerView.adapter = appsAdapter

        // Setup gesture detector for swipe up
        setupGestureDetector()

        // Setup search functionality
        setupSearch()

        // Load apps
        loadInstalledApps()
    }

    private fun setupSystemBars() {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
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

    override fun onBackPressed() {
        if (isDrawerOpen) {
            closeDrawer()
        } else {
            // Do nothing - prevent exiting the launcher
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload apps when returning to launcher
        loadInstalledApps()
    }
}
