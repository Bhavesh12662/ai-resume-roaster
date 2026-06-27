package com.example.data

import kotlinx.coroutines.flow.Flow

class ProfileAndCareerRepository(private val dao: ProfileAndCareerDao) {

    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    
    val allJobApplications: Flow<List<JobApplicationEntity>> = dao.getAllJobApplications()

    suspend fun insertProfile(profile: UserProfileEntity) {
        dao.insertProfile(profile)
    }

    suspend fun insertJobApplication(application: JobApplicationEntity): Long {
        return dao.insertJobApplication(application)
    }

    suspend fun deleteJobApplication(application: JobApplicationEntity) {
        dao.deleteJobApplication(application)
    }

    suspend fun clearAllJobApplications() {
        dao.clearAllJobApplications()
    }
}
