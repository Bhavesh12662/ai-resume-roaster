package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ToneFriendly
import com.example.ui.theme.ToneSavage
import com.example.ui.viewmodel.ResumeRoasterViewModel
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ToolsScreen(
    viewModel: ResumeRoasterViewModel,
    modifier: Modifier = Modifier
) {
    val resumeInput by viewModel.resumeInput.collectAsStateWithLifecycle()
    val jdInput by viewModel.jdInput.collectAsStateWithLifecycle()
    val isMatchingJd by viewModel.isMatchingJd.collectAsStateWithLifecycle()
    val jdMatchResult by viewModel.jdMatchResult.collectAsStateWithLifecycle()

    val isGeneratingTool by viewModel.isGeneratingTool.collectAsStateWithLifecycle()
    val coverLetterResult by viewModel.coverLetterResult.collectAsStateWithLifecycle()
    val interviewPrepResult by viewModel.interviewPrepResult.collectAsStateWithLifecycle()
    val roadmapResult by viewModel.roadmapResult.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Screen tab selection (0: JD Matcher, 1: Cover Letter, 2: Interview Prep, 3: 90-Day Roadmap)
    var selectedToolTab by remember { mutableStateOf(0) }

    // Cover letter input parameters
    var companyName by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("") }

    // Custom interview target role
    var interviewRole by remember { mutableStateOf("") }

    // Custom roadmap target role
    var roadmapRole by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Horizontal tool selectors
            ScrollableTabRow(
                selectedTabIndex = selectedToolTab,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedToolTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(selected = selectedToolTab == 0, onClick = { selectedToolTab = 0 }) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Compare, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("JD Matcher", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = selectedToolTab == 1, onClick = { selectedToolTab = 1 }) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Mail, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Cover Letter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = selectedToolTab == 2, onClick = { selectedToolTab = 2 }) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.ContactSupport, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Interview Prep", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = selectedToolTab == 3, onClick = { selectedToolTab = 3 }) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Map, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("90-Day Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (resumeInput.trim().isEmpty()) {
                // Warning
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Resume Missing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "These specialized tools generate content by examining your resume. Please enter or paste your resume in the 'Home' tab first.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (selectedToolTab) {
                        0 -> JDMatcherTabContent(
                            jdInput = jdInput,
                            isMatching = isMatchingJd,
                            resultStr = jdMatchResult,
                            onJdChange = { viewModel.jdInput.value = it },
                            onMatchClick = { viewModel.matchJobDescription() },
                            clipboardManager = clipboardManager,
                            context = context
                        )
                        1 -> CoverLetterTabContent(
                            companyName = companyName,
                            role = targetRole,
                            isGenerating = isGeneratingTool,
                            resultStr = coverLetterResult,
                            onCompanyChange = { companyName = it },
                            onRoleChange = { targetRole = it },
                            onGenerateClick = { viewModel.generateCoverLetter(companyName, targetRole) },
                            clipboardManager = clipboardManager,
                            context = context
                        )
                        2 -> InterviewPrepTabContent(
                            role = interviewRole,
                            isGenerating = isGeneratingTool,
                            resultStr = interviewPrepResult,
                            onRoleChange = { interviewRole = it },
                            onGenerateClick = { viewModel.generateInterviewPrep(interviewRole) },
                            clipboardManager = clipboardManager,
                            context = context
                        )
                        3 -> RoadmapTabContent(
                            role = roadmapRole,
                            isGenerating = isGeneratingTool,
                            resultStr = roadmapResult,
                            onRoleChange = { roadmapRole = it },
                            onGenerateClick = { viewModel.generateCareerRoadmap(roadmapRole) },
                            clipboardManager = clipboardManager,
                            context = context
                        )
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

/**
 * Tab 1: JD Matcher Content
 */
@Composable
fun JDMatcherTabContent(
    jdInput: String,
    isMatching: Boolean,
    resultStr: String?,
    onJdChange: (String) -> Unit,
    onMatchClick: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Target Job Description Matcher 🚀",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Paste the official job listing text below. AI will score your alignment, detect missing keywords/skills, and recommend optimal formatting rewrites.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = jdInput,
                onValueChange = onJdChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                placeholder = {
                    Text(
                        "Paste full Job Description here... (e.g. requirements, responsibilities, tech stack needed...)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onMatchClick,
                enabled = jdInput.trim().isNotEmpty() && !isMatching,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isMatching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calculating Match...")
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp))
                        Text("CALCULATE MATCH %", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (resultStr != null) {
        Spacer(modifier = Modifier.height(16.dp))

        // Pre-parse the JSON safely before calling composable components
        val parseResult = remember(resultStr) {
            try {
                val json = JSONObject(resultStr)
                val matchPercentage = json.optInt("matchPercentage", 0)
                
                val keywordsList = mutableListOf<String>()
                val keywordsArr = json.optJSONArray("missingKeywords")
                if (keywordsArr != null) {
                    for (i in 0 until keywordsArr.length()) {
                        keywordsList.add(keywordsArr.optString(i, ""))
                    }
                }

                val skillsList = mutableListOf<String>()
                val skillsArr = json.optJSONArray("missingSkills")
                if (skillsArr != null) {
                    for (i in 0 until skillsArr.length()) {
                        skillsList.add(skillsArr.optString(i, ""))
                    }
                }

                val suggestionsList = mutableListOf<String>()
                val suggestionsArr = json.optJSONArray("suggestions")
                if (suggestionsArr != null) {
                    for (i in 0 until suggestionsArr.length()) {
                        suggestionsList.add(suggestionsArr.optString(i, ""))
                    }
                }
                
                JdMatchParsedResult(matchPercentage, keywordsList, skillsList, suggestionsList, null)
            } catch (error: Exception) {
                JdMatchParsedResult(0, emptyList(), emptyList(), emptyList(), error)
            }
        }

        val matchPercentage = parseResult.matchPercentage
        val keywordsList = parseResult.keywords
        val skillsList = parseResult.skills
        val suggestionsList = parseResult.suggestions
        val parseError = parseResult.error

        if (parseError != null) {
            // Raw display if not JSON
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Result:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = resultStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Analysis Result", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (matchPercentage >= 75) ToneFriendly.copy(alpha = 0.12f) else ToneSavage.copy(alpha = 0.12f),
                                    RoundedCornerShape(100.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Match: $matchPercentage%",
                                color = if (matchPercentage >= 75) ToneFriendly else ToneSavage,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Missing Keywords (Add these to rank high):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (keywordsList.isEmpty()) "None! Great keyword coverage." else keywordsList.joinToString(", "),
                        color = ToneSavage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Missing Skills:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (skillsList.isEmpty()) "None!" else skillsList.joinToString(", "),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Improvement Suggestions:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    suggestionsList.forEach { sugg ->
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = sugg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Cover Letter Content
 */
@Composable
fun CoverLetterTabContent(
    companyName: String,
    role: String,
    isGenerating: Boolean,
    resultStr: String?,
    onCompanyChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onGenerateClick: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AI Cover Letter Writer ✉️",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Input the company name and job role. AI will match your resume achievements with the targeted details to draft a premium cover letter.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = companyName,
                onValueChange = onCompanyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Company Name") },
                placeholder = { Text("e.g. Google, Stripe, Local Startup...") },
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = role,
                onValueChange = onRoleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Target Job Role") },
                placeholder = { Text("e.g. Junior Android Developer, Product Manager...") },
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGenerateClick,
                enabled = companyName.trim().isNotEmpty() && role.trim().isNotEmpty() && !isGenerating,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Drafting Letter...")
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.BorderColor, null, modifier = Modifier.size(18.dp))
                        Text("GENERATE COVER LETTER", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (resultStr != null) {
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Custom Draft", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(resultStr))
                            Toast.makeText(context, "Cover Letter copied!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = resultStr,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Tab 3: Interview Prep Content
 */
@Composable
fun InterviewPrepTabContent(
    role: String,
    isGenerating: Boolean,
    resultStr: String?,
    onRoleChange: (String) -> Unit,
    onGenerateClick: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Interview Preparation Coach 🧠",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter your target job role. AI will generate specialized interview questions (Easy, Medium, Hard) tailored directly to your resume and provide perfect answer scripts.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = role,
                onValueChange = onRoleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Target Job Role") },
                placeholder = { Text("e.g. Android Engineer, Backend Dev, Project Lead...") },
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGenerateClick,
                enabled = role.trim().isNotEmpty() && !isGenerating,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Coaching...")
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.School, null, modifier = Modifier.size(18.dp))
                        Text("PREPARE ME FOR INTERVIEW", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (resultStr != null) {
        Spacer(modifier = Modifier.height(16.dp))

        val parseResult = remember(resultStr) {
            try {
                val json = JSONObject(resultStr)
                val arr = json.optJSONArray("questions") ?: JSONArray()
                val questionsList = mutableListOf<Triple<String, String, String>>()
                for (i in 0 until arr.length()) {
                    val qObj = arr.optJSONObject(i)
                    if (qObj != null) {
                        questionsList.add(
                            Triple(
                                qObj.optString("question", ""),
                                qObj.optString("difficulty", "Medium"),
                                qObj.optString("sampleAnswer", "")
                            )
                        )
                    }
                }
                InterviewPrepParsedResult(questionsList, null)
            } catch (error: Exception) {
                InterviewPrepParsedResult(emptyList(), error)
            }
        }

        val questionsList = parseResult.questions
        val parseError = parseResult.error

        if (parseError != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(text = resultStr, modifier = Modifier.padding(16.dp), fontSize = 12.sp)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Custom Questions for $role",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                questionsList.forEachIndexed { i, qInfo ->
                    val question = qInfo.first
                    val difficulty = qInfo.second
                    val sampleAnswer = qInfo.third

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Q${i+1}. Difficulty: $difficulty",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = when (difficulty) {
                                        "Easy" -> ToneFriendly
                                        "Hard" -> ToneSavage
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString("$question\n\nPerfect Answer: $sampleAnswer"))
                                        Toast.makeText(context, "Copied question & answer!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(text = question, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(text = "Perfect Answer Script:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = sampleAnswer,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 4: 90-Day Roadmap Content
 */
@Composable
fun RoadmapTabContent(
    role: String,
    isGenerating: Boolean,
    resultStr: String?,
    onRoleChange: (String) -> Unit,
    onGenerateClick: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "90-Day Up-Skilling Roadmap 🗺️",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter your target career goal. AI will design a step-by-step 90-day learning curriculum, recommend top online credentials, and propose a high-value bridging project.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = role,
                onValueChange = onRoleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Target Career Goal") },
                placeholder = { Text("e.g. AI Engineer, Full Stack Engineer, Tech Lead...") },
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGenerateClick,
                enabled = role.trim().isNotEmpty() && !isGenerating,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Charting Path...")
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                        Text("GENERATE ROADMAP", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (resultStr != null) {
        Spacer(modifier = Modifier.height(16.dp))

        val parseResult = remember(resultStr) {
            try {
                val json = JSONObject(resultStr)
                val planArr = json.optJSONArray("plan90Days") ?: JSONArray()
                val coursesArr = json.optJSONArray("recommendedCourses") ?: JSONArray()
                val certsArr = json.optJSONArray("recommendedCertifications") ?: JSONArray()
                val suggestedProject = json.optString("suggestedProject", "")

                val planList = mutableListOf<String>()
                for (i in 0 until planArr.length()) {
                    planList.add(planArr.optString(i, ""))
                }

                val coursesList = mutableListOf<String>()
                for (i in 0 until coursesArr.length()) {
                    coursesList.add(coursesArr.optString(i, ""))
                }

                val certsList = mutableListOf<String>()
                for (i in 0 until certsArr.length()) {
                    certsList.add(certsArr.optString(i, ""))
                }

                RoadmapParsedResult(planList, coursesList, certsList, suggestedProject, null)
            } catch (error: Exception) {
                RoadmapParsedResult(emptyList(), emptyList(), emptyList(), "", error)
            }
        }

        val planList = parseResult.plan
        val coursesList = parseResult.courses
        val certsList = parseResult.certs
        val suggestedProject = parseResult.suggestedProject
        val parseError = parseResult.error

        if (parseError != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(text = resultStr, modifier = Modifier.padding(16.dp), fontSize = 12.sp)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Timeline
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary)
                            Text("90-Day Step Timeline", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        planList.forEachIndexed { i, step ->
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "${i+1}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = step,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Courses & Certs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Courses
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Recommended Topics", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            coursesList.forEach { c ->
                                Text(text = "• $c", fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp), lineHeight = 14.sp)
                            }
                        }
                    }

                    // Certifications
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Recommended Certs", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            certsList.forEach { cert ->
                                Text(text = "🏆 $cert", fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp), lineHeight = 14.sp)
                            }
                        }
                    }
                }

                // suggested portfolio project
                if (suggestedProject.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Build, null, tint = ToneFriendly)
                                Text("Suggested Bridging Project", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = suggestedProject,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Parsed results helper data classes to avoid try-catch inside Composable and fix type inference
data class JdMatchParsedResult(
    val matchPercentage: Int,
    val keywords: List<String>,
    val skills: List<String>,
    val suggestions: List<String>,
    val error: Throwable?
)

data class InterviewPrepParsedResult(
    val questions: List<Triple<String, String, String>>,
    val error: Throwable?
)

data class RoadmapParsedResult(
    val plan: List<String>,
    val courses: List<String>,
    val certs: List<String>,
    val suggestedProject: String,
    val error: Throwable?
)
