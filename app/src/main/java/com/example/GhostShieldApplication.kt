package com.example

import android.app.Application
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GhostShieldApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Warm up and initialize Room database
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@GhostShieldApplication)
            AppDatabase.populateInitialData(db)
        }
    }
}
