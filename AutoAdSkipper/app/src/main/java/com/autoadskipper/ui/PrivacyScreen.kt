package com.autoadskipper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        item { Text("Privacy & Security", style = MaterialTheme.typography.headlineMedium) }

        item {
            InfoCard(
                title = "Why Accessibility access is needed",
                body = "Android only allows an app to read on-screen button labels and tap them on your behalf through the Accessibility API. Auto Ad Skipper uses this solely to find and tap standard \"Skip Ad\" buttons — it does not use Accessibility to read your messages, passwords, banking apps, or anything unrelated to ad-skip buttons."
            )
        }

        item {
            InfoCard(
                title = "What this app does",
                body = "• Reads visible button text, button IDs, and (only if you enable High sensitivity) approximate on-screen button position, to decide whether something looks like a skip, close, or dismiss button.\n• Scans all currently visible windows — not just the one in focus — so detection keeps working while the notification shade is open or a video is in Picture-in-Picture mode. It never reads content from windows outside the apps you allow it to monitor.\n• Taps that button on your behalf.\n• Keeps a private, on-device count of how many ads were skipped, in which app, and how fast detection ran."
            )
        }

        item {
            InfoCard(
                title = "What this app never does",
                body = "• Never sends anything it reads off your screen anywhere — there is no network/analytics code in this app at all.\n• Never reads clipboard content, typed text, passwords, or messages.\n• Never collects your name, email, location, or any personal identifier.\n• Never shows you ads of its own or injects content into other apps."
            )
        }

        item {
            InfoCard(
                title = "Where your data lives",
                body = "All settings and statistics are stored locally on your device using Android's DataStore, inside this app's private storage. Uninstalling the app deletes this data. There is currently no cloud sync — a future optional cloud-backup feature (see Roadmap) would be opt-in and clearly disclosed before any data left your device."
            )
        }

        item {
            InfoCard(
                title = "Permissions this app requests, and why",
                body = "• Notifications — to show the skip-alert and monitoring-status notifications you control in Settings.\n• Vibration — for the optional \"vibrate on skip\" setting.\n• Ignore battery optimizations — so Android doesn't pause background monitoring; you choose whether to grant this from the System Health screen.\n• Receive boot completed — only to apply your \"start monitoring on boot\" preference.\nNo other permissions are requested."
            )
        }

        item {
            InfoCard(
                title = "Trademark & affiliation disclaimer",
                body = "Auto Ad Skipper is an independent, unofficial personal-use utility. App names such as YouTube, TikTok, Instagram, Facebook, and Twitch referenced in this app are trademarks of their respective owners. This app is not created by, affiliated with, or endorsed by any of those companies, and interacting with third-party apps in ways they don't officially support may not comply with those apps' own terms of service — that's between you and them; use is at your own discretion."
            )
        }

        item {
            AboutCard()
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Auto Ad Skipper", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text("Version 6.0.0", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "© 2026 Auto Ad Skipper. All rights reserved.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Built for personal use with Kotlin, Jetpack Compose, and Material 3. This is not a published or Play Store–distributed product; it is provided as-is, without warranty, for the individual who requested it to build, sign, and use on their own device.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Open-source components used: AndroidX / Jetpack Compose, Material Components for Android, Kotlin Coroutines, Dagger Hilt, and Kotlinx Serialization — each distributed under the Apache License 2.0 by Google LLC / JetBrains / Google Dagger, respectively.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
