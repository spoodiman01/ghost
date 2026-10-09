package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.DeadZone
import com.example.model.GhostTouchLog
import com.example.model.Profile
import com.example.model.ProfileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Profile::class, DeadZone::class, GhostTouchLog::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun deadZoneDao(): DeadZoneDao
    abstract fun ghostTouchLogDao(): GhostTouchLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ghost_touch_shield.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(getInstance(context))
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val profileDao = database.profileDao()
            val deadZoneDao = database.deadZoneDao()

            if (profileDao.getProfileCount() == 0) {
                // 1. Default Portrait Profile (Active)
                val defaultId = profileDao.insertProfile(
                    Profile(
                        name = "Default Shield",
                        type = ProfileType.PORTRAIT,
                        isActive = true
                    )
                )

                // Add starter sample dead zones (multiple realistic spots)
                deadZoneDao.insertDeadZone(
                    DeadZone(
                        profileId = defaultId,
                        left = 10,
                        top = 10,
                        right = 180,
                        bottom = 130,
                        label = "Top-Left Phantom Zone"
                    )
                )
                deadZoneDao.insertDeadZone(
                    DeadZone(
                        profileId = defaultId,
                        left = 860,
                        top = 10,
                        right = 1070,
                        bottom = 150,
                        label = "Top-Right Corner Crack"
                    )
                )
                deadZoneDao.insertDeadZone(
                    DeadZone(
                        profileId = defaultId,
                        left = 0,
                        top = 450,
                        right = 50,
                        bottom = 980,
                        label = "Left Edge Palm Jitter"
                    )
                )
                deadZoneDao.insertDeadZone(
                    DeadZone(
                        profileId = defaultId,
                        left = 280,
                        top = 2150,
                        right = 800,
                        bottom = 2340,
                        label = "Bottom Nav Phantom Tap"
                    )
                )

                // 2. Charging Mode Profile
                val chargingId = profileDao.insertProfile(
                    Profile(
                        name = "Charging Only",
                        type = ProfileType.CHARGING_ONLY,
                        isActive = false
                    )
                )
                deadZoneDao.insertDeadZone(
                    DeadZone(
                        profileId = chargingId,
                        left = 0,
                        top = 1920,
                        right = 320,
                        bottom = 2220,
                        label = "Bottom-Left USB Charger Noise"
                    )
                )
                deadZoneDao.insertDeadZone(
                    DeadZone(
                        profileId = chargingId,
                        left = 380,
                        top = 2100,
                        right = 700,
                        bottom = 2300,
                        label = "Bottom Connector Jitter"
                    )
                )
                deadZoneDao.insertDeadZone(
                    DeadZone(
                        profileId = chargingId,
                        left = 760,
                        top = 1940,
                        right = 1080,
                        bottom = 2240,
                        label = "Bottom-Right Port Surge"
                    )
                )

                // 3. Landscape Gaming Profile
                val landscapeId = profileDao.insertProfile(
                    Profile(
                        name = "Landscape Gaming",
                        type = ProfileType.LANDSCAPE,
                        isActive = false
                    )
                )
                deadZoneDao.insertDeadZone(
                    DeadZone(
                        profileId = landscapeId,
                        left = 0,
                        top = 620,
                        right = 110,
                        bottom = 960,
                        label = "Landscape Thumb Rest Ghost"
                    )
                )
                deadZoneDao.insertDeadZone(
                    DeadZone(
                        profileId = landscapeId,
                        left = 1920,
                        top = 0,
                        right = 2280,
                        bottom = 130,
                        label = "Landscape Shoulder Edge Glitch"
                    )
                )
            } else {
                // If an existing DB already exists with <= 2 dead zones, automatically augment with more presets
                val existingZones = deadZoneDao.getAllDeadZonesSync()
                if (existingZones.size <= 2) {
                    val defaultProfile = profileDao.getProfileByType(ProfileType.PORTRAIT)
                        ?: profileDao.getAllProfilesSync().firstOrNull()
                    defaultProfile?.let { prof ->
                        deadZoneDao.insertDeadZone(
                            DeadZone(
                                profileId = prof.id,
                                left = 860,
                                top = 10,
                                right = 1070,
                                bottom = 150,
                                label = "Top-Right Spiderweb Crack"
                            )
                        )
                        deadZoneDao.insertDeadZone(
                            DeadZone(
                                profileId = prof.id,
                                left = 0,
                                top = 450,
                                right = 50,
                                bottom = 980,
                                label = "Left Curved Edge Palm Rejection"
                            )
                        )
                        deadZoneDao.insertDeadZone(
                            DeadZone(
                                profileId = prof.id,
                                left = 280,
                                top = 2150,
                                right = 800,
                                bottom = 2340,
                                label = "Navigation Bar Phantom Tap"
                            )
                        )
                    }

                    val chargingProfile = profileDao.getProfileByType(ProfileType.CHARGING_ONLY)
                    chargingProfile?.let { chProf ->
                        deadZoneDao.insertDeadZone(
                            DeadZone(
                                profileId = chProf.id,
                                left = 0,
                                top = 1920,
                                right = 320,
                                bottom = 2220,
                                label = "Bottom-Left USB Charger Noise"
                            )
                        )
                        deadZoneDao.insertDeadZone(
                            DeadZone(
                                profileId = chProf.id,
                                left = 760,
                                top = 1940,
                                right = 1080,
                                bottom = 2240,
                                label = "Bottom-Right Port Surge"
                            )
                        )
                    }
                }
            }
        }
    }
}
