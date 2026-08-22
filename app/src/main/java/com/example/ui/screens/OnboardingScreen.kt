package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String
)

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val steps = listOf(
        OnboardingStep(
            title = "RCS Vault",
            description = "Securely preserve what matters.\nOn-device, private backup for incoming RCS and SMS messages.",
            icon = Icons.Filled.Shield,
            badge = "SECURE ON-DEVICE"
        ),
        OnboardingStep(
            title = "Notification-Based Capture",
            description = "Android restricts third-party access to internal RCS databases. RCS Vault utilizes authorized notification capture to store messages locally without root or cloud servers.",
            icon = Icons.Filled.Forum,
            badge = "OFFICIAL COMPLIANCE"
        ),
        OnboardingStep(
            title = "Media Preservation",
            description = "Archive received photos, audio recordings, videos, and documents to protected app storage with SHA-256 integrity verification.",
            icon = Icons.Filled.PermMedia,
            badge = "LOCAL STORAGE"
        ),
        OnboardingStep(
            title = "Verified PDF Transcripts",
            description = "Generate authentic PDF records containing only data actually stored on your device, formatted for legal and personal reference.",
            icon = Icons.Filled.PictureAsPdf,
            badge = "AUTHENTIC REPORTS"
        ),
        OnboardingStep(
            title = "Enable Notification Access",
            description = "To automatically preserve incoming messages as they arrive, please grant Notification Listener access.",
            icon = Icons.Filled.NotificationsActive,
            badge = "SETUP REQUIRED"
        )
    )

    val pagerState = rememberPagerState(pageCount = { steps.size })

    Scaffold(
        containerColor = SurfacePureWhite,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    repeat(steps.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) MintPrimary else MintPrimaryLight)
                        )
                    }
                }

                if (pagerState.currentPage == steps.size - 1) {
                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            } catch (e: Exception) {
                                // fallback
                            }
                            viewModel.repository.preferences.isOnboardingCompleted = true
                            onCompleteOnboarding()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("onboarding_grant_finish_btn")
                    ) {
                        Text("Grant Permission & Start", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.repository.preferences.isOnboardingCompleted = true
                                onCompleteOnboarding()
                            }
                        ) {
                            Text("Skip", color = TextTertiaryDark, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Next", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            val step = steps[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = MintPrimaryLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = step.badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MintPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MintPrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondaryDark,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
