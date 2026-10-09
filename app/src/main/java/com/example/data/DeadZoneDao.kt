package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.DeadZone
import kotlinx.coroutines.flow.Flow

@Dao
interface DeadZoneDao {

    @Query("SELECT * FROM dead_zones WHERE profileId = :profileId ORDER BY id ASC")
    fun getDeadZonesForProfile(profileId: Long): Flow<List<DeadZone>>

    @Query("SELECT * FROM dead_zones WHERE profileId = :profileId ORDER BY id ASC")
    suspend fun getDeadZonesForProfileSync(profileId: Long): List<DeadZone>

    @Query("SELECT * FROM dead_zones WHERE profileId IN (:profileIds) ORDER BY id ASC")
    fun getDeadZonesForProfiles(profileIds: List<Long>): Flow<List<DeadZone>>

    @Query("SELECT * FROM dead_zones WHERE profileId IN (:profileIds) ORDER BY id ASC")
    suspend fun getDeadZonesForProfilesSync(profileIds: List<Long>): List<DeadZone>

    @Query("SELECT * FROM dead_zones ORDER BY id ASC")
    suspend fun getAllDeadZonesSync(): List<DeadZone>

    @Query("SELECT * FROM dead_zones ORDER BY id ASC")
    fun getAllDeadZones(): Flow<List<DeadZone>>

    @Query("SELECT COUNT(*) FROM dead_zones")
    fun getTotalDeadZoneCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dead_zones WHERE profileId = :profileId")
    fun getDeadZoneCountForProfile(profileId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeadZone(deadZone: DeadZone): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeadZones(deadZones: List<DeadZone>)

    @Update
    suspend fun updateDeadZone(deadZone: DeadZone)

    @Query("UPDATE dead_zones SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setZoneEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE dead_zones SET `left` = :left, `top` = :top, `right` = :right, `bottom` = :bottom WHERE id = :id")
    suspend fun updateZoneBounds(id: Long, left: Int, top: Int, right: Int, bottom: Int)

    @Delete
    suspend fun deleteDeadZone(deadZone: DeadZone)

    @Query("DELETE FROM dead_zones WHERE id = :id")
    suspend fun deleteDeadZoneById(id: Long)

    @Query("DELETE FROM dead_zones WHERE profileId = :profileId")
    suspend fun deleteDeadZonesForProfile(profileId: Long)
}
