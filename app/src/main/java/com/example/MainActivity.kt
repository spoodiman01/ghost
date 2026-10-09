package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import com.example.util.ApkExportHelper
import com.example.util.ApkInfo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.model.DeadZonePreset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.engine.ProfileMerger
import com.example.model.DeadZone
import com.example.model.GhostTouchLog
import com.example.model.Profile
import com.example.model.ProfileType
import com.example.service.DeadZoneBlockerService
import com.example.service.FloatingControlService
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.DangerNeon
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ShieldAccentPurple
import com.example.ui.theme.ShieldCyan
import com.example.ui.theme.ShieldElectricBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainDashboardScreen(
                    onLaunchCalibration = { duration ->
                        val intent = Intent(this, CalibrationActivity::class.java).apply {
                            putExtra(CalibrationActivity.EXTRA_CALIBRATION_DURATION, duration)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

// Data holder for simulation visualization
data class SimulatedZone(
    val zone: DeadZone,
    val profileName: String,
    val profileColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(onLaunchCalibration: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val prefs = remember { PreferencesManager.getInstance(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Database state flows
    val profiles by db.profileDao().getAllProfiles().collectAsState(initial = emptyList())
    val activeProfiles by db.profileDao().getActiveProfiles().collectAsState(initial = emptyList())
    val allZones by db.deadZoneDao().getAllDeadZones().collectAsState(initial = emptyList())
    val recentLogs by db.ghostTouchLogDao().getRecentLogs(40).collectAsState(initial = emptyList())
    val totalLogsCount by db.ghostTouchLogDao().getTotalLogCount().collectAsState(initial = 0)

    // Calibration custom duration & presets dialog state
    var calibrationDuration by remember { mutableIntStateOf(prefs.calibrationDurationSeconds) }
    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showApkDialog by remember { mutableStateOf(false) }

    // Auto-augment sparse databases (if only 2 dead zones exist)
    LaunchedEffect(allZones) {
        if (allZones.isNotEmpty() && allZones.size <= 2) {
            withContext(Dispatchers.IO) {
                AppDatabase.populateInitialData(db)
            }
        }
    }

    // Filtered zones for selected profile viewing
    var viewingProfileId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(profiles) {
        if (viewingProfileId == null && profiles.isNotEmpty()) {
            viewingProfileId = activeProfiles.firstOrNull()?.id ?: profiles.first().id
        }
    }

    val viewingProfile = profiles.find { it.id == viewingProfileId }
    val viewingProfileZones by remember(viewingProfileId) {
        viewingProfileId?.let { db.deadZoneDao().getDeadZonesForProfile(it) } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    // Active profiles' zones for simulation & blocking
    val activeProfileIds = activeProfiles.map { it.id }
    val activeDeadZones by remember(activeProfileIds) {
        if (activeProfileIds.isNotEmpty()) {
            db.deadZoneDao().getDeadZonesForProfiles(activeProfileIds)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // Service States
    val isShieldRunning by DeadZoneBlockerService.isRunning.collectAsState()
    val isPreviewMode by DeadZoneBlockerService.isPreviewVisible.collectAsState()
    val isFloatingRunning by FloatingControlService.isFloatingRunning.collectAsState()
    val blockedTouchLiveCount by DeadZoneBlockerService.blockedTouchCount.collectAsState()
    val activeFilterCount by DeadZoneBlockerService.activeZoneCount.collectAsState()

    // Permission tracking states with lifecycle observation
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isIgnoringBattery by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else true
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // Lifecycle observer to immediately detect when user grants permissions and returns
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    isIgnoringBattery = pm.isIgnoringBatteryOptimizations(context.packageName)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission Request Launchers
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Selected Dashboard Tab (0: Controls, 1: Simulation, 2: Profiles & Zones, 3: Diagnostics)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog & Configurator States
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var showAddZoneDialog by remember { mutableStateOf(false) }
    var configuringZone by remember { mutableStateOf<DeadZone?>(null) }

    Scaffold(
        containerColor = CyberDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                                .border(1.5.dp, ShieldCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ShieldCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "GhostTouchShield Pro",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isShieldRunning) SuccessEmerald else WarningAmber, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isShieldRunning) {
                                        "BLOCKER ON • ${activeProfiles.size} PROFILES ACTIVE"
                                    } else {
                                        "BLOCKER OFF (STANDBY)"
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isShieldRunning) SuccessEmerald else WarningAmber,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDark),
                actions = {
                    IconButton(
                        onClick = { showApkDialog = true },
                        modifier = Modifier.testTag("app_apk_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = "APK Details & Export",
                            tint = Color(0xFF00E676)
                        )
                    }
                    IconButton(
                        onClick = {
                            hasOverlayPermission = Settings.canDrawOverlays(context)
                            Toast.makeText(context, if (hasOverlayPermission) "✓ Overlay permission confirmed active" else "Overlay permission not granted yet", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("refresh_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Status",
                            tint = ShieldCyan
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Navigation Tabs (Controls, Simulation, Profiles, Diagnostics)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CyberSurface,
                contentColor = ShieldCyan,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ShieldCyan,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Controls", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1, softWrap = false) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(17.dp)) },
                    modifier = Modifier.testTag("tab_controls")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Simulation", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1, softWrap = false) },
                    icon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(17.dp)) },
                    modifier = Modifier.testTag("tab_simulation")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Profiles", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(containerColor = Color(0xFF334155)) {
                                Text("${activeProfiles.size}/${profiles.size}", color = ShieldCyan, fontSize = 9.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(17.dp)) },
                    modifier = Modifier.testTag("tab_profiles")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Logs", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                            if (totalLogsCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(containerColor = DangerNeon) {
                                    Text("$totalLogsCount", color = Color.White, fontSize = 9.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(17.dp)) },
                    modifier = Modifier.testTag("tab_diagnostics")
                )
            }

            // Tab Content Router
            when (selectedTab) {
                0 -> ControlsTabContent(
                    hasOverlayPermission = hasOverlayPermission,
                    isIgnoringBattery = isIgnoringBattery,
                    hasNotificationPermission = hasNotificationPermission,
                    isShieldRunning = isShieldRunning,
                    isPreviewMode = isPreviewMode,
                    isFloatingRunning = isFloatingRunning,
                    activeProfilesCount = activeProfiles.size,
                    activeDeadZonesCount = activeDeadZones.count { it.isEnabled },
                    allZonesCount = allZones.size,
                    blockedTouchLiveCount = blockedTouchLiveCount,
                    onRequestOverlayPermission = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onRequestBatteryOptimization = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onToggleShield = { enabled ->
                        if (enabled) {
                            if (!Settings.canDrawOverlays(context)) {
                                Toast.makeText(context, "Grant 'Display over other apps' first!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                                return@ControlsTabContent
                            }
                            val intent = Intent(context, DeadZoneBlockerService::class.java).apply {
                                action = DeadZoneBlockerService.ACTION_START
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            prefs.isShieldEnabled = true
                        } else {
                            val intent = Intent(context, DeadZoneBlockerService::class.java).apply {
                                action = DeadZoneBlockerService.ACTION_STOP
                            }
                            context.startService(intent)
                            prefs.isShieldEnabled = false
                        }
                    },
                    onTogglePreview = {
                        val intent = Intent(context, DeadZoneBlockerService::class.java).apply {
                            action = DeadZoneBlockerService.ACTION_TOGGLE_PREVIEW
                        }
                        context.startService(intent)
                    },
                    onToggleFloating = { enabled ->
                        if (!Settings.canDrawOverlays(context)) {
                            Toast.makeText(context, "Grant 'Display over other apps' first!", Toast.LENGTH_SHORT).show()
                            return@ControlsTabContent
                        }
                        if (enabled) {
                            val intent = Intent(context, FloatingControlService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            prefs.isFloatingToolEnabled = true
                        } else {
                            context.stopService(Intent(context, FloatingControlService::class.java))
                            prefs.isFloatingToolEnabled = false
                        }
                    },
                    calibrationDuration = calibrationDuration,
                    onSelectCalibrationDuration = { dur ->
                        calibrationDuration = dur
                        prefs.calibrationDurationSeconds = dur
                    },
                    onCustomDurationClick = { showCustomDurationDialog = true },
                    onBuildUniversal = {
                        scope.launch(Dispatchers.IO) {
                            ProfileMerger.createOrUpdateUniversalProfile(context)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Universal Profile built and activated!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onAddPresetsToUniversal = {
                        scope.launch(Dispatchers.IO) {
                            val prof = ProfileMerger.createOrUpdateUniversalProfile(context)
                            ProfileMerger.addPresetsToProfile(context, prof.id, DeadZonePreset.ALL_PRESETS.take(6))
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "✓ Added recommended hardware presets to Universal Profile!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onOpenApkDialog = { showApkDialog = true },
                    onLaunchCalibration = { onLaunchCalibration(calibrationDuration) }
                )

                1 -> SimulationScreen(
                    profiles = profiles,
                    activeProfiles = activeProfiles,
                    activeZones = activeDeadZones.filter { it.isEnabled },
                    onNavigateToProfiles = { selectedTab = 2 }
                )

                2 -> MultiProfileZonesTabContent(
                    profiles = profiles,
                    activeProfiles = activeProfiles,
                    viewingProfile = viewingProfile,
                    viewingProfileZones = viewingProfileZones,
                    onSelectViewingProfile = { viewingProfileId = it.id },
                    onToggleProfileActive = { prof, isActive ->
                        scope.launch(Dispatchers.IO) {
                            db.profileDao().setProfileActive(prof.id, isActive)
                            ProfileMerger.notifyBlockerService(context)
                        }
                    },
                    onToggleZoneEnabled = { zone, isEnabled ->
                        scope.launch(Dispatchers.IO) {
                            db.deadZoneDao().setZoneEnabled(zone.id, isEnabled)
                            ProfileMerger.notifyBlockerService(context)
                        }
                    },
                    onCreateProfileClick = { showCreateProfileDialog = true },
                    onDeleteProfileClick = { prof ->
                        scope.launch(Dispatchers.IO) {
                            db.profileDao().deleteProfile(prof)
                            ProfileMerger.notifyBlockerService(context)
                        }
                    },
                    onAddZoneClick = { showAddZoneDialog = true },
                    onAddPresetsClick = { showPresetsDialog = true },
                    onConfigureZoneClick = { zone -> configuringZone = zone },
                    onDeleteZoneClick = { zone ->
                        scope.launch(Dispatchers.IO) {
                            db.deadZoneDao().deleteDeadZone(zone)
                            ProfileMerger.notifyBlockerService(context)
                        }
                    },
                    onLaunchCalibration = { onLaunchCalibration(calibrationDuration) }
                )

                3 -> DiagnosticsTabContent(
                    recentLogs = recentLogs,
                    totalLogsCount = totalLogsCount,
                    blockedTouchLiveCount = blockedTouchLiveCount,
                    onClearLogs = {
                        scope.launch(Dispatchers.IO) {
                            db.ghostTouchLogDao().clearLogs()
                        }
                    }
                )
            }
        }
    }

    // Modal: Interactive Zone Area Configurator (with sliding area and live preview)
    configuringZone?.let { zone ->
        ZoneConfiguratorDialog(
            zone = zone,
            onDismiss = { configuringZone = null },
            onSave = { updatedZone ->
                scope.launch(Dispatchers.IO) {
                    db.deadZoneDao().updateDeadZone(updatedZone)
                    ProfileMerger.notifyBlockerService(context)
                }
                configuringZone = null
                Toast.makeText(context, "Zone bounds updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Create Profile
    if (showCreateProfileDialog) {
        var newProfileName by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(ProfileType.CUSTOM) }

        AlertDialog(
            onDismissRequest = { showCreateProfileDialog = false },
            containerColor = CyberSurfaceCard,
            titleContentColor = TextPrimary,
            title = { Text("Create New Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name") },
                        placeholder = { Text("e.g. Gaming Mode, Bedside") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ShieldCyan,
                            unfocusedBorderColor = CyberSurfaceBorder,
                            focusedLabelColor = ShieldCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_profile_name")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Target Scenario:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    ProfileType.values().filter { it != ProfileType.UNIVERSAL }.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = type }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        if (selectedType == type) ShieldCyan else Color.Transparent,
                                        CircleShape
                                    )
                                    .border(1.5.dp, ShieldCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(type.displayName, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                val profile = Profile(
                                    name = newProfileName.trim(),
                                    type = selectedType,
                                    isActive = true
                                )
                                val id = db.profileDao().insertProfile(profile)
                                viewingProfileId = id
                                ProfileMerger.notifyBlockerService(context)
                            }
                            showCreateProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldCyan, contentColor = CyberDark),
                    modifier = Modifier.testTag("confirm_create_profile_btn")
                ) {
                    Text("Create Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Add Manual Dead Zone
    // Dialog: Add Manual Dead Zone with Quick Preset Autofill
    if (showAddZoneDialog && viewingProfile != null) {
        var leftStr by remember { mutableStateOf("20") }
        var topStr by remember { mutableStateOf("100") }
        var widthStr by remember { mutableStateOf("250") }
        var heightStr by remember { mutableStateOf("250") }
        var labelStr by remember { mutableStateOf("Screen Edge Zone") }

        AlertDialog(
            onDismissRequest = { showAddZoneDialog = false },
            containerColor = CyberSurfaceCard,
            titleContentColor = TextPrimary,
            title = { Text("Add Manual Dead Zone", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Target profile: ${viewingProfile?.name}",
                        fontSize = 12.sp,
                        color = ShieldCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Quick Preset Autofill:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(DeadZonePreset.ALL_PRESETS.take(6)) { p ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                    .clickable {
                                        labelStr = p.name
                                        leftStr = p.left.toString()
                                        topStr = p.top.toString()
                                        widthStr = p.width.toString()
                                        heightStr = p.height.toString()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(p.name.take(14), fontSize = 10.sp, color = ShieldCyan)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = labelStr,
                        onValueChange = { labelStr = it },
                        label = { Text("Filter Label") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ShieldCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = leftStr,
                            onValueChange = { leftStr = it },
                            label = { Text("X (Left)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = topStr,
                            onValueChange = { topStr = it },
                            label = { Text("Y (Top)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = widthStr,
                            onValueChange = { widthStr = it },
                            label = { Text("Width (px)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = heightStr,
                            onValueChange = { heightStr = it },
                            label = { Text("Height (px)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val left = leftStr.toIntOrNull() ?: 0
                        val top = topStr.toIntOrNull() ?: 0
                        val w = widthStr.toIntOrNull() ?: 100
                        val h = heightStr.toIntOrNull() ?: 100

                        scope.launch(Dispatchers.IO) {
                            viewingProfile?.let { prof ->
                                db.deadZoneDao().insertDeadZone(
                                    DeadZone(
                                        profileId = prof.id,
                                        left = left,
                                        top = top,
                                        right = left + w,
                                        bottom = top + h,
                                        label = labelStr.ifBlank { "Manual Zone" },
                                        isEnabled = true
                                    )
                                )
                                ProfileMerger.notifyBlockerService(context)
                            }
                        }
                        showAddZoneDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldCyan, contentColor = CyberDark)
                ) {
                    Text("Add Filter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddZoneDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Custom Calibration Duration
    if (showCustomDurationDialog) {
        var sliderVal by remember { mutableFloatStateOf(calibrationDuration.toFloat()) }
        AlertDialog(
            onDismissRequest = { showCustomDurationDialog = false },
            containerColor = CyberSurfaceCard,
            title = { Text("Custom Calibration Scan Window", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "${sliderVal.toInt()} seconds scan time",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShieldCyan
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        valueRange = 5f..120f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = ShieldCyan,
                            activeTrackColor = ShieldCyan,
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allows enough time for intermittent hardware ghost touches to be registered and mapped accurately.",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dur = sliderVal.toInt()
                        calibrationDuration = dur
                        prefs.calibrationDurationSeconds = dur
                        showCustomDurationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldCyan, contentColor = CyberDark)
                ) {
                    Text("Set Duration")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDurationDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Hardware Fault Problem Presets Picker
    if (showPresetsDialog && viewingProfile != null) {
        Dialog(onDismissRequest = { showPresetsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ghost Touch Presets",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Target: ${viewingProfile?.name}",
                                fontSize = 11.5.sp,
                                color = ShieldCyan
                            )
                        }
                        IconButton(onClick = { showPresetsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(DeadZonePreset.ALL_PRESETS) { preset ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF22354E))
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = preset.description,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${preset.category} • ${preset.width}x${preset.height} px",
                                            fontSize = 10.sp,
                                            color = ShieldCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                viewingProfile?.let { prof ->
                                                    ProfileMerger.addPresetsToProfile(context, prof.id, listOf(preset))
                                                }
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Added '${preset.name}'", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ShieldCyan, contentColor = CyberDark),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                viewingProfile?.let { prof ->
                                    ProfileMerger.addPresetsToProfile(context, prof.id, DeadZonePreset.ALL_PRESETS.take(6))
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Added 6 recommended presets!", Toast.LENGTH_SHORT).show()
                                    showPresetsDialog = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ShieldAccentPurple, contentColor = CyberDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add All Recommended Presets (+6)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dialog: APK Package Details & Export
    if (showApkDialog) {
        val apkInfo = remember { ApkExportHelper.getApkInfo(context) }
        AlertDialog(
            onDismissRequest = { showApkDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF132E1B), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Application Package (APK)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("Ready to sideload & install on device", fontSize = 11.5.sp, color = Color(0xFF00E676))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101927)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("APK File:", fontSize = 11.5.sp, color = TextMuted)
                                Text(apkInfo.fileName, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Size:", fontSize = 11.5.sp, color = TextMuted)
                                Text(apkInfo.installedApkSize, fontSize = 12.sp, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Version:", fontSize = 11.5.sp, color = TextMuted)
                                Text("${apkInfo.versionName} (Build ${apkInfo.versionCode})", fontSize = 12.sp, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Target SDK:", fontSize = 11.5.sp, color = TextMuted)
                                Text("API ${apkInfo.targetSdk} (Min API ${apkInfo.minSdk})", fontSize = 12.sp, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Package:", fontSize = 11.5.sp, color = TextMuted)
                                Text(apkInfo.packageName, fontSize = 10.5.sp, color = ShieldCyan)
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Workspace Artifact Path:", fontSize = 11.sp, color = ShieldCyan, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = apkInfo.projectArtifactPath,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0B132B), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Gradle Output Path:", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = apkInfo.gradleOutputPath,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0B132B), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                ApkExportHelper.shareApk(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = CyberDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export / Share APK to Device", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            ApkExportHelper.copyPathToClipboard(context, apkInfo.projectArtifactPath)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy .build-outputs/app-debug.apk Path")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showApkDialog = false }) {
                    Text("Close", color = ShieldCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CyberSurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: CONTROLS
// -------------------------------------------------------------------------------------------------

@Composable
fun ControlsTabContent(
    hasOverlayPermission: Boolean,
    isIgnoringBattery: Boolean,
    hasNotificationPermission: Boolean,
    isShieldRunning: Boolean,
    isPreviewMode: Boolean,
    isFloatingRunning: Boolean,
    activeProfilesCount: Int,
    activeDeadZonesCount: Int,
    allZonesCount: Int,
    blockedTouchLiveCount: Int,
    calibrationDuration: Int,
    onSelectCalibrationDuration: (Int) -> Unit,
    onCustomDurationClick: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onToggleShield: (Boolean) -> Unit,
    onTogglePreview: () -> Unit,
    onToggleFloating: (Boolean) -> Unit,
    onBuildUniversal: () -> Unit,
    onAddPresetsToUniversal: () -> Unit,
    onOpenApkDialog: () -> Unit = {},
    onLaunchCalibration: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Permissions banner if any missing
        if (!hasOverlayPermission || !isIgnoringBattery || !hasNotificationPermission) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF26191B)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DangerNeon)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DangerNeon)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "System Permissions Required",
                                fontWeight = FontWeight.Bold,
                                color = DangerNeon,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        if (!hasOverlayPermission) {
                            PermissionRow(
                                title = "1. Display Over Other Apps",
                                desc = "Required to intercept and drop ghost touches outside app",
                                isGranted = false,
                                onGrant = onRequestOverlayPermission,
                                testTag = "grant_overlay_perm_btn"
                            )
                        }
                        if (!hasNotificationPermission) {
                            PermissionRow(
                                title = "2. Notification Permission",
                                desc = "Required to keep background touch shield alive",
                                isGranted = false,
                                onGrant = onRequestNotificationPermission,
                                testTag = "grant_notification_perm_btn"
                            )
                        }
                        if (!isIgnoringBattery) {
                            PermissionRow(
                                title = "3. Disable Battery Optimizations",
                                desc = "Prevents OS from killing shield during screen off/sleep",
                                isGranted = false,
                                onGrant = onRequestBatteryOptimization,
                                testTag = "grant_battery_perm_btn"
                            )
                        }
                    }
                }
            }
        }

        // Master Shield Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isShieldRunning) ShieldCyan else CyberSurfaceBorder)
                ),
                modifier = Modifier.testTag("master_shield_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Master Blocker Shield",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isShieldRunning) "Currently actively blocking ghost touches" else "Disabled - all touches pass through",
                                fontSize = 12.sp,
                                color = if (isShieldRunning) SuccessEmerald else TextSecondary
                            )
                        }
                        Switch(
                            checked = isShieldRunning,
                            onCheckedChange = onToggleShield,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberDark,
                                checkedTrackColor = ShieldCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.testTag("master_shield_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = CyberSurfaceBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Multi-Profile Active Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active Profiles", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = "$activeProfilesCount Running Together",
                                fontWeight = FontWeight.SemiBold,
                                color = ShieldCyan,
                                fontSize = 14.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Active Filter Zones", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = "$activeDeadZonesCount Zones Active",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Calibration Hero Action with Custom Duration Controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ShieldElectricBlue)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ghost Touch Calibration Scanner",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Scans screen jitter and consolidates multi-touch points into dead zones.",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onLaunchCalibration,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShieldCyan,
                                contentColor = CyberDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("start_calibration_btn")
                        ) {
                            Text("Scan ${calibrationDuration}s", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scan Window Duration:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ShieldCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(10, 15, 30, 60).forEach { dur ->
                            val isSelected = calibrationDuration == dur
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ShieldCyan else Color(0xFF1E293B))
                                    .border(1.dp, if (isSelected) ShieldCyan else Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .clickable { onSelectCalibrationDuration(dur) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${dur}s",
                                    color = if (isSelected) CyberDark else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                        val isCustom = calibrationDuration !in listOf(10, 15, 30, 60)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCustom) ShieldAccentPurple else Color(0xFF1E293B))
                                .border(1.dp, if (isCustom) ShieldAccentPurple else Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable { onCustomDurationClick() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isCustom) "${calibrationDuration}s (Custom)" else "Custom...",
                                color = if (isCustom) CyberDark else ShieldAccentPurple,
                                fontSize = 12.sp,
                                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Secondary Toggles: Floating Tool & Zone Visibility
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Visual Inspection & Overlay Tools",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Floating Bubble Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Widgets, contentDescription = null, tint = ShieldCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Floating Bubble Tool", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                                Text("Draggable bubble dock for on-screen calibration", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Switch(
                            checked = isFloatingRunning,
                            onCheckedChange = onToggleFloating,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberDark,
                                checkedTrackColor = ShieldCyan
                            ),
                            modifier = Modifier.testTag("floating_bubble_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = CyberSurfaceBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Zone Visual Preview Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPreviewMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = DangerNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Highlight Shield Zones on Screen", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                                Text("Render color-coded translucent boxes to see blocked areas", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Switch(
                            checked = isPreviewMode,
                            onCheckedChange = { onTogglePreview() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberDark,
                                checkedTrackColor = DangerNeon
                            ),
                            modifier = Modifier.testTag("preview_overlay_switch")
                        )
                    }
                }
            }
        }

        // Synthesize Universal Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ShieldAccentPurple)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MergeType, contentDescription = null, tint = ShieldAccentPurple)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Universal Master Shield",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                        }
                        Badge(containerColor = ShieldAccentPurple) {
                            Text("$allZonesCount zones active", color = CyberDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Synthesizes proximate & overlapping dead zones across all profiles into a master shield, enriched with comprehensive hardware failure presets.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onBuildUniversal,
                            modifier = Modifier.weight(1f).testTag("build_universal_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShieldAccentPurple,
                                contentColor = CyberDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Synthesize", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onAddPresetsToUniversal,
                            modifier = Modifier.weight(1f).testTag("add_presets_universal_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldCyan)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Presets (+6)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Application Package & APK Build Artifact Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00E676))
                ),
                modifier = Modifier.testTag("apk_info_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF132E1B), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Android APK Package",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Ready to sideload & install on device",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                        Badge(containerColor = Color(0xFF00E676)) {
                            Text("BUILD READY", color = CyberDark, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // APK Details Specs Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF101927), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("APK File:", fontSize = 11.sp, color = TextMuted)
                            Text("app-debug.apk (~22.7 MB)", fontSize = 11.5.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Build Artifact:", fontSize = 11.sp, color = TextMuted)
                            Text(".build-outputs/app-debug.apk", fontSize = 11.5.sp, color = ShieldCyan, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Package ID:", fontSize = 11.sp, color = TextMuted)
                            Text("com.aistudio.ghosttouchshield.pxlmrt", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Version / Target:", fontSize = 11.sp, color = TextMuted)
                            Text("1.0 (Build 1) • Android 14 (API 34)", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    ApkExportHelper.shareApk(context)
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("share_apk_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = CyberDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export / Share APK", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                ApkExportHelper.copyPathToClipboard(context, ".build-outputs/app-debug.apk")
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("copy_apk_path_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldCyan)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Path", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onOpenApkDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inspect_apk_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676))
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Full APK Package Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 2: INTERACTIVE SIMULATION SCREEN
// "add a simulation screen to see where and which profile blocks what"
// -------------------------------------------------------------------------------------------------

@Composable
fun SimulationScreen(
    profiles: List<Profile>,
    activeProfiles: List<Profile>,
    activeZones: List<DeadZone>,
    onNavigateToProfiles: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val displayMetrics = context.resources.displayMetrics
    val screenW = displayMetrics.widthPixels.toFloat()
    val screenH = displayMetrics.heightPixels.toFloat()

    // Assign consistent palette color per profile
    val profileColorMap = remember(profiles) {
        val colors = listOf(
            Color(0xFF00F2FE), // Cyan
            Color(0xFF10B981), // Emerald
            Color(0xFFF59E0B), // Amber
            Color(0xFFA855F7), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFF38BDF8)  // Blue
        )
        profiles.mapIndexed { idx, p -> p.id to colors[idx % colors.size] }.toMap()
    }

    val simulatedZoneList = remember(activeZones, activeProfiles) {
        activeZones.map { zone ->
            val owner = activeProfiles.find { it.id == zone.profileId }
            SimulatedZone(
                zone = zone,
                profileName = owner?.name ?: "Universal Profile",
                profileColor = profileColorMap[zone.profileId] ?: Color(0xFFFF4B4B)
            )
        }
    }

    // Touch testing interactive state
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var blockingHit by remember { mutableStateOf<SimulatedZone?>(null) }

    // Simulated Jitter Bursts
    val burstPoints = remember { mutableStateListOf<Offset>() }
    var isSimulatingBurst by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simulation Header Info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Interactive Touch Simulation",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Touch or drag across canvas below to see which profile intercepts where",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend of active profiles
                    if (activeProfiles.isEmpty()) {
                        Text(
                            text = "⚠️ No profiles currently active. Go to 'Profiles' tab to activate one or more profiles.",
                            color = WarningAmber,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToProfiles,
                            colors = ButtonDefaults.buttonColors(containerColor = ShieldCyan, contentColor = CyberDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Activate Profiles", fontSize = 12.sp)
                        }
                    } else {
                        Text("Active Profiles Blocking Now:", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(activeProfiles) { prof ->
                                val color = profileColorMap[prof.id] ?: ShieldCyan
                                val zoneCount = activeZones.count { it.profileId == prof.id }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                        .border(1.dp, color, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${prof.name} ($zoneCount zones)",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Touch Result Status Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        blockingHit != null -> Color(0xFF3B151E)
                        touchPos != null -> Color(0xFF132B20)
                        else -> CyberSurfaceCard
                    }
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        when {
                            blockingHit != null -> DangerNeon
                            touchPos != null -> SuccessEmerald
                            else -> CyberSurfaceBorder
                        }
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                blockingHit != null -> "🛑 BLOCKED by ${blockingHit?.profileName}"
                                touchPos != null -> "✅ ALLOWED (Normal Screen Interaction)"
                                else -> "Touch phone display below to test interception"
                            },
                            fontWeight = FontWeight.Bold,
                            color = when {
                                blockingHit != null -> DangerNeon
                                touchPos != null -> SuccessEmerald
                                else -> TextPrimary
                            },
                            fontSize = 14.sp
                        )
                        if (blockingHit != null) {
                            Text(
                                text = "Zone '${blockingHit?.zone?.label ?: "#${blockingHit?.zone?.id}"}' absorbs input. Underlying apps will NEVER receive this ghost tap.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFFCA5A5)
                            )
                        } else if (touchPos != null) {
                            Text(
                                text = "Input passes directly through to system & apps.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF86EFAC)
                            )
                        }
                    }
                    touchPos?.let { pos ->
                        Text(
                            text = "X: ${pos.x.toInt()}\nY: ${pos.y.toInt()}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        // Simulation Canvas (Phone Aspect Ratio)
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F172A))
                        .border(2.dp, ShieldCyan, RoundedCornerShape(24.dp))
                        .pointerInput(simulatedZoneList) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val canvasW = size.width
                                    val canvasH = size.height
                                    val realX = (offset.x / canvasW) * screenW
                                    val realY = (offset.y / canvasH) * screenH

                                    touchPos = Offset(realX, realY)
                                    blockingHit = simulatedZoneList.find { it.zone.contains(realX, realY) }
                                }
                            )
                        }
                        .pointerInput(simulatedZoneList) {
                            detectDragGestures { change, _ ->
                                val canvasW = size.width
                                val canvasH = size.height
                                val realX = (change.position.x / canvasW) * screenW
                                val realY = (change.position.y / canvasH) * screenH

                                touchPos = Offset(realX, realY)
                                blockingHit = simulatedZoneList.find { it.zone.contains(realX, realY) }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height
                        val scaleX = canvasW / screenW
                        val scaleY = canvasH / screenH

                        // Grid lines
                        for (i in 1..4) {
                            val y = canvasH * (i / 5f)
                            drawLine(Color(0x22334155), Offset(0f, y), Offset(canvasW, y), strokeWidth = 1f)
                        }
                        for (i in 1..3) {
                            val x = canvasW * (i / 4f)
                            drawLine(Color(0x22334155), Offset(x, 0f), Offset(x, canvasH), strokeWidth = 1f)
                        }

                        // Draw dead zones
                        for (item in simulatedZoneList) {
                            val z = item.zone
                            val zLeft = z.left * scaleX
                            val zTop = z.top * scaleY
                            val zW = z.width * scaleX
                            val zH = z.height * scaleY

                            // Fill
                            drawRect(
                                color = item.profileColor.copy(alpha = 0.35f),
                                topLeft = Offset(zLeft, zTop),
                                size = Size(zW, zH)
                            )
                            // Stroke
                            drawRect(
                                color = item.profileColor,
                                topLeft = Offset(zLeft, zTop),
                                size = Size(zW, zH),
                                style = Stroke(width = 2.5f)
                            )
                        }

                        // Draw user touch point
                        touchPos?.let { pos ->
                            val ptX = pos.x * scaleX
                            val ptY = pos.y * scaleY
                            val isBlocked = blockingHit != null
                            val ptColor = if (isBlocked) DangerNeon else SuccessEmerald

                            drawCircle(
                                color = ptColor.copy(alpha = 0.4f),
                                radius = 22f,
                                center = Offset(ptX, ptY)
                            )
                            drawCircle(
                                color = ptColor,
                                radius = 10f,
                                center = Offset(ptX, ptY)
                            )
                        }

                        // Draw phantom burst points
                        for (burst in burstPoints) {
                            val bX = burst.x * scaleX
                            val bY = burst.y * scaleY
                            val isHit = simulatedZoneList.any { it.zone.contains(burst.x, burst.y) }
                            drawCircle(
                                color = if (isHit) DangerNeon else SuccessEmerald,
                                radius = 12f,
                                center = Offset(bX, bY)
                            )
                        }
                    }

                    // On-canvas helper instructions
                    if (touchPos == null) {
                        Text(
                            text = "Tap or drag finger anywhere inside to test dead-zone shields",
                            color = Color(0x66FFFFFF),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }

        // Action: Generate Ghost Jitter Burst
        item {
            Button(
                onClick = {
                    if (isSimulatingBurst) return@Button
                    isSimulatingBurst = true
                    burstPoints.clear()
                    scope.launch {
                        repeat(12) {
                            val rx = Random.nextFloat() * screenW
                            val ry = Random.nextFloat() * screenH
                            burstPoints.add(Offset(rx, ry))
                            delay(120)
                        }
                        delay(1500)
                        burstPoints.clear()
                        isSimulatingBurst = false
                    }
                },
                enabled = !isSimulatingBurst,
                colors = ButtonDefaults.buttonColors(containerColor = ShieldElectricBlue, contentColor = CyberDark),
                modifier = Modifier.fillMaxWidth().testTag("simulate_jitter_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSimulatingBurst) "Simulating Phantom Touches..." else "Test Phantom Jitter Burst (12 random taps)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 3: PROFILES & ZONES (MULTI-PROFILE SELECTION & PER-FILTER TOGGLES)
// "make sure i select which profiles to active . and multiple profiles can be active at same time"
// -------------------------------------------------------------------------------------------------

@Composable
fun MultiProfileZonesTabContent(
    profiles: List<Profile>,
    activeProfiles: List<Profile>,
    viewingProfile: Profile?,
    viewingProfileZones: List<DeadZone>,
    onSelectViewingProfile: (Profile) -> Unit,
    onToggleProfileActive: (Profile, Boolean) -> Unit,
    onToggleZoneEnabled: (DeadZone, Boolean) -> Unit,
    onCreateProfileClick: () -> Unit,
    onDeleteProfileClick: (Profile) -> Unit,
    onAddZoneClick: () -> Unit,
    onAddPresetsClick: () -> Unit,
    onConfigureZoneClick: (DeadZone) -> Unit,
    onDeleteZoneClick: (DeadZone) -> Unit,
    onLaunchCalibration: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Multi-Profile Active Management Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Select Active Profiles",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Multiple profiles can be checked & active simultaneously",
                                color = ShieldCyan,
                                fontSize = 11.5.sp
                            )
                        }
                        OutlinedButton(
                            onClick = onCreateProfileClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("create_profile_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Profile", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    profiles.forEach { prof ->
                        val isViewing = prof.id == viewingProfile?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isViewing) Color(0xFF1E293B) else Color.Transparent)
                                .clickable { onSelectViewingProfile(prof) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = prof.isActive,
                                    onCheckedChange = { isChecked ->
                                        onToggleProfileActive(prof, isChecked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ShieldCyan,
                                        checkmarkColor = CyberDark,
                                        uncheckedColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("profile_checkbox_${prof.id}")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = prof.name,
                                        fontWeight = if (prof.isActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (prof.isActive) ShieldCyan else TextPrimary,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = "${prof.type.displayName} • ${if (prof.isActive) "RUNNING" else "STANDBY"}",
                                        fontSize = 11.sp,
                                        color = if (prof.isActive) SuccessEmerald else TextMuted
                                    )
                                }
                            }

                            if (prof.type != ProfileType.UNIVERSAL && profiles.size > 1) {
                                IconButton(
                                    onClick = { onDeleteProfileClick(prof) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Profile",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Profile Filters & Zones
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Filters in '${viewingProfile?.name ?: "None"}'",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${viewingProfileZones.size} zones defined (${viewingProfileZones.count { it.isEnabled }} enabled)",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onAddPresetsClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("add_presets_btn")
                    ) {
                        Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(13.dp), tint = ShieldAccentPurple)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Presets", fontSize = 11.sp, color = ShieldAccentPurple)
                    }
                    OutlinedButton(
                        onClick = onAddZoneClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("add_manual_zone_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Manual", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onLaunchCalibration,
                        colors = ButtonDefaults.buttonColors(containerColor = ShieldCyan, contentColor = CyberDark),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Calibrate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (viewingProfileZones.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Dead Zones In This Profile", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                        Text(
                            text = "Tap 'Calibrate' to scan phantom jitter or 'Add Manual' to create filters.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(viewingProfileZones) { zone ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (zone.isEnabled) CyberSurfaceCard else Color(0xFF161E2E)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (zone.isEnabled) CyberSurfaceBorder else Color(0xFF26334D))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = zone.label ?: "Shield Zone #${zone.id}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (zone.isEnabled) TextPrimary else TextMuted,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (!zone.isEnabled) {
                                    Badge(containerColor = Color(0xFF334155)) {
                                        Text("FILTER DISABLED", fontSize = 9.sp, color = TextMuted)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "[X: ${zone.left}..${zone.right}, Y: ${zone.top}..${zone.bottom}]",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (zone.isEnabled) ShieldElectricBlue else TextMuted
                            )
                            Text(
                                text = "Dimensions: ${zone.width} x ${zone.height} px (${zone.area} px²)",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Filter toggle switch
                            Switch(
                                checked = zone.isEnabled,
                                onCheckedChange = { isChecked -> onToggleZoneEnabled(zone, isChecked) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberDark,
                                    checkedTrackColor = ShieldCyan
                                ),
                                modifier = Modifier.testTag("zone_switch_${zone.id}")
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            // Configure Slider Button
                            IconButton(
                                onClick = { onConfigureZoneClick(zone) },
                                modifier = Modifier.testTag("configure_zone_${zone.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Bounds with Sliders", tint = ShieldCyan, modifier = Modifier.size(18.dp))
                            }

                            // Delete button
                            IconButton(
                                onClick = { onDeleteZoneClick(zone) },
                                modifier = Modifier.testTag("delete_zone_${zone.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Dead Zone", tint = DangerNeon, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// INTERACTIVE ZONE CONFIGURATOR DIALOG (WITH SLIDERS & LIVE PREVIEW CANVAS)
// "make sure we can configure the zones .. by adding extra and lower area by sliding and i can see the area when sliding.,."
// -------------------------------------------------------------------------------------------------

@Composable
fun ZoneConfiguratorDialog(
    zone: DeadZone,
    onDismiss: () -> Unit,
    onSave: (DeadZone) -> Unit
) {
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val screenW = displayMetrics.widthPixels.toFloat()
    val screenH = displayMetrics.heightPixels.toFloat()

    // Slider bounds state
    var leftVal by remember { mutableFloatStateOf(zone.left.toFloat().coerceIn(0f, screenW)) }
    var topVal by remember { mutableFloatStateOf(zone.top.toFloat().coerceIn(0f, screenH)) }
    var widthVal by remember { mutableFloatStateOf(zone.width.toFloat().coerceIn(20f, screenW)) }
    var heightVal by remember { mutableFloatStateOf(zone.height.toFloat().coerceIn(20f, screenH)) }
    var labelVal by remember { mutableStateOf(zone.label ?: "Shield Zone #${zone.id}") }
    var isEnabledVal by remember { mutableStateOf(zone.isEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ShieldCyan))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Configure Zone Area", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        Text("Drag sliders to resize bounds with live preview", fontSize = 11.5.sp, color = ShieldCyan)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // LIVE PREVIEW PHONE CANVAS (Updates dynamically as user drags sliders)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0B132B))
                        .border(1.5.dp, ShieldCyan, RoundedCornerShape(16.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cW = size.width
                        val cH = size.height
                        val sX = cW / screenW
                        val sY = cH / screenH

                        val rL = leftVal * sX
                        val rT = topVal * sY
                        val rW = (widthVal * sX).coerceAtMost(cW - rL)
                        val rH = (heightVal * sY).coerceAtMost(cH - rT)

                        // Draw live rectangle
                        drawRect(
                            color = if (isEnabledVal) DangerNeon.copy(alpha = 0.45f) else Color.Gray.copy(alpha = 0.25f),
                            topLeft = Offset(rL, rT),
                            size = Size(rW, rH)
                        )
                        drawRect(
                            color = if (isEnabledVal) DangerNeon else Color.Gray,
                            topLeft = Offset(rL, rT),
                            size = Size(rW, rH),
                            style = Stroke(width = 2.5f)
                        )
                    }

                    // Bounds badge on live canvas
                    Text(
                        text = "${widthVal.toInt()} x ${heightVal.toInt()} px",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Coordinate Summary Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("X (Left)", fontSize = 10.sp, color = TextMuted)
                        Text("${leftVal.toInt()} px", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Y (Top)", fontSize = 10.sp, color = TextMuted)
                        Text("${topVal.toInt()} px", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Width", fontSize = 10.sp, color = TextMuted)
                        Text("${widthVal.toInt()} px", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ShieldCyan)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Height", fontSize = 10.sp, color = TextMuted)
                        Text("${heightVal.toInt()} px", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ShieldCyan)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SLIDER 1: Width (Area Expand / Shrink)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Width (Horizontal Coverage):", fontSize = 12.sp, color = TextPrimary)
                        Text("${widthVal.toInt()} px", fontSize = 12.sp, color = ShieldCyan, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = widthVal,
                        onValueChange = { widthVal = it },
                        valueRange = 20f..screenW,
                        colors = SliderDefaults.colors(thumbColor = ShieldCyan, activeTrackColor = ShieldCyan),
                        modifier = Modifier.testTag("slider_width")
                    )
                }

                // SLIDER 2: Height (Lower / Upper Area)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Height (Lower / Vertical Area):", fontSize = 12.sp, color = TextPrimary)
                        Text("${heightVal.toInt()} px", fontSize = 12.sp, color = ShieldCyan, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = heightVal,
                        onValueChange = { heightVal = it },
                        valueRange = 20f..screenH,
                        colors = SliderDefaults.colors(thumbColor = ShieldCyan, activeTrackColor = ShieldCyan),
                        modifier = Modifier.testTag("slider_height")
                    )
                }

                // SLIDER 3: Position X (Left)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Position X (Left margin):", fontSize = 12.sp, color = TextPrimary)
                        Text("${leftVal.toInt()} px", fontSize = 12.sp, color = TextSecondary)
                    }
                    Slider(
                        value = leftVal,
                        onValueChange = { leftVal = it },
                        valueRange = 0f..(screenW - 20f),
                        colors = SliderDefaults.colors(thumbColor = TextSecondary, activeTrackColor = TextSecondary),
                        modifier = Modifier.testTag("slider_left")
                    )
                }

                // SLIDER 4: Position Y (Top)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Position Y (Top margin):", fontSize = 12.sp, color = TextPrimary)
                        Text("${topVal.toInt()} px", fontSize = 12.sp, color = TextSecondary)
                    }
                    Slider(
                        value = topVal,
                        onValueChange = { topVal = it },
                        valueRange = 0f..(screenH - 20f),
                        colors = SliderDefaults.colors(thumbColor = TextSecondary, activeTrackColor = TextSecondary),
                        modifier = Modifier.testTag("slider_top")
                    )
                }

                // Quick Margin Expanders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            leftVal = (leftVal - 25f).coerceAtLeast(0f)
                            topVal = (topVal - 25f).coerceAtLeast(0f)
                            widthVal = (widthVal + 50f).coerceAtMost(screenW)
                            heightVal = (heightVal + 50f).coerceAtMost(screenH)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+25px Margins", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            widthVal = (widthVal - 30f).coerceAtLeast(30f)
                            heightVal = (heightVal - 30f).coerceAtLeast(30f)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("-30px Shrink", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save Action
                Button(
                    onClick = {
                        val finalLeft = leftVal.toInt()
                        val finalTop = topVal.toInt()
                        val finalRight = (leftVal + widthVal).toInt()
                        val finalBottom = (topVal + heightVal).toInt()

                        onSave(
                            zone.copy(
                                left = finalLeft,
                                top = finalTop,
                                right = finalRight,
                                bottom = finalBottom,
                                label = labelVal,
                                isEnabled = isEnabledVal
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_zone_bounds_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldCyan, contentColor = CyberDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply & Save Bounds", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 4: DIAGNOSTICS & TELEMETRY
// -------------------------------------------------------------------------------------------------

@Composable
fun DiagnosticsTabContent(
    recentLogs: List<GhostTouchLog>,
    totalLogsCount: Int,
    blockedTouchLiveCount: Int,
    onClearLogs: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Live Intercepted", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$blockedTouchLiveCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerNeon
                        )
                        Text("ghost inputs dropped", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Diagnostic Records", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalLogsCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShieldCyan
                        )
                        Text("telemetry events", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Header with Clear Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detected Ghost Touch Logs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (recentLogs.isNotEmpty()) {
                    TextButton(onClick = onClearLogs, modifier = Modifier.testTag("clear_logs_btn")) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = DangerNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs", color = DangerNeon, fontSize = 12.sp)
                    }
                }
            }
        }

        if (recentLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Ghost Touches Logged Yet", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Touches intercepted by the shield or registered in calibration will appear here in real time.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recentLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (log.source == "BLOCKED") DangerNeon else ShieldCyan,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "X: ${log.x.toInt()}, Y: ${log.y.toInt()}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${log.source} • ${log.profileName}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    desc: String,
    isGranted: Boolean,
    onGrant: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
            Text(desc, fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = DangerNeon, contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.testTag(testTag)
        ) {
            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
