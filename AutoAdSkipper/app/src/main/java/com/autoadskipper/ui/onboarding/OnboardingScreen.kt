package com.autoadskipper.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoadskipper.viewmodel.OnboardingViewModel

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String
)

private val staticPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.WavingHand,
        title = "Welcome to Auto Ad Skipper",
        body = "Automatically detect and tap \"Skip Ad\" buttons across your apps, so you spend less time waiting and more time doing."
    ),
    OnboardingPage(
        icon = Icons.Filled.SkipNext,
        title = "How it works",
        body = "Auto Ad Skipper watches for standard skip buttons — by their text, their button ID, and optionally their position — then taps them for you. Everything runs locally on your device."
    ),
    OnboardingPage(
        icon = Icons.Filled.PrivacyTip,
        title = "Your privacy, respected",
        body = "No personal data is collected. No cloud storage. No hidden background activity. You can read the full breakdown anytime from Settings → Privacy & Security."
    )
)

/**
 * Simple index-driven step flow (Welcome / How it works / Privacy / Accessibility
 * setup / Battery setup), navigated with Next/Back buttons rather than a swipeable
 * pager — this avoids depending on Compose Foundation's Pager APIs, which have
 * changed shape across library versions.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onFinished: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = staticPages.size + 2 // + accessibility setup + battery setup
    val isLastPage = currentPage == totalPages - 1

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            when {
                currentPage < staticPages.size -> StaticPageContent(staticPages[currentPage])
                currentPage == staticPages.size -> AccessibilitySetupPage(viewModel)
                else -> BatterySetupPage(viewModel)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                viewModel.completeOnboarding(enableService = false)
                onFinished()
            }) { Text("Skip") }

            Row {
                if (currentPage > 0) {
                    TextButton(onClick = { currentPage -= 1 }) { Text("Back") }
                    Spacer(Modifier.padding(horizontal = 4.dp))
                }
                Button(onClick = {
                    if (isLastPage) {
                        viewModel.completeOnboarding(enableService = viewModel.isAccessibilityGranted())
                        onFinished()
                    } else {
                        currentPage += 1
                    }
                }) {
                    Text(if (isLastPage) "Start Monitoring" else "Next")
                }
            }
        }
    }
}

@Composable
private fun StaticPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(page.icon, contentDescription = null, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text(page.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(page.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AccessibilitySetupPage(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Accessibility, contentDescription = null, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text("Enable Accessibility Access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "For security, Android requires you to turn this on manually. Tap below, find \"Auto Ad Skipper\" in the list, and enable it.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { viewModel.openAccessibilitySettings() }) {
            Text("Open Accessibility Settings")
        }
    }
}

@Composable
private fun BatterySetupPage(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text("Avoid Battery Restrictions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Some phones aggressively stop background apps to save battery. Allowing Auto Ad Skipper to ignore battery optimization keeps monitoring reliable. This is optional.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { viewModel.openBatterySettings() }) {
            Text("Open Battery Settings")
        }
    }
}
