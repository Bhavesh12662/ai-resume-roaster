package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeAnalysisDao {
    @Query("SELECT * FROM resume_analyses ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<ResumeAnalysisEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: ResumeAnalysisEntity): Long

    @Delete
    suspend fun deleteAnalysis(analysis: ResumeAnalysisEntity)

    @Query("DELETE FROM resume_analyses")
    suspend fun clearAll()
}
