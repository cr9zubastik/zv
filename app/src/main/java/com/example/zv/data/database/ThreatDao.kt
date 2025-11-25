package com.example.zv.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatDao {
    @Query("SELECT * FROM threats ORDER BY detectedAt DESC")
    fun getAllThreats(): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE id = :id")
    suspend fun getThreatById(id: Long): ThreatEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreat(threat: ThreatEntity): Long
    
    @Query("DELETE FROM threats WHERE id = :id")
    suspend fun deleteThreat(id: Long)
    
    @Query("DELETE FROM threats")
    suspend fun deleteAllThreats()
    
    @Query("SELECT COUNT(*) FROM threats")
    suspend fun getThreatCount(): Int
}

