# Changelog

Android app (Kotlin + Jetpack Compose, MVVM, Hilt, DataStore) that uses an
AccessibilityService to detect and tap standard "Skip Ad" buttons, with a
multi-layer detection engine, per-app controls, system health checks,
onboarding, and richer analytics.

## Opening the project

1. Open this folder in **Android Studio (Koala or newer)**.
2. Let Android Studio generate the Gradle wrapper on first sync.
3. Sync Gradle, run on a device/emulator with **API 26+**.

## What's new in V2

- **Multi-layer detection** (`accessibility/AdDetector.kt`):
  1. Text (expanded phrase list: EN/AR + FR/ES/DE/PT/HI/TR/ID variants)
  2. Accessibility resource-ID matching (`skip_ad`, `btn_skip`, etc.)
  3. Position/layout heuristic — opt-in via Settings → "High" sensitivity,
     requires the same spot to repeat across a few frames before acting
     (`PositionMatchTracker`) to limit false positives
  4. Visual/ML recognition — **architectural stub only**
     (`SkipButtonVisualDetector` / `NoOpVisualDetector`). This is not a
     working model; it's the integration point for a real on-device model
     later. The Settings toggle for it is clearly labeled "Coming in a
     future update" and does nothing yet.
- **Detection cache** (`DetectionCache`) — remembers which layer/identifier
  worked last time per package, so repeat ads on the same app skip the full
  tree walk.
- **Per-app monitoring on/off** — Supported Apps screen now lists your real
  installed apps (icon, name, package) via PackageManager, each with a
  toggle (`AppListRepository`, `AppPreferencesRepository`).
- **System Health screen** — checks accessibility permission, battery
  optimization, notification permission, and overlay permission, each with
  a one-tap deep link to the right system settings page
  (`SystemHealthUtils`).
- **Onboarding flow** — welcome → how it works → privacy → accessibility
  setup → battery setup → start monitoring.
- **Privacy & Security screen** — plain-language explanation of what the
  service reads, what it never does, where data lives, permission
  rationale, and a trademark/affiliation disclaimer plus **About/copyright
  section** (version, © notice, OSS component licenses).
- **Persistent monitoring notification** with Pause/Resume actions
  (`NotificationActionReceiver`), separate from the one-off "ad skipped"
  alert notification.
- **Extended analytics** — daily/weekly/monthly/lifetime counts, most
  frequent apps, detection success rate, and rolling average detection
  latency (`StatisticsRepository`).
- **Reliability hardening** — every accessibility/coroutine call in the
  service is wrapped defensively so one bad frame can't crash monitoring
  for every app; `onUnbind` allows a clean reconnect if the OS rebinds the
  service.
- **Start-on-boot behavior** (`BootReceiver`) — controls whether monitoring
  resumes active or paused after a reboot.

## Honest limitations (not silently faked)

- **Visual/ML detection (Layer 4)** is a stub. No model ships in this
  project; wiring in a real on-device classifier is future work.
- **Play Store publishing** requires your own signing key — this project
  can be built and side-loaded, but I can't generate a production keystore
  under your identity.
- **"No hidden behavior"**: every permission declared in the manifest is
  tied to a specific visible feature — see the comments in
  `AndroidManifest.xml` and the Privacy & Security screen's permission list.

## Project structure

```
com.autoadskipper
├── ui/                     Home, Settings, Stats, Supported Apps, System Health, Privacy, onboarding/, theme/, nav/
├── accessibility/          AdSkipAccessibilityService, AdDetector (layers 1-3), DetectionCache,
│                           PositionMatchTracker, SkipButtonVisualDetector (V3 stub)
├── data/                   DataStoreManager, StatisticsRepository, AppPreferencesRepository,
│                           AppListRepository, model/
├── viewmodel/              Home, Settings, Stats, SupportedApps, SystemHealth, Onboarding
├── receiver/               NotificationActionReceiver (pause/resume), BootReceiver
├── di/                     Hilt AppModule
└── utils/                  AccessibilityUtils, SystemHealthUtils, Constants
```

## Roadmap (V3, not implemented)

- Real on-device visual/ML skip-button recognition
- Optional, clearly-disclosed cloud backup
- Community-contributed detection profiles

## V3 — reliability & universal coverage update

- **Multi-window scanning**: the service now enumerates every currently
  visible window (`getWindows()`), not just the focused one. This is the
  fix for missed skip buttons when the notification shade/quick settings
  are open, and for Picture-in-Picture / floating mini-player states — the
  ad app's window is still scanned even when it isn't the "active" one.
- **Generic post-ad popup handling** (`PopupFlowManager` +
  `AdDetector.findOverflowMenuButton` / `findDismissMenuItem`): when a
  small popup window shows a three-dot/overflow control with no direct
  close text, the engine opens it, then clicks the first dismiss-style menu
  item that appears next. Built as a generic two-step pattern (not
  YouTube-specific), with a 4-second timeout so a menu that never appears
  can't leave it stuck. Popup dismissals are shown in their own
  notification wording ("Cleared a leftover ad popup…") and are **not**
  counted in the "ads skipped" statistic, to keep that number honest.
- **Ad-SDK container scoping** (`AdDetector.findAdContainerBounds`): the
  position-heuristic fallback now prefers to search only inside a subtree
  whose class name matches a known ad SDK (AdMob, MoPub, AppLovin, Unity
  Ads, ironSource, Vungle, Meta Audience Network, Chartboost, Tapjoy,
  AdColony) rather than the whole screen, meaningfully cutting false
  positives when scanning apps with no specific rules written for them.
- **Broader close/dismiss vocabulary**: standalone interstitial/rewarded-ad
  completion screens are now matched via `Constants.CLOSE_DISMISS_PHRASES`
  (multi-language) plus content-description matching for icon-only close
  (X) buttons with no visible text.
- **More accessibility event types** (`typeWindowsChanged`,
  `typeNotificationStateChanged` added alongside the existing ones) so a
  relevant UI change is less likely to be missed entirely.

### Honest limits of this update
- The popup/overflow flow is heuristic, matched by content-description and
  resource-id fragments — a sufficiently different future redesign could
  still slip past it, though it's written generically rather than pinned
  to YouTube's current layout specifically.
- "Universal app support" here means a generic, app-agnostic detection
  engine (text + resource-ID + ad-SDK-scoped position + close/dismiss
  vocabulary) — it is not a guarantee against every ad implementation,
  particularly ads rendered inside a WebView with no native Android
  accessibility nodes at all (some ad formats do this), which no
  accessibility-service-based approach can see into.

## Design & performance refresh

- **Language now actually works.** Previously the Settings language picker
  only wrote a preference to DataStore — nothing ever told Android to
  switch. `utils/LocaleUtils.kt` now calls
  `AppCompatDelegate.setApplicationLocales()` (the modern per-app-language
  API) the moment the user picks a language, and the Home, Settings, and
  bottom-navigation screens now use real translatable string resources
  (`values/strings.xml` + `values-ar/strings.xml`) instead of hardcoded
  English text, so switching to Arabic actually changes the UI.
  **Not yet converted**: Stats, Supported Apps, System Health, Privacy, and
  onboarding screens still use hardcoded English strings — the pattern is
  now established (see any of the converted screens), so extending it to
  the rest is straightforward, just not done in this pass.
- **Lighter build**: migrated Hilt's annotation processor from `kapt` to
  **KSP**, which is meaningfully faster and less resource-hungry — this
  should visibly reduce build/sync time, which was likely a real part of
  what "felt heavy."
- **New app icon**: replaced the flat green double-triangle mark with a
  violet-to-teal gradient background and a softer, rounded double-chevron
  glyph, plus a monochrome layer for Android 13+ themed (Material You)
  icons.
- **Visual refresh**: new violet/teal color palette across the app
  (`ui/theme/Color.kt`), replacing flat elevated Material cards with
  softer tonal (shadow-free) surfaces, a gradient hero status card with a
  subtle pulsing "live" indicator on Home, and rounder corners throughout
  for a lighter, less boxy feel.

### Honest note on "heaviness"
If "heavy" also referred to APK size: `material-icons-extended` (used for
the wide icon set across the app) is a genuinely large dependency (several
MB, thousands of icons). Trimming it would mean replacing every icon
reference with a smaller custom set — a bigger follow-up task than fit in
this pass, but a legitimate next optimization if app size specifically is
the concern.

## V4 — critical fixes

1. **Removed the three-dot/overflow-menu automation entirely** (Option B).
   It was clicking overflow menus unrelated to ads across other apps —
   there's no reliable signal to confirm a three-dot button is ad-related,
   so rather than ship a heuristic with residual false-positive risk, it's
   gone. The engine now only ever acts on explicit text, resource-id, or
   close/dismiss-phrase matches — never on an unlabeled icon by itself.
2. **Language switching actually works now.** Root cause: the previous fix
   relied on `AppCompatDelegate.setApplicationLocales()`, which needs
   `AppCompatActivity` to reliably apply pre-Android 13 — this app uses
   plain `ComponentActivity` (Compose), so on API < 33 the change was
   silently not taking effect. Replaced with a direct
   `attachBaseContext()`-based Configuration wrap (`utils/LocaleUtils.kt`)
   applied at both the Application and Activity level, with the Activity
   recreating itself when the setting changes. Works identically on every
   supported API level. Notification text is now also translated.
3. **Performance**: layers 1–3 (text, resource-id, close/dismiss) are now
   evaluated in a **single** tree walk per window instead of three separate
   full traversals — this was the biggest avoidable cost, since it ran on
   every accessibility event. Added event throttling (max one full scan per
   150ms during event bursts, which are very common during video playback)
   and early filtering of irrelevant system windows (status bar, launcher,
   input method) before any node retrieval. Detection caching from earlier
   versions is retained.
4. **Supported Apps screen**: added a search/filter field (by name or
   package). Universal detection remains the default behavior — there is no
   allowlist gating which apps get scanned; per-app toggles are purely an
   opt-out control.

Version bumped to 4.0.0.

## V5 — confidence-gated close detection + real performance fix

1. **Fixed "clicks almost any close button."** Root cause: the previous
   `CLOSE_DISMISS_PHRASES` list included extremely generic words — "Close",
   "OK", "Continue", "Done", "Got It" — that appear constantly in totally
   unrelated Android UI (permission dialogs, onboarding, terms-of-service
   screens). These are now split:
   - `AD_EXPLICIT_CLOSE_PHRASES` ("Close Ad", "Dismiss Ad") — low ambiguity,
     acted on directly, same tier as Layer 1/2.
   - `GENERIC_CLOSE_PHRASES` (the ambiguous words above) — **never** acted
     on by text alone anymore. A confidence score is computed
     (`AdDetector.confidenceScore`) from three signals: the button sits
     inside a recognized ad-SDK container (+3), ad-indicator text like
     "Sponsored"/"Advertisement" is found nearby (+2), and/or it's in a
     typical ad-close corner position (+1). A match is only clicked once
     the total clears `CLOSE_CONFIDENCE_THRESHOLD` (3) — a bare generic
     word with none of these present is never clicked.
2. **Real performance fix**: settings and the disabled-package list were
   previously re-read from DataStore via `.first()` on every single
   accessibility event — each call has real overhead (disk-backed,
   coroutine dispatch), and this could fire many times a second during
   video playback. Replaced with a single background collector that keeps
   an in-memory cached copy, read synchronously on the hot path. The scan
   itself still runs off the main thread (dispatched via the service's
   background coroutine scope) so this doesn't risk UI jank.
3. **Supported Apps screen**: added "Monitor All" / "Exclude All" bulk
   actions alongside the existing per-app toggle and search — the
   "option to monitor all applications" / "ability to exclude specific
   applications" requirements.
4. Universal detection framework (text, resource-id, hierarchy/ad-container
   analysis, position analysis) remains the default with no per-app
   allowlist; layer 5 (AI/ML visual recognition) remains an honest stub —
   no working model ships in this project.

### Honest limits
The confidence scoring is a heuristic, not a guarantee — an ad implemented
with no recognizable SDK class name, no nearby "Sponsored"-style text, and
a generic "Close" label in a non-corner position still won't be clicked
(by design, to stay safe), meaning some ads may go un-skipped rather than
risk a wrong click. That trade-off is intentional given the false-positive
problem this update fixes.

## V6 — remaining false positives + time saved

### The real remaining cause of random clicks
V5 confidence-gated generic *close* words but left the *skip* list ungated —
and it contained bare "Skip", "تخطي", "Ignorer", "Omitir", "Pular", "Geç",
"Lewati". Those appear constantly in onboarding tutorials, setup wizards
and intro carousels, and were being clicked on text alone. Fixed the same
way as close buttons:
- `AD_EXPLICIT_SKIP_PHRASES` ("Skip Ad", "تخطي الإعلان") act directly.
- `GENERIC_SKIP_PHRASES` (bare words) now require the ad-context
  confidence score, same threshold as generic close text.

### Three additional hardening measures
1. **Position layer now requires a real ad container.** It previously fell
   back to scanning the whole screen's corners — effectively "click any
   small button in the corner". It now only runs when a recognized ad-SDK
   container is actually found.
2. **Post-launch grace period** (`GENERIC_MATCH_FOREGROUND_GRACE_MS`, 2.5s):
   generic confidence-gated matches are suppressed briefly after an app
   comes to the foreground, since tutorial/onboarding "Skip" buttons
   cluster there while real in-app ads appear later during use.
3. **Click rate ceiling** (`MAX_CLICKS_PER_WINDOW`, 6/minute): a hard cap
   independent of what detection believes. If the engine ever latches onto
   a recurring non-ad element, it stops clicking instead of hammering it.

### Time saved
New card on Home and a breakdown on Stats (today / this week / all time),
derived from skip counts at ~12s per skip. Clearly labelled as an estimate
in both English and Arabic — the app has no way to know how long any given
ad would actually have run, so presenting it as exact would be misleading.

Note: you asked for "version 4", but the project was already at 5 — this is
tagged 6.0.0 to keep the numbering moving forward.
