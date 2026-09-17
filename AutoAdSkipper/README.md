# Auto Ad Skipper

An Android utility that automatically detects and taps "Skip Ad" buttons using
Android's Accessibility Services. Built with Kotlin, Jetpack Compose and
Material 3.

> **Personal-use project.** This is not published on the Play Store. It is
> provided as-is, without warranty. See [Disclaimer](#disclaimer).

---

## Features

- **Multi-layer detection engine** — skip buttons are found by text, resource
  ID, ad-container hierarchy analysis, and (optionally) on-screen position
- **Confidence-gated clicking** — ambiguous labels like "Skip" or "Close" are
  only acted on when corroborating ad-context evidence is present, so the app
  doesn't click tutorial or dialog buttons
- **Works app-wide** — a generic engine rather than a per-app allowlist
- **Per-app control** — enable/disable monitoring for any installed app, with
  search and bulk actions
- **Dashboard & analytics** — daily/weekly/monthly/lifetime counts, estimated
  time saved, detection success rate and speed
- **System health checks** — verifies accessibility, battery optimisation and
  notification permissions, with one-tap links to the right settings screens
- **Bilingual** — English and Arabic (العربية)
- **Light / dark / Material You theming**
- **Fully on-device** — no network code, no analytics, no data collection

---

## Screenshots

<!-- Add screenshots here. Suggested: Home, Stats, Supported Apps, Settings.
     Put image files in a docs/ folder and reference them like:
     ![Home](docs/screenshot-home.png) -->

_Coming soon._

---

## Requirements

| | |
|---|---|
| Minimum Android | 8.0 (API 26) |
| Target SDK | 34 |
| JDK | 17 |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |

---

## Getting the APK

### Option A — download a prebuilt APK (easiest)

Every push to `main` triggers an automated build. To grab the result:

1. Open the **Actions** tab of this repository
2. Click the most recent successful **Build APK** run
3. Download the `auto-ad-skipper-debug` artifact from the Artifacts section
4. Unzip it and transfer the `.apk` to your phone

### Option B — build locally

```bash
git clone https://github.com/<your-username>/auto-ad-skipper.git
cd auto-ad-skipper
```

Then open the folder in **Android Studio (Koala or newer)**, let Gradle sync
(it will generate the Gradle wrapper automatically), and use
**Build → Build Bundle(s) / APK(s) → Build APK(s)**.

The APK lands in `app/build/outputs/apk/debug/`.

---

## Installation & setup

1. Install the APK on your device. Android may warn about installing from an
   unknown source — this is normal for any app not distributed via the Play
   Store.
2. If Play Protect blocks the install, choose **Install anyway**, or
   temporarily disable Play Protect scanning in
   *Settings → Google Play Protect*.
3. Sideloaded apps are restricted from sensitive permissions by default on
   Android 13+. If the accessibility toggle is greyed out, go to
   **Settings → Apps → Auto Ad Skipper → ⋮ → Allow restricted settings**.
4. Enable the service in **Settings → Accessibility → Auto Ad Skipper**.
   Android requires this to be done manually — an app cannot grant itself
   accessibility access.

---

## How it works

The accessibility service scans all visible windows on relevant UI events and
looks for skip/close controls in layered order, cheapest and most precise
first:

| Layer | Method | Acts alone? |
|---|---|---|
| 1 | Explicit ad text ("Skip Ad", "تخطي الإعلان") | Yes |
| 2 | Known skip-button resource IDs | Yes |
| 3 | Ambiguous text ("Skip", "Close", "OK") | Only with confidence ≥ threshold |
| 4 | Position within a recognised ad-SDK container | Only at High sensitivity, with repeat confirmation |
| 5 | Visual/ML recognition | Not implemented — architectural stub only |

Ambiguous matches are scored against ad-context signals: presence inside a
known ad-SDK container (+3), nearby ad-indicator text such as "Sponsored"
(+2), and typical corner placement (+1). A score of 3 is required before any
click. Additional safeguards include a post-launch grace period (tutorial
"Skip" buttons cluster right after an app opens) and a hard ceiling of 6
clicks per minute.

See [`CHANGELOG.md`](CHANGELOG.md) for the full development history.

---

## Project structure

```
com.autoadskipper
├── ui/               Compose screens, theme, navigation, onboarding
├── accessibility/    AdSkipAccessibilityService, AdDetector, caches
├── data/             Repositories, DataStore, models
├── viewmodel/        One ViewModel per screen (MVVM)
├── receiver/         Boot + notification-action receivers
├── di/               Hilt module
└── utils/            Constants, locale, system health, formatting
```

---

## Privacy

Auto Ad Skipper reads on-screen button labels and IDs solely to identify skip
and close controls. It does not read messages, passwords, or other personal
content, and contains **no networking code of any kind** — nothing it reads
can leave your device. All settings and statistics are stored locally via
DataStore, and are deleted when the app is uninstalled.

---

## Known limitations

- Ads rendered entirely inside a WebView with no native accessibility nodes
  cannot be detected by any accessibility-service-based approach
- Layer 5 (visual/ML detection) is an interface stub — no model ships here
- Detection is heuristic; some ads will be missed by design, since the
  confidence gating deliberately favours missing an ad over a wrong click
- Aggressive OEM battery managers (Xiaomi, Huawei, Oppo, Vivo) may stop the
  service in the background beyond standard Android battery optimisation

---

## Disclaimer

Auto Ad Skipper is an independent, unofficial personal-use utility. App names
such as YouTube, TikTok, Instagram and Facebook are trademarks of their
respective owners; this project is not affiliated with or endorsed by any of
them.

Automatically interacting with third-party apps may not comply with those
apps' terms of service, and ad-skipping tools sit in a restricted area of
Google Play's policies. Use at your own discretion.

---

## License

[MIT](LICENSE)
