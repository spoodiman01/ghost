package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.CalibrationActivity
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.engine.ProfileMerger
import com.example.model.Profile
import com.example.model.ProfileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

class FloatingControlService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PreferencesManager

    private var bubbleView: View? = null
    private var menuOverlayView: View? = null

    private lateinit var bubbleLayoutParams: WindowManager.LayoutParams
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val TAG = "FloatingControlService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "floating_control_channel"

        private val _isFloatingRunning = MutableStateFlow(false)
        val isFloatingRunning: StateFlow<Boolean> = _isFloatingRunning.asStateFlow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = PreferencesManager.getInstance(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        _isFloatingRunning.value = true
        prefs.isFloatingToolEnabled = true

        if (Settings.canDrawOverlays(this)) {
            showFloatingBubble()
        } else {
            Toast.makeText(this, "Overlay permission required for Floating Tool", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingBubble() {
        if (bubbleView != null) return

        val density = resources.displayMetrics.density
        // Minimal compact circular puck (38dp)
        val sizePx = (38 * density).toInt()
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        bubbleLayoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - sizePx - 16
            y = screenHeight / 3
        }

        // Minimalist transparent cyber bubble without icon
        val bubbleContainer = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#3300F2FE"))
                setStroke((1.8f * density).toInt(), Color.parseColor("#9900F2FE"))
            }
            background = bg
            elevation = 10f
            alpha = 0.20f // Transparent at rest
        }

        // Clean minimal centered micro-pip
        val pipView = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00F2FE"))
            }
        }
        val pipSize = (7 * density).toInt()
        val pipParams = FrameLayout.LayoutParams(pipSize, pipSize, Gravity.CENTER)
        bubbleContainer.addView(pipView, pipParams)

        // Drag and touch animation logic: fully visible on touch, transparent when idle
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        bubbleContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Fully visible immediately on touch
                    bubbleContainer.animate()
                        .alpha(1.0f)
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(100)
                        .start()

                    initialX = bubbleLayoutParams.x
                    initialY = bubbleLayoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    bubbleContainer.alpha = 1.0f
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    bubbleLayoutParams.x = initialX + deltaX
                    bubbleLayoutParams.y = initialY + deltaY
                    windowManager.updateViewLayout(bubbleContainer, bubbleLayoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val distanceMoved = hypot(
                        (event.rawX - initialTouchX).toDouble(),
                        (event.rawY - initialTouchY).toDouble()
                    )
                    if (distanceMoved < 15) {
                        // Tapped! Open menu
                        toggleMenu()
                    } else {
                        // Drag released: snap to closest margin & return to transparent
                        val curScreenWidth = resources.displayMetrics.widthPixels
                        val finalX = if (bubbleLayoutParams.x + sizePx / 2 < curScreenWidth / 2) {
                            12
                        } else {
                            curScreenWidth - sizePx - 12
                        }
                        bubbleLayoutParams.x = finalX
                        windowManager.updateViewLayout(bubbleContainer, bubbleLayoutParams)

                        bubbleContainer.animate()
                            .alpha(0.20f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(350)
                            .start()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    bubbleContainer.animate()
                        .alpha(0.20f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(350)
                        .start()
                    true
                }
                else -> false
            }
        }

        bubbleView = bubbleContainer
        try {
            windowManager.addView(bubbleContainer, bubbleLayoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach floating bubble: ${e.message}")
        }
    }

    private fun toggleMenu() {
        if (menuOverlayView != null) {
            dismissMenu()
        } else {
            showActionMenu()
        }
    }

    private fun dismissMenu() {
        menuOverlayView?.let {
            runCatching { windowManager.removeView(it) }
            menuOverlayView = null
        }
        bubbleView?.animate()
            ?.alpha(0.20f)
            ?.scaleX(1.0f)
            ?.scaleY(1.0f)
            ?.setDuration(350)
            ?.start()
    }

    private fun showActionMenu() {
        if (menuOverlayView != null) return

        val density = resources.displayMetrics.density
        val menuWidth = (280 * density).toInt()

        val menuLayoutParams = WindowManager.LayoutParams(
            menuWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#151D2A"))
                cornerRadius = 24 * density
                setStroke(2, Color.parseColor("#00F2FE"))
            }
            background = bg
            setPadding((18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt())
            elevation = 24f
        }

        // Header
        val titleText = TextView(this).apply {
            text = "⚡ Ghost Shield Dock"
            setTextColor(Color.WHITE)
            textSize = 17f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, (12 * density).toInt())
        }
        cardLayout.addView(titleText)

        // Action 1: Quick Calibrate Current
        cardLayout.addView(createMenuItem("🎯 Quick Calibrate Current", Color.parseColor("#00F2FE")) {
            dismissMenu()
            launchCalibration()
        })

        // Action 2: New Profile Here
        cardLayout.addView(createMenuItem("➕ New Profile & Calibrate", Color.parseColor("#38BDF8")) {
            dismissMenu()
            createNewProfileAndCalibrate()
        })

        // Action 3: Merge to Universal
        cardLayout.addView(createMenuItem("🌐 Merge to Universal", Color.parseColor("#818CF8")) {
            dismissMenu()
            mergeToUniversal()
        })

        // Action 4: Toggle Preview Mode
        val previewModeText = if (prefs.isPreviewOverlayVisible) "👁️ Hide Zone Colors" else "🎨 Show Zone Colors"
        cardLayout.addView(createMenuItem(previewModeText, Color.parseColor("#F43F5E")) {
            dismissMenu()
            togglePreviewMode()
        })

        // Action 5: Hide Floating Button
        cardLayout.addView(createMenuItem("✕ Hide Floating Button", Color.parseColor("#94A3B8")) {
            dismissMenu()
            stopSelf()
        })

        menuOverlayView = cardLayout
        try {
            windowManager.addView(cardLayout, menuLayoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show menu overlay: ${e.message}")
        }
    }

    private fun createMenuItem(label: String, accentColor: Int, onClick: () -> Unit): View {
        val density = resources.displayMetrics.density
        val btn = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 14 * density
                setStroke(1, accentColor)
            }
            background = bg
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * density).toInt()
                bottomMargin = (6 * density).toInt()
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val text = TextView(this).apply {
            this.text = label
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
        }
        btn.addView(text)
        return btn
    }

    private fun launchCalibration() {
        val intent = Intent(this, CalibrationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CalibrationActivity.EXTRA_CALIBRATION_DURATION, prefs.calibrationDurationSeconds)
        }
        startActivity(intent)
    }

    private fun createNewProfileAndCalibrate() {
        serviceScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@FloatingControlService)
            val timeString = SimpleDateFormat("MMdd-HHmm", Locale.getDefault()).format(Date())
            val newProfile = Profile(
                name = "Profile #$timeString",
                type = ProfileType.CUSTOM,
                isActive = true
            )
            val id = db.profileDao().insertProfile(newProfile)
            db.profileDao().setActiveProfile(id)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@FloatingControlService, "Created Profile #$timeString", Toast.LENGTH_SHORT).show()
                launchCalibration()
            }
        }
    }

    private fun mergeToUniversal() {
        serviceScope.launch(Dispatchers.IO) {
            val profile = ProfileMerger.createOrUpdateUniversalProfile(this@FloatingControlService)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@FloatingControlService,
                    "✓ Universal Profile Synthesized & Activated!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun togglePreviewMode() {
        val intent = Intent(this, DeadZoneBlockerService::class.java).apply {
            action = DeadZoneBlockerService.ACTION_TOGGLE_PREVIEW
        }
        startService(intent)
        val state = if (!prefs.isPreviewOverlayVisible) "Visible" else "Stealth"
        Toast.makeText(this, "Zone Coverage: $state", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ghost Shield Floating Dock",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ghost Shield Floating Tool")
            .setContentText("Tap floating bubble on screen to calibrate or switch profiles")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissMenu()
        bubbleView?.let {
            runCatching { windowManager.removeView(it) }
            bubbleView = null
        }
        _isFloatingRunning.value = false
        prefs.isFloatingToolEnabled = false
    }
}
