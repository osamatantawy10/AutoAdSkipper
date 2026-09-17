package com.autoadskipper.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoadskipper.data.model.MonitoredAppInfo
import com.autoadskipper.viewmodel.SupportedAppsViewModel
import java.util.concurrent.TimeUnit

@Composable
fun SupportedAppsScreen(viewModel: SupportedAppsViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val filteredApps = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        item { Text("Supported Apps", style = MaterialTheme.typography.headlineMedium) }
        item {
            Text(
                "Auto Ad Skipper monitors every installed app by default using a generic detection engine — there's no fixed app list. Turn off monitoring for specific apps below if you'd rather it not act there.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { viewModel.enableAllMonitoring() }, modifier = Modifier.weight(1f)) {
                    Text("Monitor All")
                }
                OutlinedButton(onClick = { viewModel.disableAllMonitoring() }, modifier = Modifier.weight(1f)) {
                    Text("Exclude All")
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps or package name") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )
        }

        if (apps.isEmpty()) {
            item { Text("Loading installed apps…", style = MaterialTheme.typography.bodyMedium) }
        } else if (filteredApps.isEmpty()) {
            item { Text("No apps match \"$query\"", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(filteredApps, key = { it.packageName }) { app ->
                SupportedAppRow(app, onToggle = { enabled -> viewModel.setMonitoringEnabled(app.packageName, enabled) })
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SupportedAppRow(app: MonitoredAppInfo, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bitmap = remember(app.packageName) {
                app.icon?.let { drawableToImageBitmap(it) }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Column(modifier = Modifier.size(44.dp)) {}
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(app.packageName, style = MaterialTheme.typography.bodyMedium)
                val detail = if (app.lastDetectedActivity != null && app.lastDetectionTime != null) {
                    "${app.lastDetectedActivity} · ${relativeTime(app.lastDetectionTime)}"
                } else {
                    "No detections yet"
                }
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }

            Switch(checked = app.isMonitoringEnabled, onCheckedChange = onToggle)
        }
    }
}

private fun drawableToImageBitmap(drawable: android.graphics.drawable.Drawable) =
    runCatching {
        val bitmap = android.graphics.Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    }.getOrNull()

private fun relativeTime(timeMillis: Long): String {
    val diffMs = System.currentTimeMillis() - timeMillis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "${TimeUnit.MILLISECONDS.toDays(diffMs)} d ago"
    }
}
