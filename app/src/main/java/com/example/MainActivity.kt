package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.*
import com.example.ui.viewmodel.ResumeRoasterViewModel
import com.example.ui.viewmodel.ResumeRoasterViewModelFactory

enum class AppTab(
    val route: String,
    val title: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    DASHBOARD("dashboard", "Roast & Scores", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    TOOLS("tools", "AI Tools", Icons.Filled.Build, Icons.Outlined.Build),
    HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History),
    ADMIN("admin", "Stats", Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard)
}

class MainActivity : ComponentActivity() {
    private val viewModel: ResumeRoasterViewModel by viewModels {
        ResumeRoasterViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(AppTab.HOME) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = NavigationBarDefaults.Elevation
                        ) {
                            AppTab.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.iconSelected else tab.iconUnselected,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { currentTab = AppTab.DASHBOARD }
                            )
                            AppTab.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel
                            )
                            AppTab.TOOLS -> ToolsScreen(
                                viewModel = viewModel
                            )
                            AppTab.HISTORY -> HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { currentTab = AppTab.DASHBOARD }
                            )
                            AppTab.ADMIN -> AdminScreen()
                        }
                    }
                }
            }
        }
    }
}
