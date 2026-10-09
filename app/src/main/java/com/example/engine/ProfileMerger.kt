package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import com.example.data.AppDatabase
import com.example.model.DeadZone
import com.example.model.Profile
import com.example.model.ProfileType
import com.example.service.DeadZoneBlockerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProfileMerger {

    const val PROXIMITY_THRESHOLD_PX = 25

    data class RectBox(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val width: Int get() = maxOf(0, right - left)
        val height: Int get() = maxOf(0, bottom - top)

        fun canMergeWith(other: RectBox, threshold: Int): Boolean {
            // Check if bounding boxes expanded by threshold overlap
            val noOverlapX = (this.right + threshold < other.left) || (other.right + threshold < this.left)
            val noOverlapY = (this.bottom + threshold < other.top) || (other.bottom + threshold < this.top)
            return !(noOverlapX || noOverlapY)
        }

        fun unionWith(other: RectBox): RectBox {
            return RectBox(
                left = minOf(this.left, other.left),
                top = minOf(this.top, other.top),
                right = maxOf(this.right, other.right),
                bottom = maxOf(this.bottom, other.bottom)
            )
        }
    }

    /**
     * Iterative spatial clustering of rectangles:
     * Continually clusters any pair of boxes within proximity threshold into their bounding union
     * until a stable partition is reached.
     */
    fun clusterRectangles(boxes: List<RectBox>, threshold: Int = PROXIMITY_THRESHOLD_PX): List<RectBox> {
        if (boxes.size <= 1) return boxes

        val currentList = boxes.toMutableList()
        var mergedInPass = true

        while (mergedInPass) {
            mergedInPass = false
            var i = 0
            while (i < currentList.size) {
                var j = i + 1
                while (j < currentList.size) {
                    if (currentList[i].canMergeWith(currentList[j], threshold)) {
                        val merged = currentList[i].unionWith(currentList[j])
                        currentList[i] = merged
                        currentList.removeAt(j)
                        mergedInPass = true
                        // Restart check for expanded box i
                        continue
                    }
                    j++
                }
                i++
            }
        }
        return currentList
    }

    /**
     * Reads all existing dead zones across ALL profiles, clusters them,
     * writes/updates the "Universal Profile", sets it active, and pushes live to blocker service.
     */
    suspend fun createOrUpdateUniversalProfile(context: Context): Profile = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val profileDao = db.profileDao()
        val deadZoneDao = db.deadZoneDao()

        // 1. Gather all dead zones from non-universal profiles
        val nonUniversalProfiles = profileDao.getAllProfilesSync().filter { it.type != ProfileType.UNIVERSAL }
        val nonUniversalIds = nonUniversalProfiles.map { it.id }
        val collectedZones = if (nonUniversalIds.isNotEmpty()) {
            deadZoneDao.getDeadZonesForProfilesSync(nonUniversalIds).toMutableList()
        } else {
            deadZoneDao.getAllDeadZonesSync().toMutableList()
        }

        // If there are few zones, augment with standard hardware presets so Universal has full protection
        val boxList = collectedZones.filter { it.isValid() }.map {
            RectBox(it.left, it.top, it.right, it.bottom)
        }.toMutableList()

        if (boxList.size < 5) {
            val presets = com.example.model.DeadZonePreset.ALL_PRESETS.take(6)
            for (preset in presets) {
                boxList.add(RectBox(preset.left, preset.top, preset.right, preset.bottom))
            }
        }

        // 2. Run spatial clustering
        val clusteredBoxes = clusterRectangles(boxList, PROXIMITY_THRESHOLD_PX)

        // 3. Find or create Universal Profile
        var universalProfile = profileDao.getProfileByType(ProfileType.UNIVERSAL)
            ?: profileDao.getProfileByName("Universal Profile")

        val profileId: Long
        if (universalProfile == null) {
            val newProfile = Profile(
                name = "Universal Profile",
                type = ProfileType.UNIVERSAL,
                isActive = true
            )
            profileId = profileDao.insertProfile(newProfile)
            universalProfile = newProfile.copy(id = profileId)
        } else {
            profileId = universalProfile.id
            // Clear existing zones for Universal Profile
            deadZoneDao.deleteDeadZonesForProfile(profileId)
        }

        // 4. Insert clustered zones into Universal Profile
        val newDeadZones = clusteredBoxes.mapIndexed { index, box ->
            DeadZone(
                profileId = profileId,
                left = box.left,
                top = box.top,
                right = box.right,
                bottom = box.bottom,
                label = "Universal Shield #${index + 1} (${box.width}x${box.height})"
            )
        }
        if (newDeadZones.isNotEmpty()) {
            deadZoneDao.insertDeadZones(newDeadZones)
        }

        // 5. Set active
        profileDao.setActiveProfile(profileId)

        // 6. Notify blocker service live
        notifyBlockerService(context)

        return@withContext universalProfile.copy(isActive = true)
    }

    /**
     * Adds hardware fault presets into a specific profile.
     */
    suspend fun addPresetsToProfile(
        context: Context,
        profileId: Long,
        presets: List<com.example.model.DeadZonePreset>
    ) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val deadZoneDao = db.deadZoneDao()
        val zones = presets.map { p ->
            DeadZone(
                profileId = profileId,
                left = p.left,
                top = p.top,
                right = p.right,
                bottom = p.bottom,
                label = p.name,
                isEnabled = true
            )
        }
        deadZoneDao.insertDeadZones(zones)
        notifyBlockerService(context)
    }

    /**
     * Consolidates selected profiles into a single custom named profile.
     */
    suspend fun mergeSelectedProfiles(
        context: Context,
        sourceProfileIds: List<Long>,
        targetName: String
    ): Profile = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val profileDao = db.profileDao()
        val deadZoneDao = db.deadZoneDao()

        // 1. Collect zones from selected profile IDs
        val collectedZones = mutableListOf<DeadZone>()
        for (pId in sourceProfileIds) {
            collectedZones.addAll(deadZoneDao.getDeadZonesForProfileSync(pId))
        }

        val boxList = collectedZones.filter { it.isValid() }.map {
            RectBox(it.left, it.top, it.right, it.bottom)
        }

        // 2. Spatial cluster
        val clustered = clusterRectangles(boxList, PROXIMITY_THRESHOLD_PX)

        // 3. Create new profile
        val profile = Profile(
            name = targetName.ifBlank { "Merged Shield (${sourceProfileIds.size} profiles)" },
            type = ProfileType.CUSTOM,
            isActive = true
        )
        val newProfileId = profileDao.insertProfile(profile)

        // 4. Insert dead zones
        val mergedZones = clustered.mapIndexed { index, box ->
            DeadZone(
                profileId = newProfileId,
                left = box.left,
                top = box.top,
                right = box.right,
                bottom = box.bottom,
                label = "Merged Zone #${index + 1}"
            )
        }
        if (mergedZones.isNotEmpty()) {
            deadZoneDao.insertDeadZones(mergedZones)
        }

        // 5. Set active
        profileDao.setActiveProfile(newProfileId)

        // 6. Notify blocker
        notifyBlockerService(context)

        return@withContext profile.copy(id = newProfileId, isActive = true)
    }

    /**
     * Merges newly calibrated bounding boxes into the target profile without replacing prior zones.
     */
    suspend fun mergeWithExistingZones(
        context: Context,
        profileId: Long,
        newBoxes: List<Rect>
    ) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val deadZoneDao = db.deadZoneDao()

        val existing = deadZoneDao.getDeadZonesForProfileSync(profileId)
        val combinedBoxes = (existing.map { RectBox(it.left, it.top, it.right, it.bottom) } +
                newBoxes.map { RectBox(it.left, it.top, it.right, it.bottom) })

        val clustered = clusterRectangles(combinedBoxes, PROXIMITY_THRESHOLD_PX)

        // Replace profile's zones with clustered result
        deadZoneDao.deleteDeadZonesForProfile(profileId)
        val finalDeadZones = clustered.mapIndexed { index, box ->
            DeadZone(
                profileId = profileId,
                left = box.left,
                top = box.top,
                right = box.right,
                bottom = box.bottom,
                label = "Shield Zone #${index + 1}"
            )
        }
        if (finalDeadZones.isNotEmpty()) {
            deadZoneDao.insertDeadZones(finalDeadZones)
        }

        notifyBlockerService(context)
    }

    fun notifyBlockerService(context: Context) {
        try {
            val intent = Intent(context, DeadZoneBlockerService::class.java).apply {
                action = DeadZoneBlockerService.ACTION_RELOAD_ZONES
            }
            context.startService(intent)
        } catch (_: Exception) {
            // Service might not be started yet; will read on startup
        }
    }
}
