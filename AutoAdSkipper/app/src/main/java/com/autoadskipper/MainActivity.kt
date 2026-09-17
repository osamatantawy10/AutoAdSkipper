package com.autoadskipper

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoadskipper.ui.navigation.AutoAdSkipperNavHost
import com.autoadskipper.ui.theme.AutoAdSkipperTheme
import com.autoadskipper.utils.Constants
import com.autoadskipper.utils.LocaleUtils
import com.autoadskipper.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoAdSkipperRoot()
        }
    }
}

@Composable
private fun AutoAdSkipperRoot(settingsViewModel: SettingsViewModel = hiltViewModel()) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The language actually baked into this Activity's Context at creation
    // time (via attachBaseContext). If the persisted setting has since
    // diverged from it (the user just changed it in Settings), recreate the
    // Activity so attachBaseContext runs again with the new value — a
    // Configuration wrap made after the fact doesn't retroactively re-apply
    // to already-resolved Compose resources.
    val appliedLanguage = remember { LocaleUtils.getSavedLanguage(context) }
    LaunchedEffect(settings.language) {
        if (settings.language.isNotBlank() && settings.language != appliedLanguage) {
            (context as? android.app.Activity)?.recreate()
        }
    }

    val darkTheme = when (settings.themeMode) {
        Constants.ThemeMode.DARK -> true
        Constants.ThemeMode.LIGHT -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    AutoAdSkipperTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AutoAdSkipperNavHost()
        }
    }
}
