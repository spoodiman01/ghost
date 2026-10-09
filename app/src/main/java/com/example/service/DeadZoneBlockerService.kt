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
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.model.DeadZone
import com.example.model.GhostTouchLog
import com.example.model.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ActiveZoneInfo(
    val zone: DeadZone,
    val profileName: String,
    val profileId: Long
)

class DeadZoneBlockerService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PreferencesManager
    private lateinit var database: AppDatabase

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var dbObserverJob: Job? = null

    // Track active injected overlay views
    private val activeZoneViews = mutableListOf<View>()
    private var currentActiveProfiles = listOf<Profile>()
    private var currentActiveZones = listOf<ActiveZoneInfo>()

    // Rate limiting for ghost touch diagnostic logs
    private var lastLoggedTime = 0L

    companion object {
        private const val TAG = "DeadZoneBlockerService"
        const val CHANNEL_ID = "ghost_touch_blocker_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START_SHIELD"
        const val ACTION_STOP = "com.example.action.STOP_SHIELD"
        const val ACTION_TOGGLE = "com.example.action.TOGGLE_SHIELD"
        const val ACTION_RELOAD_ZONES = "com.example.action.RELOAD_ZONES"
        const val ACTION_TOGGLE_PREVIEW = "com.example.action.TOGGLE_PREVIEW"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isPreviewVisible = MutableStateFlow(false)
        val isPreviewVisible: StateFlow<Boolean> = _isPreviewVisible.asStateFlow()

        private val _blockedTouchCount = MutableStateFlow(0)
        val blockedTouchCount: StateFlow<Int> = _blockedTouchCount.asStateFlow()

        private val _activeZoneCount = MutableStateFlow(0)
        val activeZoneCount: StateFlow<Int> = _activeZoneCount.asStateFlow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = PreferencesManager.getInstance(this)
        database = AppDatabase.getInstance(this)
        _isPreviewVisible.value = prefs.isPreviewOverlayVisible

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Initializing Ghost Shield..."))
        _isRunning.value = true
        prefs.isShieldEnabled = true

        startObservingDatabase()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                removeAllZoneViews()
                _isRunning.value = false
                prefs.isShieldEnabled = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                _isRunning.value = true
                prefs.isShieldEnabled = true
                loadAndApplyZones()
            }
            ACTION_TOGGLE_PREVIEW -> {
                val newPreview = !prefs.isPreviewOverlayVisible
                prefs.isPreviewOverlayVisible = newPreview
                _isPreviewVisible.value = newPreview
                refreshOverlayAppearance()
                updateNotification()
            }
            ACTION_RELOAD_ZONES -> {
                loadAndApplyZones()
            }
            else -> {
                _isRunning.value = true
                prefs.isShieldEnabled = true
                loadAndApplyZones()
            }
        }
        return START_STICKY
    }

    /**
     * Observes all active profiles in the database.
     * When any profile is activated/deactivated or zones are updated,
     * live overlays are immediately synchronized.
     */
    private fun startObservingDatabase() {
        dbObserverJob?.cancel()
        dbObserverJob = serviceScope.launch {
            database.profileDao().getActiveProfiles().collect { activeProfiles ->
                currentActiveProfiles = activeProfiles
                if (activeProfiles.isNotEmpty()) {
                    val profileIds = activeProfiles.map { it.id }
                    database.deadZoneDao().getDeadZonesForProfiles(profileIds).collect { zones ->
                        val activeInfoList = zones.filter { it.isEnabled && it.isValid() }.map { zone ->
                            val ownerProfile = activeProfiles.find { it.id == zone.profileId }
                            ActiveZoneInfo(
                                zone = zone,
                                profileName = ownerProfile?.name ?: "Unknown Profile",
                                profileId = zone.profileId
                            )
                        }
                        currentActiveZones = activeInfoList
                        _activeZoneCount.value = activeInfoList.size
                        withContext(Dispatchers.Main) {
                            if (prefs.isShieldEnabled) {
                                applyDeadZones(activeInfoList)
                            } else {
                                removeAllZoneViews()
                            }
                            updateNotification()
                        }
                    }
                } else {
                    currentActiveZones = emptyList()
                    _activeZoneCount.value = 0
                    withContext(Dispatchers.Main) {
                        removeAllZoneViews()
                        updateNotification()
                    }
                }
            }
        }
    }

    private fun loadAndApplyZones() {
        serviceScope.launch(Dispatchers.IO) {
            val activeProfiles = database.profileDao().getActiveProfilesSync()
            currentActiveProfiles = activeProfiles
            if (activeProfiles.isNotEmpty()) {
                val profileIds = activeProfiles.map { it.id }
                val zones = database.deadZoneDao().getDeadZonesForProfilesSync(profileIds)
                val activeInfoList = zones.filter { it.isEnabled && it.isValid() }.map { zone ->
                    val ownerProfile = activeProfiles.find { it.id == zone.profileId }
                    ActiveZoneInfo(
                        zone = zone,
                        profileName = ownerProfile?.name ?: "Unknown Profile",
                        profileId = zone.profileId
                    )
                }
                currentActiveZones = activeInfoList
                _activeZoneCount.value = activeInfoList.size
                withContext(Dispatchers.Main) {
                    if (prefs.isShieldEnabled) {
                        applyDeadZones(activeInfoList)
                    } else {
                        removeAllZoneViews()
                    }
                    updateNotification()
                }
            } else {
                currentActiveZones = emptyList()
                _activeZoneCount.value = 0
                withContext(Dispatchers.Main) {
                    removeAllZoneViews()
                    updateNotification()
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun applyDeadZones(zoneInfos: List<ActiveZoneInfo>) {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot draw overlays: Permission missing!")
            return
        }

        removeAllZoneViews()

        val isPreview = _isPreviewVisible.value

        for (info in zoneInfos) {
            val zone = info.zone
            val width = zone.width
            val height = zone.height
            if (width <= 0 || height <= 0) continue

            val layoutParams = WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // CRITICAL: FLAG_NOT_FOCUSABLE without FLAG_NOT_TOUCH_MODAL intercepts touches inside,
                // while FLAG_LAYOUT_IN_SCREEN and FLAG_LAYOUT_NO_LIMITS ensure 1:1 pixel accuracy
                // across status bars and navigation cutouts matching rawX and rawY.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = zone.left
                y = zone.top
            }

            val zoneContainer = FrameLayout(this)
            updateViewStyle(zoneContainer, isPreview, info)

            // Touch interceptor: consume touch events and log diagnostics
            zoneContainer.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        _blockedTouchCount.value += 1
                        logBlockedTouch(
                            absX = zone.left + event.x,
                            absY = zone.top + event.y,
                            profileName = info.profileName
                        )
                    }
                }
                // Return true to swallow the ghost touch completely
                true
            }

            try {
                windowManager.addView(zoneContainer, layoutParams)
                activeZoneViews.add(zoneContainer)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add dead zone view to window manager: ${e.message}")
            }
        }
    }

    private fun updateViewStyle(view: FrameLayout, isPreview: Boolean, info: ActiveZoneInfo) {
        view.removeAllViews()
        val zone = info.zone
        if (isPreview) {
            // Distinct tint based on profile ID hash for multi-profile visual separation
            val hue = ((info.profileId * 67) % 360).toFloat()
            val strokeColor = Color.HSVToColor(floatArrayOf(hue, 0.85f, 1.0f))
            val fillColor = Color.HSVToColor(90, floatArrayOf(hue, 0.75f, 0.95f))

            val background = GradientDrawable().apply {
                setColor(fillColor)
                setStroke(3, strokeColor)
                cornerRadius = 8f
            }
            view.background = background

            // Show informative indicator tag
            if (zone.width >= 50 && zone.height >= 26) {
                val labelView = TextView(this).apply {
                    text = "${info.profileName}\n[#${zone.id}]"
                    textSize = 8.5f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.argb(175, 0, 0, 0))
                    setPadding(6, 2, 6, 2)
                    gravity = Gravity.CENTER
                }
                val labelParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                view.addView(labelView, labelParams)
            }
        } else {
            // Invisible mode: minimal transparent alpha to consume touches cleanly
            view.setBackgroundColor(Color.argb(1, 0, 0, 0))
        }
    }

    private fun refreshOverlayAppearance() {
        val isPreview = _isPreviewVisible.value
        activeZoneViews.forEachIndexed { index, view ->
            if (view is FrameLayout && index < currentActiveZones.size) {
                updateViewStyle(view, isPreview, currentActiveZones[index])
            }
        }
    }

    private fun removeAllZoneViews() {
        for (view in activeZoneViews) {
            runCatching {
                windowManager.removeView(view)
            }
        }
        activeZoneViews.clear()
    }

    private fun logBlockedTouch(absX: Float, absY: Float, profileName: String) {
        val now = System.currentTimeMillis()
        if (now - lastLoggedTime > 1000) {
            lastLoggedTime = now
            serviceScope.launch(Dispatchers.IO) {
                database.ghostTouchLogDao().insertLog(
                    GhostTouchLog(
                        timestamp = now,
                        x = absX,
                        y = absY,
                        profileName = profileName,
                        source = "BLOCKED"
                    )
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ghost Touch Blocker Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active dead-zone touch shielding service"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePreviewIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DeadZoneBlockerService::class.java).apply {
                action = ACTION_TOGGLE_PREVIEW
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, DeadZoneBlockerService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val previewLabel = if (_isPreviewVisible.value) "Hide Highlights" else "Show Highlights"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GhostTouchShield Pro Active")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, previewLabel, togglePreviewIntent)
            .addAction(0, "Deactivate", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val activeCount = currentActiveProfiles.size
        val profileSummary = if (activeCount == 1) {
            currentActiveProfiles.first().name
        } else if (activeCount > 1) {
            "$activeCount Active Profiles"
        } else {
            "No Active Profiles"
        }

        val count = currentActiveZones.size
        val preview = if (_isPreviewVisible.value) "(Visible Mode)" else "(Stealth Mode)"
        val text = "$profileSummary • $count Filters Blocking $preview"

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        dbObserverJob?.cancel()
        removeAllZoneViews()
        _isRunning.value = false
        prefs.isShieldEnabled = false
    }
}
