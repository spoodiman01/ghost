package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.data.PreferencesManager
import com.example.service.DeadZoneBlockerService
import com.example.service.FloatingControlService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val prefs = PreferencesManager.getInstance(context)
            if (prefs.autoStartOnBoot && prefs.isShieldEnabled && Settings.canDrawOverlays(context)) {
                // Auto-start touch shield blocker service
                val serviceIntent = Intent(context, DeadZoneBlockerService::class.java).apply {
                    this.action = DeadZoneBlockerService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // If floating tool was active, start it as well
                if (prefs.isFloatingToolEnabled) {
                    val floatingIntent = Intent(context, FloatingControlService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(floatingIntent)
                    } else {
                        context.startService(floatingIntent)
                    }
                }
            }
        }
    }
}
