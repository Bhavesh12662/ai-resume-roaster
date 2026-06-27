package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "current_user",
    
    // Basic Profile info
    val fullName: String = "John Doe",
    val username: String = "johndoe",
    val email: String = "john.doe@example.com",
    val phone: String = "+1 (555) 019-2834",
    val dob: String = "1998-05-15",
    val gender: String = "Male",
    val country: String = "United States",
    val state: String = "California",
    val city: String = "San Francisco",
    val currentJobTitle: String = "Software Engineer",
    val experienceLevel: String = "Mid-Level",
    val education: String = "Bachelor of Science in Computer Science",
    val university: String = "Stanford University",
    val graduationYear: String = "2020",
    val profilePhotoUri: String = "",

    // Career Information
    val careerGoal: String = "Become a Principal Solutions Architect and build large-scale cloud-native apps.",
    val preferredJobRole: String = "Senior Android Engineer / Staff Full Stack Developer",
    val skills: String = "Android,Kotlin,Jetpack Compose,Java,Node.js,React,AWS,GraphQL,Git,CI/CD",
    val programmingLanguages: String = "Kotlin,Java,TypeScript,JavaScript,Python,SQL",
    val frameworks: String = "Jetpack Compose,Spring Boot,React.js,Next.js,Express",
    val tools: String = "Docker,Kubernetes,Git,Firebase,Figma,Jira",
    val certifications: String = "AWS Certified Developer Associate, Google Associate Android Developer",
    val languagesKnown: String = "English (Fluent), Spanish (Conversational)",
    val yearsOfExperience: Int = 4,
    val currentCompany: String = "Acme Tech Solutions",
    val portfolioWebsite: String = "https://johndoe.dev",
    val linkedinProfile: String = "https://linkedin.com/in/johndoe",
    val githubProfile: String = "https://github.com/johndoe",
    val leetcodeProfile: String = "https://leetcode.com/johndoe",
    val hackerrankProfile: String = "https://hackerrank.com/johndoe",

    // Preferences & Settings
    val isDarkMode: Boolean = false,
    val languageSelection: String = "English",
    val emailNotificationsEnabled: Boolean = true,
    
    // Security info
    val lastLogin: String = "June 27, 2026, 05:00 AM",
    val loginDevices: String = "Google Pixel 8, macOS Ventura Desktop",
    val accountStatus: String = "Active",
    val emailVerificationStatus: String = "Verified",
    val twoFactorEnabled: Boolean = false
) {
    fun calculateCompletionPercentage(): Int {
        val fields = listOf(
            fullName, username, email, phone, dob, country, state, city,
            currentJobTitle, experienceLevel, education, university, graduationYear,
            careerGoal, preferredJobRole, skills, programmingLanguages, frameworks,
            tools, certifications, languagesKnown, currentCompany, portfolioWebsite,
            linkedinProfile, githubProfile, leetcodeProfile, hackerrankProfile
        )
        val completed = fields.count { it.isNotBlank() }
        return ((completed.toFloat() / fields.size) * 100).toInt()
    }
}
