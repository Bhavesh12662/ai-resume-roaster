package com.example.data

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class AnalysisResult(
    val overallScore: Int = 0,
    val atsScore: Int = 0,
    val hrScore: Int = 0,
    val keywordMatch: Int = 0,
    val grammarScore: Int = 0,
    val formattingScore: Int = 0,
    val readabilityScore: Int = 0,
    val roast: String = "",
    val atsFeedback: String = "",
    val hrFeedback: String = "",
    val grammarIssues: List<GrammarIssue> = emptyList(),
    val impactUpdates: List<ImpactUpdate> = emptyList(),
    val sectionAnalysis: List<SectionCheck> = emptyList(),
    val contactIssues: List<ContactCheck> = emptyList(),
    val developerMetrics: DeveloperMetrics? = null,
    val skillGapAnalysis: List<SkillGap> = emptyList(),
    val salaryEstimation: SalaryEstimation = SalaryEstimation()
) {
    companion object {
        fun fromJson(jsonStr: String): AnalysisResult {
            try {
                val json = JSONObject(jsonStr)
                
                val grammarList = mutableListOf<GrammarIssue>()
                val grammarArr = json.optJSONArray("grammarIssues")
                if (grammarArr != null) {
                    for (i in 0 until grammarArr.length()) {
                        val obj = grammarArr.optJSONObject(i)
                        if (obj != null) {
                            grammarList.add(GrammarIssue(
                                original = obj.optString("original", ""),
                                suggestion = obj.optString("suggestion", ""),
                                explanation = obj.optString("explanation", "")
                            ))
                        }
                    }
                }

                val impactList = mutableListOf<ImpactUpdate>()
                val impactArr = json.optJSONArray("impactUpdates")
                if (impactArr != null) {
                    for (i in 0 until impactArr.length()) {
                        val obj = impactArr.optJSONObject(i)
                        if (obj != null) {
                            impactList.add(ImpactUpdate(
                                original = obj.optString("original", ""),
                                replacement = obj.optString("replacement", "")
                            ))
                        }
                    }
                }

                val sectionList = mutableListOf<SectionCheck>()
                val sectionArr = json.optJSONArray("sectionAnalysis")
                if (sectionArr != null) {
                    for (i in 0 until sectionArr.length()) {
                        val obj = sectionArr.optJSONObject(i)
                        if (obj != null) {
                            sectionList.add(SectionCheck(
                                sectionName = obj.optString("sectionName", ""),
                                status = obj.optString("status", ""),
                                recommendation = obj.optString("recommendation", "")
                            ))
                        }
                    }
                }

                val contactList = mutableListOf<ContactCheck>()
                val contactArr = json.optJSONArray("contactIssues")
                if (contactArr != null) {
                    for (i in 0 until contactArr.length()) {
                        val obj = contactArr.optJSONObject(i)
                        if (obj != null) {
                            contactList.add(ContactCheck(
                                item = obj.optString("item", ""),
                                status = obj.optString("status", ""),
                                recommendation = obj.optString("recommendation", "")
                            ))
                        }
                    }
                }

                val devObj = json.optJSONObject("developerMetrics")
                val devMetrics = if (devObj != null) {
                    DeveloperMetrics(
                        githubScore = devObj.optInt("githubScore", 50),
                        projectsScore = devObj.optInt("projectsScore", 50),
                        techStackMatch = devObj.optString("techStackMatch", "Unknown"),
                        feedback = devObj.optString("feedback", "")
                    )
                } else null

                val gapList = mutableListOf<SkillGap>()
                val gapArr = json.optJSONArray("skillGapAnalysis")
                if (gapArr != null) {
                    for (i in 0 until gapArr.length()) {
                        val obj = gapArr.optJSONObject(i)
                        if (obj != null) {
                            val skills = mutableListOf<String>()
                            val skillsArr = obj.optJSONArray("missingSkills")
                            if (skillsArr != null) {
                                for (j in 0 until skillsArr.length()) {
                                    skills.add(skillsArr.optString(j, ""))
                                }
                            }
                            gapList.add(SkillGap(
                                role = obj.optString("role", ""),
                                missingSkills = skills,
                                recommendation = obj.optString("recommendation", "")
                            ))
                        }
                    }
                }

                val salObj = json.optJSONObject("salaryEstimation")
                val salaryEst = if (salObj != null) {
                    SalaryEstimation(
                        rangeMin = salObj.optString("rangeMin", "N/A"),
                        rangeMax = salObj.optString("rangeMax", "N/A"),
                        currency = salObj.optString("currency", "USD"),
                        marketContext = salObj.optString("marketContext", "")
                    )
                } else SalaryEstimation()

                return AnalysisResult(
                    overallScore = json.optInt("overallScore", 60),
                    atsScore = json.optInt("atsScore", 60),
                    hrScore = json.optInt("hrScore", 60),
                    keywordMatch = json.optInt("keywordMatch", 50),
                    grammarScore = json.optInt("grammarScore", 70),
                    formattingScore = json.optInt("formattingScore", 65),
                    readabilityScore = json.optInt("readabilityScore", 70),
                    roast = json.optString("roast", "No roast available."),
                    atsFeedback = json.optString("atsFeedback", "No ATS feedback available."),
                    hrFeedback = json.optString("hrFeedback", "No HR feedback available."),
                    grammarIssues = grammarList,
                    impactUpdates = impactList,
                    sectionAnalysis = sectionList,
                    contactIssues = contactList,
                    developerMetrics = devMetrics,
                    skillGapAnalysis = gapList,
                    salaryEstimation = salaryEst
                )
            } catch (e: Exception) {
                Log.e("AnalysisResult", "Error parsing json", e)
                // Fallback structured analysis
                return AnalysisResult(
                    overallScore = 50,
                    roast = "Failed to parse AI roast JSON cleanly. It might have been formatted incorrectly by the model. Error: ${e.localizedMessage}. Original: $jsonStr"
                )
            }
        }
    }
}

data class GrammarIssue(
    val original: String,
    val suggestion: String,
    val explanation: String
)

data class ImpactUpdate(
    val original: String,
    val replacement: String
)

data class SectionCheck(
    val sectionName: String,
    val status: String,
    val recommendation: String
)

data class ContactCheck(
    val item: String,
    val status: String,
    val recommendation: String
)

data class DeveloperMetrics(
    val githubScore: Int,
    val projectsScore: Int,
    val techStackMatch: String,
    val feedback: String
)

data class SkillGap(
    val role: String,
    val missingSkills: List<String>,
    val recommendation: String
)

data class SalaryEstimation(
    val rangeMin: String = "N/A",
    val rangeMax: String = "N/A",
    val currency: String = "USD",
    val marketContext: String = ""
)
