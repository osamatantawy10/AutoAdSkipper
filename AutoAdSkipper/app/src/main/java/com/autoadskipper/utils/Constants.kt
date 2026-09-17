package com.autoadskipper.utils

/**
 * Central place for static configuration used across the detection engine,
 * UI, and data layers.
 */
object Constants {

    // ---- Layer 1: text-based detection --------------------------------
    // Split by ambiguity, exactly like the close phrases below.
    //
    // AD_EXPLICIT_SKIP_PHRASES name an ad outright — essentially zero
    // false-positive risk, acted on directly.
    //
    // GENERIC_SKIP_PHRASES are bare words like "Skip" / "تخطي" / "Ignorer".
    // These appear CONSTANTLY in non-ad UI: onboarding tutorials, setup
    // wizards, intro carousels, permission primers, "skip this step" flows.
    // They are NEVER acted on by text alone — they require the same
    // ad-context confidence check as generic close buttons.
    val AD_EXPLICIT_SKIP_PHRASES: List<String> = listOf(
        // English
        "Skip Ad", "Skip Ads", "Skip this ad", "Skippable Ad", "Skip Advertisement",
        // Arabic
        "تجاوز الإعلان", "تخطي الإعلان",
        // French
        "Ignorer l'annonce", "Passer l'annonce",
        // Spanish
        "Omitir anuncio", "Saltar anuncio",
        // German
        "Werbung überspringen", "Anzeige überspringen",
        // Portuguese
        "Pular anúncio", "Ignorar anúncio",
        // Hindi
        "विज्ञापन छोड़ें",
        // Turkish
        "Reklamı geç",
        // Indonesian
        "Lewati iklan"
    )

    val GENERIC_SKIP_PHRASES: List<String> = listOf(
        "Skip", "Skip Video", "تخطي", "Ignorer", "Passer", "Omitir",
        "Saltar", "Überspringen", "Pular", "छोड़ें", "Geç", "Lewati"
    )

    // ---- Layer 2: accessibility resource-id based detection ------------
    // Matched as a case-insensitive "contains" against a node's viewIdResourceName.
    val SKIP_RESOURCE_ID_KEYWORDS: List<String> = listOf(
        "skip_ad", "skipad", "skip_button", "btn_skip", "ad_skip",
        "skip_ad_button", "instream_skip", "close_ad_button"
    )

    // ---- Layer 3: close/dismiss detection -------------------------------
    // Split into two tiers by how ambiguous the text is:
    //
    // AD_EXPLICIT_CLOSE_PHRASES literally reference an ad ("Close Ad") —
    // low false-positive risk on their own, similar to Layer 1.
    //
    // GENERIC_CLOSE_PHRASES ("Close", "OK", "Continue", "Done", "Got It")
    // are extremely common across ALL Android UI — permission dialogs,
    // onboarding flows, terms-of-service screens, you name it. These are
    // NEVER acted on by text alone. A match is only acted on once
    // AdDetector's confidence check finds real ad-context evidence nearby
    // (an ad-SDK container ancestor, or nearby ad-indicator text like
    // "Sponsored"/"Advertisement"). See AdDetector.hasAdContextEvidence.
    val AD_EXPLICIT_CLOSE_PHRASES: List<String> = listOf(
        "Close Ad", "Close ad", "Dismiss Ad", "Dismiss ad",
        "إغلاق الإعلان", "تجاهل الإعلان",
        "Fermer l'annonce", "Cerrar anuncio", "Anzeige schließen"
    )
    val GENERIC_CLOSE_PHRASES: List<String> = listOf(
        "Dismiss", "Close", "No Thanks", "No thanks", "Not Now", "Not now",
        "Continue", "Got It", "Got it", "OK", "Done",
        "إغلاق", "تجاهل", "لا شكرا",
        "Fermer", "Ignorer", "Cerrar", "Ahora no", "Schließen", "Fechar"
    )
    val CLOSE_CONTENT_DESCRIPTION_KEYWORDS: List<String> = listOf(
        "close", "dismiss", "exit", "cancel ad", "close ad", "close button"
    )

    // Words/phrases that, found near a candidate close button, count as
    // corroborating evidence the surrounding UI is actually an ad — used
    // only to help decide whether to act on a GENERIC_CLOSE_PHRASES match.
    val AD_INDICATOR_TEXT_KEYWORDS: List<String> = listOf(
        "sponsored", "advertisement", "install now", "download now",
        "learn more", "visit site", "shop now", "play now", "ad •",
        "إعلان", "برعاية", "ثبت الآن", "تنزيل الآن"
    )

    // Confidence scoring for GENERIC_CLOSE_PHRASES / icon-only close buttons.
    // A candidate must reach this total before the engine will act on it.
    const val CLOSE_CONFIDENCE_THRESHOLD = 3
    const val CONFIDENCE_AD_CONTAINER_BOUNDS = 3
    const val CONFIDENCE_NEARBY_AD_TEXT = 2
    const val CONFIDENCE_CORNER_POSITION = 1

    // ---- Layer 4: position/layout heuristic (opt-in, "High" sensitivity) --
    // Skip buttons are conventionally clickable, small, and pinned to the
    // top-right or bottom-right corner of the screen during video ad overlays.
    const val POSITION_ZONE_WIDTH_FRACTION = 0.35f
    const val POSITION_ZONE_HEIGHT_FRACTION = 0.25f
    // A position-only match must repeat in the same location across this
    // many consecutive events before it's trusted, to curb false clicks.
    const val POSITION_MATCH_CONFIRMATIONS_REQUIRED = 2

    // Class-name fragments belonging to common ad SDKs. When a node's class
    // matches one of these, its subtree is treated as a known "ad container"
    // and the position heuristic is scoped to search only inside it.
    val AD_SDK_CLASS_KEYWORDS: List<String> = listOf(
        "com.google.android.gms.ads", "com.google.ads", "com.mopub",
        "com.applovin", "com.unity3d.ads", "com.unity3d.services",
        "com.ironsource", "com.vungle", "com.facebook.ads",
        "com.chartboost", "com.tapjoy", "com.adcolony"
    )

    const val DEFAULT_CLICK_DELAY_MS = 300L
    const val MIN_CLICK_DELAY_MS = 0L
    const val MAX_CLICK_DELAY_MS = 3000L

    // Debounce so the same button isn't clicked repeatedly across rapid-fire events.
    const val CLICK_DEBOUNCE_MS = 800L

    // A burst of accessibility events (very common during video playback,
    // where content-changed can fire many times per second) triggers at
    // most one full scan per this interval — meaningfully cuts CPU/battery
    // cost with no perceptible effect on click responsiveness.
    const val MIN_SCAN_INTERVAL_MS = 150L

    // ---- Safety net -----------------------------------------------------
    // Hard ceiling on how often the engine may click, regardless of what
    // detection thinks it found. Real ad-skipping is inherently infrequent
    // (a handful of times per minute at most); a burst beyond this means
    // something has gone wrong — a misidentified recurring UI element, a
    // detection loop — and continuing would mean repeatedly clicking
    // unrelated content. Clicking stops until the window passes.
    const val MAX_CLICKS_PER_WINDOW = 6
    const val CLICK_RATE_WINDOW_MS = 60_000L

    // Minimum time that must pass after an app comes to the foreground
    // before generic (confidence-gated) matches may fire. Onboarding /
    // tutorial "Skip" buttons overwhelmingly appear in the first moments
    // after launching an app, whereas real in-app ads appear later during
    // use — this simple delay removes a large share of remaining false
    // positives at essentially no cost to real ad coverage.
    const val GENERIC_MATCH_FOREGROUND_GRACE_MS = 2500L

    // ---- Time saved estimation -------------------------------------------
    // Typical skippable pre-roll ads run 15-30s, with the skip becoming
    // available around 5s in — so skipping saves roughly the remainder.
    // This is an ESTIMATE, labelled as such in the UI; the app has no way
    // to know how long a given ad would actually have run.
    const val ESTIMATED_SECONDS_SAVED_PER_SKIP = 12

    const val NOTIFICATION_CHANNEL_ID = "ad_skip_channel"
    const val NOTIFICATION_ID = 1001
    const val MONITORING_NOTIFICATION_CHANNEL_ID = "monitoring_channel"
    const val MONITORING_NOTIFICATION_ID = 2001

    const val ACTION_PAUSE_MONITORING = "com.autoadskipper.action.PAUSE_MONITORING"
    const val ACTION_RESUME_MONITORING = "com.autoadskipper.action.RESUME_MONITORING"

    // Known / commonly supported packages surfaced as sensible defaults on
    // the Supported Apps screen. The engine isn't restricted to these — it
    // works on any foreground app by default — this only seeds friendly
    // display names for a curated subset.
    val KNOWN_APP_LABELS: Map<String, String> = mapOf(
        "com.google.android.youtube" to "YouTube",
        "com.google.android.apps.youtube.music" to "YouTube Music",
        "com.facebook.katana" to "Facebook",
        "com.instagram.android" to "Instagram",
        "com.zhiliaoapp.musically" to "TikTok",
        "tv.twitch.android.app" to "Twitch",
        "com.kiloo.subwaysurf" to "Subway Surfers",
        "com.king.candycrushsaga" to "Candy Crush Saga",
        "com.mxtech.videoplayer.ad" to "MX Player",
        "com.google.android.play.games" to "Google Play Games"
    )

    // Preference keys (DataStore — settings)
    const val KEY_SERVICE_ENABLED = "service_enabled"
    const val KEY_MONITORING_PAUSED = "monitoring_paused"
    const val KEY_CLICK_DELAY = "click_delay_ms"
    const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    const val KEY_SOUND_ENABLED = "sound_enabled"
    const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    const val KEY_LANGUAGE = "language"
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_SENSITIVITY = "detection_sensitivity"
    const val KEY_START_ON_BOOT = "start_on_boot"
    const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    const val KEY_EXPERIMENTAL_AI_DETECTION = "experimental_ai_detection"
    const val KEY_DISABLED_PACKAGES = "disabled_packages"

    // Statistics keys
    const val KEY_TOTAL_SKIPPED = "total_ads_skipped"
    const val KEY_TODAY_SKIPPED = "today_ads_skipped"
    const val KEY_TODAY_DATE = "today_date"
    const val KEY_LAST_SKIPPED_APP = "last_skipped_app"
    const val KEY_LAST_SKIPPED_PACKAGE = "last_skipped_package"
    const val KEY_LAST_SKIPPED_TIME = "last_skipped_time"
    const val KEY_DAILY_HISTORY_JSON = "daily_history_json"
    const val KEY_APP_FREQUENCY_JSON = "app_frequency_json"
    const val KEY_ATTEMPT_COUNT = "detection_attempt_count"
    const val KEY_SUCCESS_COUNT = "detection_success_count"
    const val KEY_AVG_DETECTION_MS = "avg_detection_ms"

    object Language {
        const val ENGLISH = "en"
        const val ARABIC = "ar"
        const val SYSTEM_DEFAULT = "system"
    }

    object ThemeMode {
        const val LIGHT = "light"
        const val DARK = "dark"
        const val SYSTEM = "system"
    }

    object Sensitivity {
        /** Text + resource-id + close/dismiss layers only. Safest, fewest false positives. */
        const val STANDARD = "standard"
        /** Adds the position/layout heuristic as a fallback. */
        const val HIGH = "high"
    }
}
