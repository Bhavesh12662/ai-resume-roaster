package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileAndCareerDao {

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 'current_user' LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    // Job Applications
    @Query("SELECT * FROM job_applications ORDER BY id DESC")
    fun getAllJobApplications(): Flow<List<JobApplicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobApplication(application: JobApplicationEntity): Long

    @Delete
    suspend fun deleteJobApplication(application: JobApplicationEntity)

    @Query("DELETE FROM job_applications")
    suspend fun clearAllJobApplications()
}
