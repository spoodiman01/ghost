package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.model.Profile
import com.example.model.ProfileType
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles ORDER BY createdAt DESC")
    suspend fun getAllProfilesSync(): List<Profile>

    @Query("SELECT * FROM profiles WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveProfilesSync(): List<Profile>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfileSync(): Profile?

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): Profile?

    @Query("SELECT * FROM profiles WHERE type = :type LIMIT 1")
    suspend fun getProfileByType(type: ProfileType): Profile?

    @Query("SELECT * FROM profiles WHERE name = :name LIMIT 1")
    suspend fun getProfileByName(name: String): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Query("UPDATE profiles SET isActive = :isActive WHERE id = :id")
    suspend fun setProfileActive(id: Long, isActive: Boolean)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun activateProfileById(id: Long)

    @Transaction
    suspend fun setActiveProfile(targetId: Long) {
        deactivateAllProfiles()
        activateProfileById(targetId)
    }

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int

    @Query("SELECT COUNT(*) FROM profiles WHERE isActive = 1")
    fun getActiveProfileCount(): Flow<Int>
}
