package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NavigationTab

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}

@Composable
fun MainAppRoot(viewModel: MainViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val isOnboardingDone = viewModel.repository.preferences.isOnboardingCompleted
    var showOnboarding by remember { mutableStateOf(!isOnboardingDone) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    val currentTab by viewModel.currentTab.collectAsState()
    val selectedConversation by viewModel.selectedConversation.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    when {
        showSplash -> {
            SplashScreen(onSplashFinished = { showSplash = false })
        }
        isAppLocked -> {
            SecurityLockScreen(viewModel = viewModel)
        }
        showOnboarding -> {
            OnboardingScreen(
                viewModel = viewModel,
                onCompleteOnboarding = { showOnboarding = false }
            )
        }
        showPrivacyPolicy -> {
            PrivacyPolicyScreen(onBackClick = { showPrivacyPolicy = false })
        }
        selectedConversation != null -> {
            ConversationDetailScreen(
                conversation = selectedConversation!!,
                viewModel = viewModel,
                onBackClick = { viewModel.closeConversation() },
                onGenerateReportClick = {
                    viewModel.closeConversation()
                    viewModel.selectTab(NavigationTab.REPORTS)
                }
            )
        }
        else -> {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    AppBottomNavigationBar(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) { innerPadding ->
                AnimatedContent(
                    targetState = currentTab,
                    label = "TabTransition",
                    modifier = Modifier.padding(innerPadding)
                ) { tab ->
                    when (tab) {
                        NavigationTab.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToPrivacy = { showPrivacyPolicy = true }
                            )
                        }
                        NavigationTab.MESSAGES -> {
                            MessagesScreen(
                                viewModel = viewModel,
                                onOpenConversation = { viewModel.openConversation(it) }
                            )
                        }
                        NavigationTab.MEDIA -> {
                            MediaScreen(viewModel = viewModel)
                        }
                        NavigationTab.REPORTS -> {
                            ReportsScreen(viewModel = viewModel)
                        }
                        NavigationTab.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToPrivacy = { showPrivacyPolicy = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // Global Update Dialog (triggers on startup or when manual check finds update)
    if (!showSplash && !isAppLocked) {
        UpdateDialog(viewModel = viewModel)
    }
}
