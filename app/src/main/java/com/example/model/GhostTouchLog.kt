package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ghost_touch_logs")
data class GhostTouchLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val x: Float,
    val y: Float,
    val profileName: String = "Active Profile",
    val source: String = "BLOCKED" // "BLOCKED" or "CALIBRATION"
)
