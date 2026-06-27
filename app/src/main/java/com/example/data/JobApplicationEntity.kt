package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyName: String = "",
    val jobTitle: String = "",
    val location: String = "",
    val appliedDate: String = "",
    val status: String = ""
)
