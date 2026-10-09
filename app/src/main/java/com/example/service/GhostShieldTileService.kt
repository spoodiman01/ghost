package com.example.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.engine.ProfileMerger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class GhostShieldTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val isCurrentlyActive = DeadZoneBlockerService.isRunning.value
        val prefs = PreferencesManager.getInstance(this)

        if (isCurrentlyActive) {
            // Stop blocker
            val stopIntent = Intent(this, DeadZoneBlockerService::class.java).apply {
                action = DeadZoneBlockerService.ACTION_STOP
            }
            startService(stopIntent)
            prefs.isShieldEnabled = false
        } else {
            // Start blocker
            val startIntent = Intent(this, DeadZoneBlockerService::class.java).apply {
                action = DeadZoneBlockerService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
            prefs.isShieldEnabled = true
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = DeadZoneBlockerService.isRunning.value

        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Ghost Shield"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            serviceScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@GhostShieldTileService)
                val activeProfile = db.profileDao().getActiveProfileSync()
                val profileName = activeProfile?.name ?: "No Profile"
                tile.subtitle = if (isRunning) "$profileName (ON)" else "Inactive (OFF)"
                tile.updateTile()
            }
        } else {
            tile.updateTile()
        }
    }
}
