package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resume_analyses")
data class ResumeAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tone: String,
    val overallScore: Int,
    val atsScore: Int,
    val hrScore: Int,
    val resultJson: String
)
