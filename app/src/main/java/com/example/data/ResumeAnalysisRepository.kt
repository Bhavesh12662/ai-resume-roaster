package com.example.data

import kotlinx.coroutines.flow.Flow

class ResumeAnalysisRepository(private val dao: ResumeAnalysisDao) {
    val allAnalyses: Flow<List<ResumeAnalysisEntity>> = dao.getAllAnalyses()

    suspend fun insertAnalysis(analysis: ResumeAnalysisEntity): Long {
        return dao.insertAnalysis(analysis)
    }

    suspend fun deleteAnalysis(analysis: ResumeAnalysisEntity) {
        dao.deleteAnalysis(analysis)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
