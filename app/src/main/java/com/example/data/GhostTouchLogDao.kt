package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.model.GhostTouchLog
import kotlinx.coroutines.flow.Flow

@Dao
interface GhostTouchLogDao {

    @Query("SELECT * FROM ghost_touch_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<GhostTouchLog>>

    @Query("SELECT COUNT(*) FROM ghost_touch_logs")
    fun getTotalLogCount(): Flow<Int>

    @Insert
    suspend fun insertLog(log: GhostTouchLog)

    @Query("DELETE FROM ghost_touch_logs")
    suspend fun clearLogs()
}
