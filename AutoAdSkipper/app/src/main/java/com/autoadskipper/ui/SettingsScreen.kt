package com.autoadskipper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoadskipper.R
import com.autoadskipper.utils.Constants
import com.autoadskipper.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onOpenSystemHealth: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium) }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_auto_skip),
                subtitle = stringResource(R.string.settings_auto_skip_sub),
                checked = settings.serviceEnabled,
                onCheckedChange = viewModel::setAutoSkipEnabled
            )
        }

        item { SensitivityCard(current = settings.sensitivity, onSelected = viewModel::setSensitivity) }

        item { DelaySettingCard(delayMs = settings.clickDelayMs, onDelayChange = viewModel::setClickDelay) }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_vibration),
                subtitle = stringResource(R.string.settings_vibration_sub),
                checked = settings.vibrationEnabled,
                onCheckedChange = viewModel::setVibrationEnabled
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_sound),
                subtitle = stringResource(R.string.settings_sound_sub),
                checked = settings.soundEnabled,
                onCheckedChange = viewModel::setSoundEnabled
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_notifications),
                subtitle = stringResource(R.string.settings_notifications_sub),
                checked = settings.notificationEnabled,
                onCheckedChange = viewModel::setNotificationEnabled
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_start_on_boot),
                subtitle = stringResource(R.string.settings_start_on_boot_sub),
                checked = settings.startOnBoot,
                onCheckedChange = viewModel::setStartOnBoot
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_ai_beta),
                subtitle = stringResource(R.string.settings_ai_beta_sub),
                checked = settings.experimentalAiDetectionEnabled,
                onCheckedChange = viewModel::setExperimentalAiDetectionEnabled
            )
        }

        item {
            ChoiceSettingCard(
                title = stringResource(R.string.settings_theme),
                current = settings.themeMode,
                options = listOf(
                    Constants.ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                    Constants.ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                    Constants.ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
                ),
                onSelected = viewModel::setThemeMode
            )
        }

        item {
            ChoiceSettingCard(
                title = stringResource(R.string.settings_language),
                current = settings.language,
                options = listOf(
                    Constants.Language.SYSTEM_DEFAULT to stringResource(R.string.settings_lang_system),
                    Constants.Language.ENGLISH to stringResource(R.string.settings_lang_en),
                    Constants.Language.ARABIC to stringResource(R.string.settings_lang_ar)
                ),
                onSelected = viewModel::setLanguage
            )
        }

        item {
            LinkRow(
                title = stringResource(R.string.settings_health_check),
                subtitle = stringResource(R.string.settings_health_check_sub),
                onClick = onOpenSystemHealth
            )
        }

        item {
            LinkRow(
                title = stringResource(R.string.settings_privacy),
                subtitle = stringResource(R.string.settings_privacy_sub),
                onClick = onOpenPrivacy
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun DelaySettingCard(delayMs: Long, onDelayChange: (Long) -> Unit) {
    var sliderPosition by remember(delayMs) { mutableStateOf(delayMs.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.settings_delay_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${sliderPosition.toInt()} ms", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = { onDelayChange(sliderPosition.toLong()) },
                valueRange = Constants.MIN_CLICK_DELAY_MS.toFloat()..Constants.MAX_CLICK_DELAY_MS.toFloat(),
                steps = 29
            )
        }
    }
}

@Composable
private fun SensitivityCard(current: String, onSelected: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.settings_sensitivity), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (current == Constants.Sensitivity.HIGH) stringResource(R.string.settings_sensitivity_high) else stringResource(R.string.settings_sensitivity_standard),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = current == Constants.Sensitivity.HIGH,
                    onCheckedChange = { onSelected(if (it) Constants.Sensitivity.HIGH else Constants.Sensitivity.STANDARD) }
                )
            }
        }
    }
}

@Composable
private fun ChoiceSettingCard(title: String, current: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == current }?.second ?: options.first().second

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.settings_change)) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (code, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { onSelected(code); expanded = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onClick) { Text(stringResource(R.string.settings_open)) }
        }
    }
}
