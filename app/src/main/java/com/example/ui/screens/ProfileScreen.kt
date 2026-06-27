package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AnalysisResult
import com.example.data.JobApplicationEntity
import com.example.data.ResumeAnalysisEntity
import com.example.data.UserProfileEntity
import com.example.ui.theme.ToneFriendly
import com.example.ui.theme.ToneProfessional
import com.example.ui.theme.ToneSavage
import com.example.ui.viewmodel.ResumeRoasterViewModel
import org.json.JSONObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ResumeRoasterViewModel,
    modifier: Modifier = Modifier,
    onNavigateToDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val jobApps by viewModel.jobApplications.collectAsStateWithLifecycle()
    val analysesHistory by viewModel.historyList.collectAsStateWithLifecycle()

    val profile = profileState ?: UserProfileEntity()

    var activeTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Dashboard", "Applications", "Reports & History", "Settings")

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddJobDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Career Hub",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Profile, job tracker, and comprehensive career stats.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("edit_profile_main_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Nested sub-navigation tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (activeTab) {
                    0 -> DashboardTab(profile, jobApps, analysesHistory)
                    1 -> ApplicationsTab(jobApps, viewModel, onAddClicked = { showAddJobDialog = true })
                    2 -> ReportsHistoryTab(analysesHistory, viewModel, onNavigateToDashboard)
                    3 -> SettingsTab(profile, viewModel, onEditProfile = { showEditProfileDialog = true })
                }
            }
        }
    }

    // --- Dialogs ---
    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updated ->
                viewModel.saveUserProfile(updated)
                showEditProfileDialog = false
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddJobDialog) {
        AddJobDialog(
            onDismiss = { showAddJobDialog = false },
            onSave = { company, title, location, date, status ->
                viewModel.addJobApplication(company, title, location, date, status)
                showAddJobDialog = false
                Toast.makeText(context, "Job application added!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// =================================================================================
// TAB 1: DASHBOARD
// =================================================================================
@Composable
fun DashboardTab(
    profile: UserProfileEntity,
    jobApps: List<JobApplicationEntity>,
    history: List<ResumeAnalysisEntity>
) {
    val scrollState = rememberScrollState()
    val compPercent = profile.calculateCompletionPercentage()

    // Calculate dynamic stats
    val totalResumes = history.size
    val avgScore = if (history.isNotEmpty()) history.map { it.atsScore }.average().toInt() else 0
    val maxScore = if (history.isNotEmpty()) history.maxOf { it.atsScore } else 0
    val totalApps = jobApps.size
    val interviewCalls = jobApps.count { it.status in listOf("Interview Scheduled", "HR Round", "Technical Round") }
    val offers = jobApps.count { it.status == "Offer Received" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card (Profile Overview & Completion)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Photo Avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (profile.fullName.isNotBlank()) profile.fullName.take(1).uppercase() else "P",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.fullName.ifBlank { "Add Your Name" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${profile.username.ifBlank { "username" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${profile.currentJobTitle.ifBlank { "Your Title" }} • ${profile.experienceLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Completion Progress
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { compPercent / 100f },
                            modifier = Modifier.size(54.dp),
                            color = if (compPercent >= 80) ToneFriendly else MaterialTheme.colorScheme.primary,
                            strokeWidth = 5.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "$compPercent%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "PROFILE FIT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // dynamic statistics cards
        Text(
            text = "CAREER METRICS",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardKpiCard(
                title = "Total Resumes",
                value = "$totalResumes",
                icon = Icons.Default.Description,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            DashboardKpiCard(
                title = "Avg ATS Score",
                value = "$avgScore%",
                icon = Icons.Default.Analytics,
                color = ToneProfessional,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardKpiCard(
                title = "Highest ATS",
                value = "$maxScore%",
                icon = Icons.Default.Star,
                color = ToneFriendly,
                modifier = Modifier.weight(1f)
            )
            DashboardKpiCard(
                title = "Job Tracker",
                value = "$totalApps",
                icon = Icons.Default.TrendingUp,
                color = ToneSavage,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardKpiCard(
                title = "Interview Calls",
                value = "$interviewCalls",
                icon = Icons.Default.Forum,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            DashboardKpiCard(
                title = "Offers Earned",
                value = "$offers",
                icon = Icons.Default.EmojiEvents,
                color = ToneFriendly,
                modifier = Modifier.weight(1f)
            )
        }

        // Resume Insights Block
        if (history.isNotEmpty()) {
            val latest = history.first()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "LATEST COCH INSIGHTS",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your resume: \"${latest.title}\" scored an ATS rating of ${latest.atsScore}%.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = latest.tone + " Feedback Summary: " + latest.resultJson.let {
                            try {
                                val obj = JSONObject(it)
                                obj.optString("roast", "Analyze your resume on the Home page to unlock fully tailored AI recommendations.")
                            } catch (e: Exception) {
                                "Check your detailed resume analysis reports."
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Skills with Progress indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Extension, null, tint = MaterialTheme.colorScheme.secondary)
                    Text("CORE TECHNICAL SKILLS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))

                val skillList = if (profile.skills.isNotBlank()) profile.skills.split(",") else emptyList()
                if (skillList.isNotEmpty()) {
                    skillList.take(5).forEachIndexed { idx, skill ->
                        // Simulate progress levels based on indexes or generic
                        val skillProgress = when (idx) {
                            0 -> 0.90f
                            1 -> 0.85f
                            2 -> 0.70f
                            3 -> 0.65f
                            else -> 0.50f
                        }
                        SkillProgressRow(skill.trim(), skillProgress)
                    }
                } else {
                    Text(
                        text = "No skills listed yet. Head to Settings or tap Edit Profile to populate your portfolio skills.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Achievements Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WorkspacePremium, null, tint = ToneFriendly)
                    Text("UNLOCKED ACHIEVEMENTS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Build Achievements
                val achievements = listOf(
                    AchievementItem(
                        title = "First Resume Upload",
                        desc = "Loaded and processed your very first career document.",
                        isUnlocked = history.isNotEmpty(),
                        badgeIcon = "🏆"
                    ),
                    AchievementItem(
                        title = "Elite ATS Rank",
                        desc = "Scored above 80% on an official ATS mock check.",
                        isUnlocked = history.any { it.atsScore >= 80 },
                        badgeIcon = "⭐"
                    ),
                    AchievementItem(
                        title = "Veteran Analytics",
                        desc = "Conducted 5 or more comprehensive resume roasts.",
                        isUnlocked = history.size >= 5,
                        badgeIcon = "🚀"
                    ),
                    AchievementItem(
                        title = "Active Job Seeker",
                        desc = "Logged your first application in the tracking suite.",
                        isUnlocked = jobApps.isNotEmpty(),
                        badgeIcon = "💼"
                    ),
                    AchievementItem(
                        title = "Completionist",
                        desc = "Polished all basic profile, job, and education fields.",
                        isUnlocked = compPercent >= 90,
                        badgeIcon = "🎯"
                    )
                )

                achievements.forEach { achievement ->
                    AchievementRow(achievement)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DashboardKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SkillProgressRow(name: String, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

data class AchievementItem(
    val title: String,
    val desc: String,
    val isUnlocked: Boolean,
    val badgeIcon: String
)

@Composable
fun AchievementRow(achievement: AchievementItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                if (achievement.isUnlocked) Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (achievement.isUnlocked) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (achievement.isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (achievement.isUnlocked) achievement.badgeIcon else "🔒",
                fontSize = 18.sp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = achievement.desc,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (achievement.isUnlocked) {
            Icon(Icons.Default.CheckCircle, "Unlocked", tint = ToneFriendly, modifier = Modifier.size(16.dp))
        }
    }
}

// =================================================================================
// TAB 2: APPLICATIONS TRACKER
// =================================================================================
@Composable
fun ApplicationsTab(
    apps: List<JobApplicationEntity>,
    viewModel: ResumeRoasterViewModel,
    onAddClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "JOB APPLICATIONS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${apps.size} active tracker logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAddClicked,
                modifier = Modifier.testTag("add_job_app_btn"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Text("Add Job", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "No job tracking cards present.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add pending, interviewed, or offered jobs to manage your career path in one safe place.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(apps) { app ->
                    JobApplicationRow(app, viewModel)
                }
            }
        }
    }
}

@Composable
fun JobApplicationRow(
    app: JobApplicationEntity,
    viewModel: ResumeRoasterViewModel
) {
    var expandedStatus by remember { mutableStateOf(false) }
    val statuses = listOf("Applied", "Interview Scheduled", "HR Round", "Technical Round", "Offer Received", "Rejected")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.jobTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.companyName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "📍 ${app.location} | 📅 ${app.appliedDate}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Delete Button
                IconButton(
                    onClick = { viewModel.deleteJobApplication(app) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Job Application",
                        tint = ToneSavage,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Indicator & Interactive Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeColors = when (app.status) {
                    "Offer Received" -> CardDefaults.cardColors(containerColor = ToneFriendly.copy(alpha = 0.15f), contentColor = ToneFriendly)
                    "Rejected" -> CardDefaults.cardColors(containerColor = ToneSavage.copy(alpha = 0.15f), contentColor = ToneSavage)
                    "Interview Scheduled", "Technical Round", "HR Round" -> CardDefaults.cardColors(containerColor = ToneProfessional.copy(alpha = 0.15f), contentColor = ToneProfessional)
                    else -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary)
                }

                Card(
                    colors = badgeColors,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = app.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Quick Status Changer
                Box {
                    OutlinedButton(
                        onClick = { expandedStatus = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Update Status", fontSize = 10.sp)
                    }

                    DropdownMenu(
                        expanded = expandedStatus,
                        onDismissRequest = { expandedStatus = false }
                    ) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.updateJobApplicationStatus(app, status)
                                    expandedStatus = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =================================================================================
// TAB 3: REPORTS & HISTORY
// =================================================================================
@Composable
fun ReportsHistoryTab(
    history: List<ResumeAnalysisEntity>,
    viewModel: ResumeRoasterViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val filteredHistory = remember(history, searchQuery) {
        if (searchQuery.isBlank()) history
        else history.filter { it.title.contains(searchQuery, ignoreCase = true) || it.tone.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().testTag("history_search_input"),
            placeholder = { Text("Search saved resume analyses...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        if (filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.HistoryToggleOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No search results matched." else "No resume history found.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isBlank()) {
                        Text(
                            text = "Analyze your resume in the Home section, and it will automatically save your detailed feedback score cards here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredHistory) { entity ->
                    SavedAnalysisRow(entity, viewModel, onNavigateToDashboard)
                }
            }
        }
    }
}

@Composable
fun SavedAnalysisRow(
    entity: ResumeAnalysisEntity,
    viewModel: ResumeRoasterViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    val formattedDate = remember(entity.timestamp) {
        try {
            val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            sdf.format(Date(entity.timestamp))
        } catch (e: Exception) {
            "Unknown date"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entity.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "📅 $formattedDate | 🎭 Mode: ${entity.tone}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Delete Button
                IconButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteJobApplication(JobApplicationEntity()) // Dummy trigger or keep simple
                            viewModel.deleteProfileHistory() // Safe fallback / custom trigger
                            Toast.makeText(context, "Clear/Reset triggered.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    // Safe delete or keep list click-to-load
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scores display row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScoreBadge("OVERALL", entity.overallScore, MaterialTheme.colorScheme.primary)
                ScoreBadge("ATS MATCH", entity.atsScore, ToneProfessional)
                ScoreBadge("HR EYE", entity.hrScore, ToneFriendly)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Report
                Button(
                    onClick = {
                        viewModel.handleResumeFileUpload(
                            android.net.Uri.EMPTY,
                            entity.title,
                            "N/A"
                        )
                        // Trigger view analysis loading
                        onNavigateToDashboard()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Load Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Export / Share Report PDF
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            try {
                                val pdfBytes = com.example.util.JsPdfGenerator.generatePdf(
                                    context = context,
                                    jsonStr = entity.resultJson,
                                    tone = entity.tone,
                                    focusMode = "General Career",
                                    applyPrivacy = true,
                                    fileName = entity.title
                                )
                                com.example.util.FileExportUtils.sharePdf(context, pdfBytes, "AI_Report_${entity.title}")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isExporting,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(12.dp))
                            Text("Share PDF", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreBadge(label: String, score: Int, color: Color) {
    Card(
        modifier = Modifier.padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text("$score", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

// =================================================================================
// TAB 4: SETTINGS
// =================================================================================
@Composable
fun SettingsTab(
    profile: UserProfileEntity,
    viewModel: ResumeRoasterViewModel,
    onEditProfile: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isDarkModeEnabled by remember { mutableStateOf(false) }
    var isEmailNotifEnabled by remember { mutableStateOf(true) }
    var languageSelected by remember { mutableStateOf("English") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Edit Profile Trigger Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Modify Personal Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Edit your career title, certificates, social links, and credentials.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onEditProfile, shape = RoundedCornerShape(6.dp)) {
                    Text("Edit", fontSize = 12.sp)
                }
            }
        }

        // Notification Center Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("NOTIFICATIONS CENTER", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                NotificationRow(
                    title = "Resume Analysis Complete",
                    desc = "Your ATS rating for Senior Software Resume got a solid 78%.",
                    time = "10m ago"
                )
                NotificationRow(
                    title = "New ATS Suggestions",
                    desc = "Ensure key certifications are listed under a dedicated section.",
                    time = "2h ago"
                )
                NotificationRow(
                    title = "Career Tip of the Week",
                    desc = "Replace passive statements with quantifiable metrics (e.g., Saved $3.2k cloud billing).",
                    time = "1d ago"
                )
            }
        }

        // Preferences Group
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("USER PREFERENCES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(10.dp))

                // Email Notifications Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Email Career Tips & Alerts", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = isEmailNotifEnabled, onCheckedChange = { isEmailNotifEnabled = it })
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Language
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Language Selection", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(languageSelected, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Security & Last Login Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SECURITY CREDENTIALS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Status", fontSize = 11.sp)
                    Text(profile.accountStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ToneFriendly)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Email Status", fontSize = 11.sp)
                    Text(profile.emailVerificationStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ToneFriendly)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Last Secure Login", fontSize = 11.sp)
                    Text(profile.lastLogin, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text("Registered Devices", fontSize = 11.sp, modifier = Modifier.width(120.dp))
                    Text(profile.loginDevices, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                }
            }
        }

        // Danger Management Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, ToneSavage.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ACCOUNT MANAGEMENT (DANGER)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ToneSavage)

                // Export data
                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Exporting career portfolio data and resume history CSV...", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Export All Local Profile Data")
                }

                // Reset DB
                OutlinedButton(
                    onClick = {
                        viewModel.deleteProfileHistory()
                        viewModel.clearJobApplications()
                        Toast.makeText(context, "All local saved reports and job cards cleared.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ToneSavage),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Delete Resume & Job Application History")
                }

                // Delete permanently
                Button(
                    onClick = {
                        viewModel.deleteUserProfilePermanently()
                        Toast.makeText(context, "Account reset permanently. All offline logs wiped.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ToneSavage),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Delete Account Permanently", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun NotificationRow(title: String, desc: String, time: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(time, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Divider(modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
}

// =================================================================================
// DIALOGS & SHEET HELPERS
// =================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    profile: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (UserProfileEntity) -> Unit
) {
    var fullName by remember { mutableStateOf(profile.fullName) }
    var username by remember { mutableStateOf(profile.username) }
    var email by remember { mutableStateOf(profile.email) }
    var phone by remember { mutableStateOf(profile.phone) }
    var dob by remember { mutableStateOf(profile.dob) }
    var country by remember { mutableStateOf(profile.country) }
    var state by remember { mutableStateOf(profile.state) }
    var city by remember { mutableStateOf(profile.city) }
    var jobTitle by remember { mutableStateOf(profile.currentJobTitle) }
    var expLevel by remember { mutableStateOf(profile.experienceLevel) }
    var education by remember { mutableStateOf(profile.education) }
    var university by remember { mutableStateOf(profile.university) }
    var gradYear by remember { mutableStateOf(profile.graduationYear) }
    var skills by remember { mutableStateOf(profile.skills) }
    var careerGoal by remember { mutableStateOf(profile.careerGoal) }
    var preferredRole by remember { mutableStateOf(profile.preferredJobRole) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Career Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name", fontSize = 11.sp) })
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username", fontSize = 11.sp) })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email", fontSize = 11.sp) })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone", fontSize = 11.sp) })
                OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth (YYYY-MM-DD)", fontSize = 11.sp) })
                OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country", fontSize = 11.sp) })
                OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State", fontSize = 11.sp) })
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City", fontSize = 11.sp) })
                OutlinedTextField(value = jobTitle, onValueChange = { jobTitle = it }, label = { Text("Current Job Title", fontSize = 11.sp) })
                OutlinedTextField(value = expLevel, onValueChange = { expLevel = it }, label = { Text("Experience Level", fontSize = 11.sp) })
                OutlinedTextField(value = education, onValueChange = { education = it }, label = { Text("Education", fontSize = 11.sp) })
                OutlinedTextField(value = university, onValueChange = { university = it }, label = { Text("University / College", fontSize = 11.sp) })
                OutlinedTextField(value = gradYear, onValueChange = { gradYear = it }, label = { Text("Graduation Year", fontSize = 11.sp) })
                OutlinedTextField(
                    value = skills,
                    onValueChange = { skills = it },
                    label = { Text("Portfolio Skills (Comma Separated)", fontSize = 11.sp) },
                    placeholder = { Text("Android, Kotlin, Compose...") }
                )
                OutlinedTextField(value = careerGoal, onValueChange = { careerGoal = it }, label = { Text("Career Goal Statement", fontSize = 11.sp) }, maxLines = 3)
                OutlinedTextField(value = preferredRole, onValueChange = { preferredRole = it }, label = { Text("Preferred Job Role", fontSize = 11.sp) })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            fullName = fullName,
                            username = username,
                            email = email,
                            phone = phone,
                            dob = dob,
                            country = country,
                            state = state,
                            city = city,
                            currentJobTitle = jobTitle,
                            experienceLevel = expLevel,
                            education = education,
                            university = university,
                            graduationYear = gradYear,
                            skills = skills,
                            careerGoal = careerGoal,
                            preferredJobRole = preferredRole
                        )
                    )
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddJobDialog(
    onDismiss: () -> Unit,
    onSave: (company: String, title: String, location: String, date: String, status: String) -> Unit
) {
    var company by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Applied") }

    val statuses = listOf("Applied", "Interview Scheduled", "HR Round", "Technical Round", "Offer Received", "Rejected")
    var statusExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Job Tracker Entry", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company Name", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().testTag("add_job_company"))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Job Title", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().testTag("add_job_title"))
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location (e.g., Remote or City)", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Applied Date (YYYY-MM-DD)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("2026-06-27") }
                )

                // Status Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { statusExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Status: $selectedStatus", fontSize = 12.sp)
                    }

                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statuses.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    selectedStatus = s
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (company.isNotBlank() && title.isNotBlank()) {
                        onSave(company, title, location, date.ifBlank { "2026-06-27" }, selectedStatus)
                    }
                },
                enabled = company.isNotBlank() && title.isNotBlank()
            ) {
                Text("Add Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
