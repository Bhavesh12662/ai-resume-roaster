package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.api.GeminiClient
import com.example.util.ResumeMetadata
import com.example.util.ResumeParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ResumeRoasterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ResumeAnalysisRepository
    private val profileRepository: ProfileAndCareerRepository

    val userProfile: StateFlow<UserProfileEntity?>
    val jobApplications: StateFlow<List<JobApplicationEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ResumeAnalysisRepository(database.resumeAnalysisDao())
        profileRepository = ProfileAndCareerRepository(database.profileAndCareerDao())

        userProfile = profileRepository.userProfile
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

        jobApplications = profileRepository.allJobApplications
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Pre-populate with beautiful default data on first app launch
        viewModelScope.launch {
            profileRepository.userProfile.take(1).collect { current ->
                if (current == null) {
                    profileRepository.insertProfile(UserProfileEntity())
                    // Pre-populate some dummy job applications as well for a richer visual dashboard
                    profileRepository.insertJobApplication(JobApplicationEntity(companyName = "Google", jobTitle = "Senior Android Developer", location = "Mountain View, CA", appliedDate = "2026-06-20", status = "Interview Scheduled"))
                    profileRepository.insertJobApplication(JobApplicationEntity(companyName = "Meta", jobTitle = "UI Engineer", location = "Menlo Park, CA", appliedDate = "2026-06-15", status = "Technical Round"))
                    profileRepository.insertJobApplication(JobApplicationEntity(companyName = "Stripe", jobTitle = "Full Stack Engineer", location = "Remote", appliedDate = "2026-06-10", status = "Offer Received"))
                    profileRepository.insertJobApplication(JobApplicationEntity(companyName = "Netflix", jobTitle = "Mobile Core Architect", location = "Los Gatos, CA", appliedDate = "2026-06-05", status = "HR Round"))
                }
            }
        }
    }

    // UI Input State
    val resumeInput = MutableStateFlow("")
    val selectedTone = MutableStateFlow("Savage") // Friendly, Professional, Savage
    val selectedFocus = MutableStateFlow("Tech/Developer Focus") // General Career, Tech/Developer Focus
    
    // Resume Upload & Extraction State
    private val _uploadedFileName = MutableStateFlow<String?>(null)
    val uploadedFileName: StateFlow<String?> = _uploadedFileName.asStateFlow()

    private val _uploadedFileSize = MutableStateFlow<String?>(null)
    val uploadedFileSize: StateFlow<String?> = _uploadedFileSize.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadSuccessMessage = MutableStateFlow<String?>(null)
    val uploadSuccessMessage: StateFlow<String?> = _uploadSuccessMessage.asStateFlow()

    private val _isEditExtractedTextEnabled = MutableStateFlow(false)
    val isEditExtractedTextEnabled: StateFlow<Boolean> = _isEditExtractedTextEnabled.asStateFlow()

    private val _parsedResumeMetadata = MutableStateFlow<ResumeMetadata?>(null)
    val parsedResumeMetadata: StateFlow<ResumeMetadata?> = _parsedResumeMetadata.asStateFlow()

    // API Status State
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult: StateFlow<AnalysisResult?> = _analysisResult.asStateFlow()

    // History data
    val historyList: StateFlow<List<ResumeAnalysisEntity>> = repository.allAnalyses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Job matching states
    val jdInput = MutableStateFlow("")
    private val _isMatchingJd = MutableStateFlow(false)
    val isMatchingJd: StateFlow<Boolean> = _isMatchingJd.asStateFlow()
    private val _jdMatchResult = MutableStateFlow<String?>(null)
    val jdMatchResult: StateFlow<String?> = _jdMatchResult.asStateFlow()

    // Tool states (Cover letter, Prep, Roadmap)
    private val _isGeneratingTool = MutableStateFlow(false)
    val isGeneratingTool: StateFlow<Boolean> = _isGeneratingTool.asStateFlow()

    private val _coverLetterResult = MutableStateFlow<String?>(null)
    val coverLetterResult: StateFlow<String?> = _coverLetterResult.asStateFlow()

    private val _interviewPrepResult = MutableStateFlow<String?>(null)
    val interviewPrepResult: StateFlow<String?> = _interviewPrepResult.asStateFlow()

    private val _roadmapResult = MutableStateFlow<String?>(null)
    val roadmapResult: StateFlow<String?> = _roadmapResult.asStateFlow()

    // Loading messages to cycle through for funny visual feedback
    val loadingMessages = listOf(
        "Reading Resume...",
        "Extracting Text...",
        "Checking ATS Score...",
        "Measuring corporate buzzword density...",
        "Analyzing Skills...",
        "Locating the 'Responsible for' copy-paste lines...",
        "Reviewing Grammar...",
        "Fanning the flames for the roast generator...",
        "Generating Suggestions...",
        "Calibrating HR critical eye...",
        "Almost Done..."
    )
    private val _currentLoadingMessage = MutableStateFlow(loadingMessages[0])
    val currentLoadingMessage: StateFlow<String> = _currentLoadingMessage.asStateFlow()

    private var loadingJob: kotlinx.coroutines.Job? = null

    // For Demo Resumes selection
    fun applyDemoResume(demo: DemoResumeItem) {
        removeUploadedFile()
        resumeInput.value = demo.content
        _parsedResumeMetadata.value = ResumeParser.parseMetadata(demo.content, 1)
    }

    // Toggle simulation mode in case of missing API key or demo testing
    val simulationMode = MutableStateFlow(false)

    fun handleResumeFileUpload(uri: android.net.Uri, fileName: String, fileSizeStr: String) {
        _errorMessage.value = null
        _uploadSuccessMessage.value = null
        _isUploading.value = true
        _uploadedFileName.value = fileName
        _uploadedFileSize.value = fileSizeStr

        viewModelScope.launch {
            try {
                // Read from uri & extract text and page count using js-based pdf.js and mammoth.js
                val (extractedText, pageCount) = com.example.util.JsFileProcessor.extractText(getApplication(), uri, fileName)
                
                // Clean unnecessary formatting (double precaution)
                val cleanedText = extractedText
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .replace(Regex("\n{3,}"), "\n\n")
                    .trim()

                resumeInput.value = cleanedText

                // Parse Metadata / Missing checklist / completeness
                val metadata = ResumeParser.parseMetadata(cleanedText, pageCount)
                _parsedResumeMetadata.value = metadata

                _uploadSuccessMessage.value = "✅ Resume Uploaded Successfully"
            } catch (e: Exception) {
                _uploadedFileName.value = null
                _uploadedFileSize.value = null
                _parsedResumeMetadata.value = null
                _errorMessage.value = e.localizedMessage ?: "Failed to parse file"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun removeUploadedFile() {
        _uploadedFileName.value = null
        _uploadedFileSize.value = null
        _uploadSuccessMessage.value = null
        _parsedResumeMetadata.value = null
        _isEditExtractedTextEnabled.value = false
        resumeInput.value = ""
        _errorMessage.value = null
    }

    fun toggleEditExtractedText(enabled: Boolean) {
        _isEditExtractedTextEnabled.value = enabled
    }
    
    fun updateExtractedText(newText: String) {
        resumeInput.value = newText
        // Re-calculate metadata dynamically
        val pageCount = _parsedResumeMetadata.value?.pageCount ?: 1
        _parsedResumeMetadata.value = ResumeParser.parseMetadata(newText, pageCount)
    }

    fun startLoadingMessageCycle() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            var index = 0
            while (true) {
                _currentLoadingMessage.value = loadingMessages[index]
                kotlinx.coroutines.delay(2500)
                index = (index + 1) % loadingMessages.size
            }
        }
    }

    fun stopLoadingMessageCycle() {
        loadingJob?.cancel()
    }

    /**
     * Call Gemini to analyze the resume text
     */
    fun analyzeResumeText(onSuccess: () -> Unit = {}) {
        val text = resumeInput.value.trim()
        if (text.isEmpty()) {
            _errorMessage.value = "Please enter some resume text or choose a pre-loaded Demo Resume above."
            return
        }

        _errorMessage.value = null
        _isAnalyzing.value = true
        _analysisResult.value = null
        startLoadingMessageCycle()

        viewModelScope.launch {
            try {
                if (simulationMode.value) {
                    // Simulated response
                    kotlinx.coroutines.delay(3500)
                    val mockResult = getMockAnalysisResult(selectedTone.value, selectedFocus.value)
                    _analysisResult.value = mockResult
                    saveResultToHistory(text, mockResult)
                    onSuccess()
                } else {
                    val rawResult = GeminiClient.analyzeResume(
                        resumeText = text,
                        tone = selectedTone.value,
                        focusMode = selectedFocus.value
                    )
                    
                    if (rawResult.contains("\"error\":")) {
                        // Check if key is missing and fall back to letting them know or turning on Simulation Mode
                        val jsonObj = JSONObject(rawResult)
                        val errMsg = jsonObj.optString("error")
                        _errorMessage.value = errMsg
                        if (errMsg.contains("Gemini API key")) {
                            // Offer enabling simulation
                            _errorMessage.value = "Gemini API Key is not configured. We activated 'Demo Simulation Mode' so you can preview the complete AI Resume Roaster metrics instantly!"
                            simulationMode.value = true
                            // Re-run as simulation
                            val mockResult = getMockAnalysisResult(selectedTone.value, selectedFocus.value)
                            _analysisResult.value = mockResult
                            saveResultToHistory(text, mockResult)
                            onSuccess()
                        }
                    } else {
                        val parsedResult = AnalysisResult.fromJson(rawResult)
                        _analysisResult.value = parsedResult
                        saveResultToHistory(text, parsedResult)
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Analysis failed", e)
                _errorMessage.value = "Failed to run analysis: ${e.localizedMessage}"
            } finally {
                _isAnalyzing.value = false
                stopLoadingMessageCycle()
            }
        }
    }

    private suspend fun saveResultToHistory(originalText: String, result: AnalysisResult) {
        // Extract a title from the resume text (e.g., first non-empty line)
        val lines = originalText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val title = lines.firstOrNull() ?: "Resume Analysis"
        
        val truncatedText = if (originalText.length > 2000) originalText.take(2000) + "..." else originalText

        // Serialize a simplified metadata or use result fields
        val entity = ResumeAnalysisEntity(
            title = title,
            tone = selectedTone.value,
            overallScore = result.overallScore,
            atsScore = result.atsScore,
            hrScore = result.hrScore,
            resultJson = JSONObject().apply {
                put("overallScore", result.overallScore)
                put("atsScore", result.atsScore)
                put("hrScore", result.hrScore)
                put("keywordMatch", result.keywordMatch)
                put("grammarScore", result.grammarScore)
                put("formattingScore", result.formattingScore)
                put("readabilityScore", result.readabilityScore)
                put("roast", result.roast)
                put("atsFeedback", result.atsFeedback)
                put("hrFeedback", result.hrFeedback)
                
                // Pack arrays
                put("grammarIssues", result.grammarIssues.toJsonArray())
                put("impactUpdates", result.impactUpdates.toJsonArray())
                put("sectionAnalysis", result.sectionAnalysis.toJsonArray())
                put("contactIssues", result.contactIssues.toJsonArray())
                if (result.developerMetrics != null) {
                    put("developerMetrics", JSONObject().apply {
                        put("githubScore", result.developerMetrics.githubScore)
                        put("projectsScore", result.developerMetrics.projectsScore)
                        put("techStackMatch", result.developerMetrics.techStackMatch)
                        put("feedback", result.developerMetrics.feedback)
                    })
                }
                put("skillGapAnalysis", result.skillGapAnalysis.toJsonArray())
                put("salaryEstimation", JSONObject().apply {
                    put("rangeMin", result.salaryEstimation.rangeMin)
                    put("rangeMax", result.salaryEstimation.rangeMax)
                    put("currency", result.salaryEstimation.currency)
                    put("marketContext", result.salaryEstimation.marketContext)
                })
            }.toString()
        )
        repository.insertAnalysis(entity)
    }

    fun loadAnalysisFromHistory(entity: ResumeAnalysisEntity) {
        _analysisResult.value = parseSavedResultJson(entity.resultJson)
        selectedTone.value = entity.tone
    }

    fun deleteHistoryItem(entity: ResumeAnalysisEntity) {
        viewModelScope.launch {
            repository.deleteAnalysis(entity)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    /**
     * Tool: Match Job Description
     */
    fun matchJobDescription() {
        val resumeText = resumeInput.value.trim()
        val jdText = jdInput.value.trim()
        if (resumeText.isEmpty() || jdText.isEmpty()) {
            _jdMatchResult.value = "Please ensure both your resume and the job description are entered."
            return
        }

        _isMatchingJd.value = true
        _jdMatchResult.value = null

        viewModelScope.launch {
            try {
                if (simulationMode.value) {
                    kotlinx.coroutines.delay(2000)
                    _jdMatchResult.value = """
                        {
                          "matchPercentage": 78,
                          "missingKeywords": ["Docker", "Kubernetes", "CI/CD Pipeline", "Unit Testing"],
                          "missingSkills": ["Cloud Architecture (AWS/GCP)", "Automated Testing Frameworks"],
                          "suggestions": [
                            "Integrate more metrics demonstrating scalability. For instance, describe backend project throughput using transactions per second.",
                            "Add a dedicated 'DevOps/Infrastructure' section in skills listing Docker or CI/CD to grab ATS scanners.",
                            "Replace passive bullets in experience with result-driven phrases."
                          ]
                        }
                    """.trimIndent()
                } else {
                    val rawMatch = GeminiClient.matchJobDescription(resumeText, jdText)
                    _jdMatchResult.value = rawMatch
                }
            } catch (e: Exception) {
                _jdMatchResult.value = "Failed: ${e.localizedMessage}"
            } finally {
                _isMatchingJd.value = false
            }
        }
    }

    /**
     * Tool: On-demand Cover Letter
     */
    fun generateCoverLetter(companyName: String, role: String) {
        val resumeText = resumeInput.value.trim()
        if (resumeText.isEmpty()) {
            _coverLetterResult.value = "Please enter your resume text on the Home page first."
            return
        }

        _isGeneratingTool.value = true
        _coverLetterResult.value = null

        viewModelScope.launch {
            try {
                if (simulationMode.value) {
                    kotlinx.coroutines.delay(2000)
                    _coverLetterResult.value = """
                        Dear Hiring Team at $companyName,

                        I am writing to express my enthusiastic interest in the $role position. With over 3 years of hands-on technical software development experience and a passion for engineering highly performant systems, I am confident in my ability to make an immediate, positive impact on your engineering division.

                        In my previous projects, I specialized in designing responsive web applications, developing robust REST APIs, and automating deployment pipelines. I am particularly drawn to $companyName because of your commitment to pioneering state-of-the-art consumer tools. I am excited by the prospect of contributing my JavaScript, React, and server optimization skills to your talented backend teams.

                        Thank you for your time and consideration. I welcome the opportunity to discuss how my qualifications align with your strategic roadmap.

                        Warm regards,
                        [Your Name]
                    """.trimIndent()
                } else {
                    val result = GeminiClient.generateCoverLetter(
                        resumeText = resumeText,
                        jdText = jdInput.value.ifEmpty { "General software development position" },
                        companyName = companyName,
                        role = role
                    )
                    _coverLetterResult.value = result
                }
            } catch (e: Exception) {
                _coverLetterResult.value = "Failed: ${e.localizedMessage}"
            } finally {
                _isGeneratingTool.value = false
            }
        }
    }

    /**
     * Tool: On-demand Interview Preparation Q&As
     */
    fun generateInterviewPrep(targetRole: String) {
        val resumeText = resumeInput.value.trim()
        if (resumeText.isEmpty()) {
            _interviewPrepResult.value = "Please enter your resume text on the Home page first."
            return
        }

        _isGeneratingTool.value = true
        _interviewPrepResult.value = null

        viewModelScope.launch {
            try {
                if (simulationMode.value) {
                    kotlinx.coroutines.delay(2000)
                    _interviewPrepResult.value = """
                        {
                          "questions": [
                            {
                              "question": "Can you explain a complex project you developed and the business value it brought?",
                              "difficulty": "Easy",
                              "sampleAnswer": "Talk about your e-commerce platform or todo app. Detail the technology stack chosen (React, Node.js) and focus on the user outcomes—e.g., how you enabled users to check weather dynamically or complete orders without page refreshes."
                            },
                            {
                              "question": "How do you handle database optimization, and what strategies do you apply when scaling tables?",
                              "difficulty": "Medium",
                              "sampleAnswer": "Reference PostgreSQL. Explain that database scaling begins with proper query indexing, analyzing slow queries using EXPLAIN ANALYZE, and caching highly requested static keys using Redis to decrease database lookup traffic."
                            },
                            {
                              "question": "Describe a time when you had to resolve a severe production bug under pressure. How did you diagnose it?",
                              "difficulty": "Hard",
                              "sampleAnswer": "A professional answer outlines: 1) Mitigating customer impact first, 2) Examining application logs (such as Kibana or CloudWatch) to find the stack trace, 3) Writing a local unit test replicating the issue, 4) Applying the fix, and 5) Creating post-mortem monitors to prevent a recurrence."
                            }
                          ]
                        }
                    """.trimIndent()
                } else {
                    val result = GeminiClient.generateInterviewPrep(resumeText, targetRole)
                    _interviewPrepResult.value = result
                }
            } catch (e: Exception) {
                _interviewPrepResult.value = "Failed: ${e.localizedMessage}"
            } finally {
                _isGeneratingTool.value = false
            }
        }
    }

    /**
     * Tool: On-demand 90-Day Learning Roadmap
     */
    fun generateCareerRoadmap(targetRole: String) {
        val resumeText = resumeInput.value.trim()
        if (resumeText.isEmpty()) {
            _roadmapResult.value = "Please enter your resume text on the Home page first."
            return
        }

        _isGeneratingTool.value = true
        _roadmapResult.value = null

        viewModelScope.launch {
            try {
                if (simulationMode.value) {
                    kotlinx.coroutines.delay(2000)
                    _roadmapResult.value = """
                        {
                          "plan90Days": [
                            "Days 1-30: Build deep foundations in $targetRole core skills. Dedicate 2 hours daily to practicing systems architecture, writing clean OOP patterns, and understanding cloud fundamentals.",
                            "Days 31-60: Build a complete full-stack portfolio project. Integrate modern tools like Docker containers, Git workflows, CI/CD automated test pipelines, and SQL transactions.",
                            "Days 61-90: Focus on mock interviews, algorithmic problem solving (LeetCode patterns), and polishing your LinkedIn headline and bio using ATS keywords."
                          ],
                          "recommendedCourses": [
                            "System Design Fundamentals by bytebytego",
                            "Advanced Kotlin and Jetpack Compose on Google Developers"
                          ],
                          "recommendedCertifications": [
                            "AWS Certified Cloud Practitioner",
                            "Associate Android Developer by Google"
                          ],
                          "suggestedProject": "Create a high-performance web analytics engine that consumes server events asynchronously, processes them with SQL query aggregations, and visualizes stats in real-time."
                        }
                    """.trimIndent()
                } else {
                    val result = GeminiClient.generateCareerRoadmap(resumeText, targetRole)
                    _roadmapResult.value = result
                }
            } catch (e: Exception) {
                _roadmapResult.value = "Failed: ${e.localizedMessage}"
            } finally {
                _isGeneratingTool.value = false
            }
        }
    }

    /**
     * Helper list to JSON converter
     */
    private fun List<GrammarIssue>.toJsonArray() = JSONArray().apply {
        forEach { issue ->
            put(JSONObject().apply {
                put("original", issue.original)
                put("suggestion", issue.suggestion)
                put("explanation", issue.explanation)
            })
        }
    }

    @JvmName("impactUpdatesToJsonArray")
    private fun List<ImpactUpdate>.toJsonArray() = JSONArray().apply {
        forEach { update ->
            put(JSONObject().apply {
                put("original", update.original)
                put("replacement", update.replacement)
            })
        }
    }

    @JvmName("sectionAnalysisToJsonArray")
    private fun List<SectionCheck>.toJsonArray() = JSONArray().apply {
        forEach { check ->
            put(JSONObject().apply {
                put("sectionName", check.sectionName)
                put("status", check.status)
                put("recommendation", check.recommendation)
            })
        }
    }

    @JvmName("contactIssuesToJsonArray")
    private fun List<ContactCheck>.toJsonArray() = JSONArray().apply {
        forEach { check ->
            put(JSONObject().apply {
                put("item", check.item)
                put("status", check.status)
                put("recommendation", check.recommendation)
            })
        }
    }

    @JvmName("skillGapAnalysisToJsonArray")
    private fun List<SkillGap>.toJsonArray() = JSONArray().apply {
        forEach { gap ->
            put(JSONObject().apply {
                put("role", gap.role)
                put("missingSkills", JSONArray(gap.missingSkills))
                put("recommendation", gap.recommendation)
            })
        }
    }

    private fun parseSavedResultJson(jsonStr: String): AnalysisResult {
        return AnalysisResult.fromJson(jsonStr)
    }

    /**
     * Complete offline mock database to guarantee a beautiful experience even without network or keys
     */
    private fun getMockAnalysisResult(tone: String, focusMode: String): AnalysisResult {
        val roastMessage = when (tone) {
            "Savage" -> {
                "Savage Roast Mode is ACTIVE! 🔥\n\n" +
                "• Your 'Objective' statement reads like you copied it from a 2011 Quora forum. 'To utilize my skills in a reputable organization'? Tell me you have no personal goals without telling me.\n\n" +
                "• You listed 'MS Office' as a skill in 2026. What's next? Listing 'Opening Google Chrome' or 'Typing with two hands'? That is not a technical skill, it's a basic requirement of human survival.\n\n" +
                "• 'Responsible for maintaining codebases'. That is a passive confession of doing the absolute bare minimum! Did you actually build anything, or did you just watch the server spin and hope it didn't crash? Where are the actual achievements and metrics?\n\n" +
                "• You've got more empty spacing here than achievements. It looks like you formatted this resume with a hammer."
            }
            "Professional" -> {
                "Professional Recruiter Review 💼\n\n" +
                "• The objective section is outdated. Modern recruitment relies on a 3-sentence Executive Summary that focuses on high-impact value instead of vague personal desires.\n\n" +
                "• Skills density is low in technical specifications. Soft skills should be integrated directly into your bullet points rather than listed separately.\n\n" +
                "• Experience bullet points focus on tasks ('Responsible for') rather than business outcomes and key performance metrics. Recruiter attention drops significantly when measurable numbers are missing."
            }
            else -> {
                "Friendly Mentor Review 🌱\n\n" +
                "• Hey there! Your resume has a great foundational layout and shows you're motivated. However, we can make it pop more by turning your tasks into active achievements!\n\n" +
                "• Try replacing 'Responsible for' with active verbs like 'Designed', 'Orchestrated', or 'Optimized'. This shows strong initiative.\n\n" +
                "• Let's replace simple skills like 'MS Office' with deeper tools you learned, and highlight some metrics in your projects to show off your great results!"
            }
        }

        return AnalysisResult(
            overallScore = if (tone == "Savage") 54 else 78,
            atsScore = 65,
            hrScore = 70,
            keywordMatch = 55,
            grammarScore = 88,
            formattingScore = 60,
            readabilityScore = 75,
            roast = roastMessage,
            atsFeedback = """
                • **Font Compatibility**: Standard sans-serif detected. ATS readable.
                • **Multi-column Layout**: Found complex side columns which can confuse older ATS scanners. Stick to a clean single-column structure.
                • **Tables & Headers**: Hidden tables used for formatting detected. These can truncate text inside scanners.
                • **Empty Footers/Headers**: Avoid putting critical contact info inside Page Headers or Footers, as some ATS software skips reading them.
            """.trimIndent(),
            hrFeedback = """
                • **First Impression**: Layout feels standard but lacks professional flair. Needs a compelling Executive Summary.
                • **Clarity of Role**: It is difficult to immediately gauge your core stack. Highlight technical skills near the top.
                • **Strengths**: Solid educational background and clear graduation credentials.
                • **Weaknesses**: Bullet points describe daily tasks rather than quantifiable impacts or metrics.
            """.trimIndent(),
            grammarIssues = listOf(
                GrammarIssue("worked on web development", "engineered responsive web applications", "Stronger verb action choice."),
                GrammarIssue("responsible for backend", "orchestrated backend microservices", "Replaces weak passive structure.")
            ),
            impactUpdates = listOf(
                ImpactUpdate("Responsible for writing Java backend API codes.", "Engineered 15+ Java backend REST endpoints with Spring Boot, reducing API latency by 28%."),
                ImpactUpdate("Helped with PostgreSQL database and query issues.", "Optimized PostgreSQL SQL query indices and table structures, saving $3,200 in monthly cloud database performance costs.")
            ),
            sectionAnalysis = listOf(
                SectionCheck("Summary / Objective", "Exists", "Replace the vague objective statement with an actionable 3-sentence summary of your technical highlights."),
                SectionCheck("Projects", "Exists", "Include tech stack details for every project (e.g., React, Spring Boot, PostgreSQL) instead of just generic names."),
                SectionCheck("Certifications", "Missing", "Add industry-standard certifications (like Google Cloud, AWS, or Coursera) to validate self-taught skills.")
            ),
            contactIssues = listOf(
                ContactCheck("Email", "Valid", "Professional email address detected."),
                ContactCheck("GitHub", "Valid", "Found valid link. Ensure repositories have descriptive READMEs."),
                ContactCheck("LinkedIn", "Missing", "No LinkedIn profile found. 85% of tech recruiters check your profile first!")
            ),
            developerMetrics = DeveloperMetrics(
                githubScore = 60,
                projectsScore = 65,
                techStackMatch = "Fair",
                feedback = "GitHub link is present but needs pinned repositories and daily contribution greens. Projects show standard tutorial work; try building a unique, fully-deployed system."
            ),
            skillGapAnalysis = listOf(
                SkillGap("Frontend Developer", listOf("TypeScript", "Next.js", "Tailwind CSS"), "You have React foundations but lack enterprise tools like TypeScript and SSR architectures."),
                SkillGap("Backend Developer", listOf("Docker", "CI/CD (GitHub Actions)", "AWS / GCP"), "You have JVM skills but lack cloud deployment and container orchestration expertise.")
            ),
            salaryEstimation = SalaryEstimation(
                rangeMin = "${'$'}65,000",
                rangeMax = "${'$'}95,000",
                currency = "USD",
                marketContext = "Calculated for an Associate Engineer level. Adding cloud deployment skills or full stack metrics can raise this range by 15-20%."
            )
        )
    }

    // --- User Profile Actions ---
    fun saveUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            profileRepository.insertProfile(profile)
        }
    }

    fun resetUserProfileToDefault() {
        viewModelScope.launch {
            profileRepository.insertProfile(UserProfileEntity())
        }
    }

    fun deleteProfileHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteUserProfilePermanently() {
        viewModelScope.launch {
            profileRepository.insertProfile(UserProfileEntity(fullName = "", username = "", email = "", phone = "", currentJobTitle = "", experienceLevel = "", education = "", university = "", graduationYear = "", careerGoal = "", preferredJobRole = "", skills = "", programmingLanguages = "", frameworks = "", tools = "", certifications = "", languagesKnown = "", currentCompany = "", portfolioWebsite = "", linkedinProfile = "", githubProfile = "", leetcodeProfile = "", hackerrankProfile = ""))
            profileRepository.clearAllJobApplications()
            repository.clearAll()
        }
    }

    // --- Job Application Tracker Actions ---
    fun addJobApplication(company: String, title: String, location: String, date: String, status: String) {
        viewModelScope.launch {
            profileRepository.insertJobApplication(
                JobApplicationEntity(
                    companyName = company,
                    jobTitle = title,
                    location = location,
                    appliedDate = date,
                    status = status
                )
            )
        }
    }

    fun updateJobApplicationStatus(app: JobApplicationEntity, newStatus: String) {
        viewModelScope.launch {
            profileRepository.insertJobApplication(app.copy(status = newStatus))
        }
    }

    fun deleteJobApplication(app: JobApplicationEntity) {
        viewModelScope.launch {
            profileRepository.deleteJobApplication(app)
        }
    }

    fun clearJobApplications() {
        viewModelScope.launch {
            profileRepository.clearAllJobApplications()
        }
    }
}

class ResumeRoasterViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResumeRoasterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResumeRoasterViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
