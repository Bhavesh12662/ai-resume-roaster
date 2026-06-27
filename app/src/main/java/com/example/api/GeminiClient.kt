package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Common helper to call Gemini API and return raw response text
     */
    private suspend fun callGeminiApi(prompt: String, systemInstruction: String? = null, forceJson: Boolean = true): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default!")
            return@withContext "{\"error\": \"Gemini API key is not configured. Please add your key to the Secrets panel in Google AI Studio.\"}"
        }

        val url = "$BASE_URL?key=$apiKey"

        // Build the contents JSON
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)

            // System instructions
            if (systemInstruction != null) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
            }

            // Generation config
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.75)
                if (forceJson) {
                    put("responseMimeType", "application/json")
                }
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response: Code ${response.code}, body: $responseStr")
                    return@withContext "{\"error\": \"API error: Code ${response.code}. Please ensure your API Key is valid and active.\"}"
                }

                // Parse the text content out of Gemini's candidate response
                val mainJson = JSONObject(responseStr)
                val candidates = mainJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "")
                        }
                    }
                }
                return@withContext "{\"error\": \"Failed to retrieve candidate content from response.\"}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API", e)
            return@withContext "{\"error\": \"Network error: ${e.localizedMessage}\"}"
        }
    }

    /**
     * Analyzes and Roasts the resume text!
     */
    suspend fun analyzeResume(resumeText: String, tone: String, focusMode: String): String {
        val systemPrompt = """
            You are a seasoned IT recruiter, ATS optimization guru, professional career coach, and a hilarious technical roaster.
            Your task is to analyze the provided resume text thoroughly and return a comprehensive analysis in strict, valid JSON format.
            You must apply the roast tone: $tone.
            - If tone is 'Friendly', be constructive, warm, but make a few lighthearted, playful jokes about typical resume blunders.
            - If tone is 'Professional', deliver realistic, corporate HR-style burns and highly technical, standard recruiting advice.
            - If tone is 'Savage', unleash absolutely brutal, savage, and hilarious roast lines. Burn their buzzwords, objective statements, empty projects, lack of metrics, and formatting choices. Ensure it remains safe, engaging, and extremely funny.
            
            The current resume focus is: $focusMode (either 'General Career' or 'Tech/Developer Focus').
            If focus is 'Tech/Developer Focus', provide extra deep analysis for GitHub links, tech stack keywords, projects complexity, and code-related gaps.
            
            You must return a single JSON object conforming exactly to this structure:
            {
              "overallScore": 85,
              "atsScore": 80,
              "hrScore": 75,
              "keywordMatch": 70,
              "grammarScore": 90,
              "formattingScore": 85,
              "readabilityScore": 88,
              "roast": "This is your customized roast text based on the selected tone...",
              "atsFeedback": "A markdown/bulleted feedback checklist detailing ATS issues (e.g., fonts, layout, tables, icons, columns, headers, footers)...",
              "hrFeedback": "A markdown/bulleted HR professional feedback checklist (first impressions, clarity, impact, career progression, strengths, weaknesses)...",
              "grammarIssues": [
                { "original": "original grammatical error text", "suggestion": "improved grammar text", "explanation": "why this is an issue" }
              ],
              "impactUpdates": [
                { "original": "Responsible for managing standard SQL databases", "replacement": "Optimized SQL database query indexing, increasing application performance by 35% and saving 10 hours in weekly maintenance tasks" }
              ],
              "sectionAnalysis": [
                { "sectionName": "Summary", "status": "Exists/Missing", "recommendation": "What to write or change here" }
              ],
              "contactIssues": [
                { "item": "LinkedIn", "status": "Valid/Missing/Broken", "recommendation": "Actionable feedback" }
              ],
              "developerMetrics": {
                "githubScore": 75,
                "projectsScore": 80,
                "techStackMatch": "Good / Needs Work",
                "feedback": "Deep feedback on developers' repositories, portfolio presence, and open source."
              },
              "skillGapAnalysis": [
                { "role": "Frontend Developer", "missingSkills": ["TypeScript", "Next.js"], "recommendation": "Focus on React eco-system..." }
              ],
              "salaryEstimation": {
                "rangeMin": "${'$'}80,000",
                "rangeMax": "${'$'}120,000",
                "currency": "USD",
                "marketContext": "Based on current tech market trends for your experience level..."
              }
            }
            Do NOT include any markdown code blocks (like ```json) in your response. Just return the raw JSON object string.
        """.trimIndent()

        val prompt = "Here is the Resume Text to analyze:\n\n$resumeText"
        return callGeminiApi(prompt = prompt, systemInstruction = systemPrompt, forceJson = true)
    }

    /**
     * On-demand Cover Letter Generator
     */
    suspend fun generateCoverLetter(resumeText: String, jdText: String, companyName: String, role: String): String {
        val systemPrompt = """
            You are an expert resume writer and career coach.
            Your goal is to write a highly persuasive, professional, and ATS-optimized Cover Letter based on the user's Resume and the Job Description.
            The letter should align the user's achievements with the job requirements and maintain an impactful, professional tone.
            
            Return a JSON object conforming to this structure:
            {
              "coverLetter": "Dear Hiring Team,\n\nI am thrilled to apply for the [Role] position at [Company]..."
            }
            Do NOT wrap in markdown blocks. Return raw JSON.
        """.trimIndent()

        val prompt = """
            Resume:
            $resumeText
            
            Job Details:
            Company: $companyName
            Role: $role
            Job Description:
            $jdText
        """.trimIndent()

        val result = callGeminiApi(prompt = prompt, systemInstruction = systemPrompt, forceJson = true)
        return try {
            JSONObject(result).optString("coverLetter", "Failed to generate Cover Letter.")
        } catch (e: Exception) {
            "Error parsing Cover Letter: $result"
        }
    }

    /**
     * On-demand Interview Preparation Q&A Generator (Top 9 custom questions)
     */
    suspend fun generateInterviewPrep(resumeText: String, targetRole: String): String {
        val systemPrompt = """
            You are an expert HR interviewer and hiring manager.
            Based on the candidate's resume and their target role ($targetRole), generate 9 highly customized interview questions of varying difficulties (3 Easy, 3 Medium, 3 Hard).
            For each question, provide a sample perfect answer tailored specifically to the candidate's achievements.
            
            Return a JSON object conforming exactly to this structure:
            {
              "questions": [
                {
                  "question": "The interview question text",
                  "difficulty": "Easy / Medium / Hard",
                  "sampleAnswer": "A highly detailed, professional sample answer showcasing the resume's skills."
                }
              ]
            }
            Do NOT wrap in markdown blocks. Return raw JSON.
        """.trimIndent()

        val prompt = "Resume:\n$resumeText"
        return callGeminiApi(prompt = prompt, systemInstruction = systemPrompt, forceJson = true)
    }

    /**
     * On-demand 90-Day Learning Roadmap
     */
    suspend fun generateCareerRoadmap(resumeText: String, targetRole: String): String {
        val systemPrompt = """
            You are a modern Tech Career Coach and Skills Expert.
            Analyze the user's resume, and generate a customized 90-day learning roadmap to transition into or excel as a '$targetRole'.
            Include actionable milestones, certification recommendations, course topics, and project ideas.
            
            Return a JSON object conforming exactly to this structure:
            {
              "plan90Days": [
                "Days 1-30: Core milestones and specific technologies to master...",
                "Days 31-60: Intermediate skills and projects...",
                "Days 61-90: Advanced optimization, certificates, and portfolio prep..."
              ],
              "recommendedCourses": [
                "Course topic 1: e.g. Advanced System Design",
                "Course topic 2: e.g. State Management in Jetpack Compose"
              ],
              "recommendedCertifications": [
                "Google Associate Android Developer",
                "AWS Certified Solutions Architect"
              ],
              "suggestedProject": "Description of a portfolio-worthy project they should build to bridge the gap."
            }
            Do NOT wrap in markdown blocks. Return raw JSON.
        """.trimIndent()

        val prompt = "Resume:\n$resumeText"
        return callGeminiApi(prompt = prompt, systemInstruction = systemPrompt, forceJson = true)
    }

    /**
     * Job Description Matcher
     */
    suspend fun matchJobDescription(resumeText: String, jdText: String): String {
        val systemPrompt = """
            You are an ATS Parser and Recruitment Algorithm.
            Compare the user's Resume with the Job Description pasted.
            Calculate an accurate Match Percentage, list missing keywords, missing skills, and detailed improvement suggestions.
            
            Return a JSON object conforming exactly to this structure:
            {
              "matchPercentage": 72,
              "missingKeywords": ["Docker", "K8s", "CI/CD"],
              "missingSkills": ["Cloud Architecture", "Unit Testing"],
              "suggestions": [
                "Rephrase your experience section to include keywords like 'CI/CD orchestration'.",
                "Add a Docker project to showcase containerization skills."
              ]
            }
            Do NOT wrap in markdown blocks. Return raw JSON.
        """.trimIndent()

        val prompt = """
            Resume:
            $resumeText
            
            Job Description:
            $jdText
        """.trimIndent()

        return callGeminiApi(prompt = prompt, systemInstruction = systemPrompt, forceJson = true)
    }
}
