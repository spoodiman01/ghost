package com.example

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.engine.ProfileMerger
import com.example.model.GhostTouchLog
import com.example.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot

data class TouchPoint(val x: Float, val y: Float, val timestamp: Long)

class CalibrationActivity : ComponentActivity() {

    private val recordedPoints = mutableStateListOf<TouchPoint>()
    private var activeProfile by mutableStateOf<Profile?>(null)
    private var totalDurationSeconds by mutableIntStateOf(15)
    private var timeLeftSeconds by mutableIntStateOf(15)
    private var isCalibrating by mutableStateOf(true)

    // Window offset to ensure 1:1 physical raw screen coordinate alignment
    private var windowOffsetX = 0f
    private var windowOffsetY = 0f

    companion object {
        const val EXTRA_CALIBRATION_DURATION = "extra_calibration_duration"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read requested duration (default 15s)
        val requestedDuration = intent.getIntExtra(EXTRA_CALIBRATION_DURATION, 15).coerceIn(5, 300)
        totalDurationSeconds = requestedDuration
        timeLeftSeconds = requestedDuration

        // Keep screen on during calibration
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Load active profile
        val db = AppDatabase.getInstance(this)
        kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
            val profile = db.profileDao().getActiveProfileSync()
            activeProfile = profile
        }

        setContent {
            CalibrationScreen(
                points = recordedPoints,
                profile = activeProfile,
                totalDurationSeconds = totalDurationSeconds,
                timeLeftSeconds = timeLeftSeconds,
                onAddMoreTime = {
                    totalDurationSeconds += 10
                    timeLeftSeconds += 10
                    Toast.makeText(this@CalibrationActivity, "+10s added to calibration", Toast.LENGTH_SHORT).show()
                },
                onFinish = { finishCalibration() },
                onCancel = { finish() }
            )
        }

        startCountdownTimer()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val loc = IntArray(2)
            window.decorView.getLocationOnScreen(loc)
            windowOffsetX = loc[0].toFloat()
            windowOffsetY = loc[1].toFloat()
        }
    }

    private fun startCountdownTimer() {
        kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
            while (timeLeftSeconds > 0 && isCalibrating) {
                delay(1000)
                timeLeftSeconds--
            }
            if (isCalibrating) {
                finishCalibration()
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null && isCalibrating) {
            val action = event.actionMasked
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_POINTER_DOWN) {
                val now = System.currentTimeMillis()
                var addedAny = false

                // Process ALL simultaneous multi-touch pointers accurately
                for (i in 0 until event.pointerCount) {
                    val rawX: Float
                    val rawY: Float

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        rawX = event.getRawX(i)
                        rawY = event.getRawY(i)
                    } else {
                        rawX = event.getX(i) + windowOffsetX
                        rawY = event.getY(i) + windowOffsetY
                    }

                    // Deduplicate closely repeated points in time (<120ms and <30px)
                    val isDuplicate = recordedPoints.any { pt ->
                        (now - pt.timestamp < 120) && (hypot((pt.x - rawX).toDouble(), (pt.y - rawY).toDouble()) < 30.0)
                    }

                    if (!isDuplicate) {
                        recordedPoints.add(TouchPoint(rawX, rawY, now))
                        addedAny = true
                    }
                }

                if (addedAny) {
                    vibrateFeedback()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun vibrateFeedback() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(10)
        }
    }

    private fun finishCalibration() {
        if (!isCalibrating) return
        isCalibrating = false

        val profile = activeProfile
        if (profile == null) {
            Toast.makeText(this, "No active profile found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (recordedPoints.isEmpty()) {
            Toast.makeText(this, "No ghost touches detected during session.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@CalibrationActivity)
            val density = resources.displayMetrics.density
            val maxW = resources.displayMetrics.widthPixels
            val maxH = resources.displayMetrics.heightPixels

            // 1. Group points using dynamic density-aware proximity
            val clusterThresholdPx = (55 * density).toDouble() // ~55dp proximity grouping
            val clusters = mutableListOf<MutableList<TouchPoint>>()

            for (pt in recordedPoints) {
                var matchedCluster: MutableList<TouchPoint>? = null
                for (cluster in clusters) {
                    // Check if pt is close to ANY point in this cluster
                    if (cluster.any { other -> hypot((other.x - pt.x).toDouble(), (other.y - pt.y).toDouble()) <= clusterThresholdPx }) {
                        matchedCluster = cluster
                        break
                    }
                }
                if (matchedCluster != null) {
                    matchedCluster.add(pt)
                } else {
                    clusters.add(mutableListOf(pt))
                }
            }

            // 2. Convert clusters to padded bounding boxes
            val marginPadding = (20 * density).toInt()
            val minSize = (48 * density).toInt() // Minimum 48dp box for effective shielding

            val rawBoxes = clusters.map { cluster ->
                val minX = cluster.minOf { it.x }.toInt()
                val maxX = cluster.maxOf { it.x }.toInt()
                val minY = cluster.minOf { it.y }.toInt()
                val maxY = cluster.maxOf { it.y }.toInt()

                val bLeft = maxOf(0, minX - marginPadding)
                val bTop = maxOf(0, minY - marginPadding)
                val bRight = minOf(maxW, maxOf(maxX + marginPadding, bLeft + minSize))
                val bBottom = minOf(maxH, maxOf(maxY + marginPadding, bTop + minSize))

                ProfileMerger.RectBox(bLeft, bTop, bRight, bBottom)
            }

            // 3. CRITICAL: Merge all overlapping and adjacent boxes from multiple touches
            // into a single unified dead zone per spot! No duplicate overlapping boxes!
            val mergedBoxes = ProfileMerger.clusterRectangles(rawBoxes, threshold = (25 * density).toInt())

            val finalRects = mergedBoxes.map { b ->
                Rect(b.left, b.top, b.right, b.bottom)
            }

            // 4. Log sample detected touches for diagnostics
            for (pt in recordedPoints.take(30)) {
                db.ghostTouchLogDao().insertLog(
                    GhostTouchLog(
                        timestamp = pt.timestamp,
                        x = pt.x,
                        y = pt.y,
                        profileName = profile.name,
                        source = "CALIBRATION"
                    )
                )
            }

            // 5. Save unified dead zones into current profile
            ProfileMerger.mergeWithExistingZones(this@CalibrationActivity, profile.id, finalRects)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@CalibrationActivity,
                    "Saved ${finalRects.size} accurate dead-zone(s) into '${profile.name}'",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }
}

@Composable
fun CalibrationScreen(
    points: List<TouchPoint>,
    profile: Profile?,
    totalDurationSeconds: Int,
    timeLeftSeconds: Int,
    onAddMoreTime: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Full-screen rich minimal canvas
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF070B14) // Deep sci-fi matrix dark
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Live Interactive Touch Drawing Canvas (100% full screen)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                // Subtle ambient grid pattern
                val gridSpacing = 80f
                var gx = 0f
                while (gx < canvasW) {
                    drawLine(Color(0x0C00F2FE), Offset(gx, 0f), Offset(gx, canvasH), strokeWidth = 1f)
                    gx += gridSpacing
                }
                var gy = 0f
                while (gy < canvasH) {
                    drawLine(Color(0x0C00F2FE), Offset(0f, gy), Offset(canvasW, gy), strokeWidth = 1f)
                    gy += gridSpacing
                }

                // Render detected touch points with neon pulses
                for (pt in points) {
                    // Outer expanding holographic halo
                    drawCircle(
                        color = Color(0x3300F2FE),
                        radius = 36f * pulseScale,
                        center = Offset(pt.x, pt.y)
                    )
                    // Mid ripple ring
                    drawCircle(
                        color = Color(0x99FF3B30),
                        radius = 20f * pulseScale,
                        center = Offset(pt.x, pt.y),
                        style = Stroke(width = 2.5f)
                    )
                    // High-precision core point
                    drawCircle(
                        color = Color(0xFFFF3B30),
                        radius = 7f,
                        center = Offset(pt.x, pt.y)
                    )
                    // Crosshair tick marks for precision targeting feel
                    val crossLen = 14f
                    drawLine(Color(0xBB00F2FE), Offset(pt.x - crossLen, pt.y), Offset(pt.x + crossLen, pt.y), strokeWidth = 1.5f)
                    drawLine(Color(0xBB00F2FE), Offset(pt.x, pt.y - crossLen), Offset(pt.x, pt.y + crossLen), strokeWidth = 1.5f)
                }
            }

            // Top Status Progress Bar (flush against top edge)
            LinearProgressIndicator(
                progress = {
                    if (totalDurationSeconds > 0) {
                        ((totalDurationSeconds - timeLeftSeconds).coerceAtLeast(0)) / totalDurationSeconds.toFloat()
                    } else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color(0xFF00F2FE),
                trackColor = Color(0x331E293B),
            )

            // Ultra-minimal floating HUD pill at top
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xDD0F172A))
                        .border(1.2.dp, Color(0x6600F2FE), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (timeLeftSeconds <= 5) Color(0xFFFF4B4B) else Color(0xFF00F2FE), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${timeLeftSeconds}s",
                            color = if (timeLeftSeconds <= 5) Color(0xFFFF4B4B) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "/${totalDurationSeconds}s",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "•",
                            color = Color(0xFF475569),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${points.size} Hits",
                            color = if (points.isNotEmpty()) Color(0xFFFF4B4B) else Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // +10s chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC1E293B))
                        .border(1.dp, Color(0xFF00F2FE), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+10s",
                        color = Color(0xFF00F2FE),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.clickable { onAddMoreTime() }
                    )
                }
            }

            // Minimalist center hint (only shown when zero touches recorded)
            if (points.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0x4400F2FE),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Touch screen or keep still to map jitter",
                        color = Color(0x8894A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Floating bottom actions (clean, minimal, translucent floating dock)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xEE0B1120))
                    .border(1.dp, Color(0x4400F2FE), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Discard", fontSize = 12.5.sp)
                }

                Button(
                    onClick = onFinish,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00F2FE),
                        contentColor = Color(0xFF070B14)
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply Shield", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
