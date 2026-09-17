package com.autoadskipper.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoadskipper.R
import com.autoadskipper.data.model.ServiceStatus
import com.autoadskipper.utils.TimeFormatUtils
import com.autoadskipper.viewmodel.HomeViewModel
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenSystemHealth: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            HeroStatusCard(
                status = state.serviceStatus,
                autoSkipEnabled = state.autoSkipEnabled,
                monitoringPaused = state.monitoringPaused,
                onToggle = { enabled ->
                    if (enabled && state.serviceStatus == ServiceStatus.ENABLED_BUT_PERMISSION_MISSING) {
                        viewModel.openAccessibilitySettings()
                    }
                    viewModel.setAutoSkipEnabled(enabled)
                },
                onGrantPermission = { viewModel.openAccessibilitySettings() },
                onPauseResume = { paused -> viewModel.setMonitoringPaused(paused) }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatBlock(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_ads_today),
                        value = state.statistics.todaySkipped.toString()
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    StatBlock(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_ads_total),
                        value = "%,d".format(state.statistics.totalSkipped)
                    )
                }
            }
        }

        item {
            TimeSavedCard(
                todaySeconds = state.statistics.secondsSavedToday,
                totalSeconds = state.statistics.secondsSavedTotal
            )
        }

        item {
            LastSkippedCard(
                appName = state.statistics.lastSkippedApp,
                timeMillis = state.statistics.lastSkippedTime
            )
        }

        item {
            SystemHealthBanner(onClick = onOpenSystemHealth)
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}



@Composable
private fun HeroStatusCard(
    status: ServiceStatus,
    autoSkipEnabled: Boolean,
    monitoringPaused: Boolean,
    onToggle: (Boolean) -> Unit,
    onGrantPermission: () -> Unit,
    onPauseResume: (Boolean) -> Unit
) {
    val (accentStart, accentEnd, statusLabel) = when (status) {
        ServiceStatus.ENABLED_AND_RUNNING -> Triple(Color(0xFF16A34A), Color(0xFF0D9488), stringResource(R.string.home_status_running))
        ServiceStatus.ENABLED_BUT_PAUSED -> Triple(Color(0xFFF59E0B), Color(0xFFEA580C), stringResource(R.string.home_status_paused))
        ServiceStatus.ENABLED_BUT_PERMISSION_MISSING -> Triple(Color(0xFFDC2626), Color(0xFFBE123C), stringResource(R.string.home_status_permission_needed))
        ServiceStatus.DISABLED -> Triple(Color(0xFF64748B), Color(0xFF475569), stringResource(R.string.home_status_disabled))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(accentStart, accentEnd)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isRunning = status == ServiceStatus.ENABLED_AND_RUNNING
                    PulsingDot(active = isRunning)
                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {
                        Text(
                            stringResource(R.string.home_service_status),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            statusLabel,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(checked = autoSkipEnabled, onCheckedChange = onToggle)
                }

                if (status == ServiceStatus.ENABLED_BUT_PERMISSION_MISSING) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.home_permission_explainer),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = onGrantPermission) {
                        Text(stringResource(R.string.home_open_accessibility_settings), color = Color.White)
                    }
                }

                if (status == ServiceStatus.ENABLED_AND_RUNNING || status == ServiceStatus.ENABLED_BUT_PAUSED) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { onPauseResume(!monitoringPaused) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (monitoringPaused) stringResource(R.string.home_resume_monitoring)
                            else stringResource(R.string.home_pause_monitoring)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.6f else 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulseScale"
    )
    Box(
        modifier = Modifier
            .size(14.dp)
            .background(Color.White.copy(alpha = if (active) 0.9f else 0.5f), CircleShape)
    )
}

@Composable
private fun StatBlock(modifier: Modifier = Modifier, title: String, value: String) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimeSavedCard(todaySeconds: Int, totalSeconds: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null)
                Text(
                    stringResource(R.string.home_time_saved),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                TimeFormatUtils.formatDuration(todaySeconds),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.home_time_saved_total), style = MaterialTheme.typography.bodyMedium)
                Text(
                    TimeFormatUtils.formatDuration(totalSeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_time_saved_estimate_note),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LastSkippedCard(appName: String?, timeMillis: Long?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_last_app_detected), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            if (appName == null) {
                Text(stringResource(R.string.home_no_ads_yet), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                timeMillis?.let {
                    Text(
                        stringResource(R.string.home_last_skip_prefix, relativeTime(it)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemHealthBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.HealthAndSafety, contentDescription = null)
            Text(
                stringResource(R.string.home_system_health),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.home_check))
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
    }
}

private fun relativeTime(timeMillis: Long): String {
    val diffMs = System.currentTimeMillis() - timeMillis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "$days d ago"
    }
}
