# 📺 MiTV Player

<p align="center">
  <img src="https://i.ibb.co/5hPyzP10/1773218533375-removebg-preview.png" width="120" alt="MiTV Logo"/>
</p>

<p align="center">
  <strong>Premium IPTV Player for Android</strong><br/>
  Built with Kotlin · Jetpack Compose · Media3 ExoPlayer · Hilt
</p>

---

## ✨ Features

| Feature | Details |
|---|---|
| 🎨 **4 Premium Themes** | Dark Gold, Midnight Blue, AMOLED Black, Crimson Dark |
| 📋 **M3U Engine** | Parse M3U playlists instantly, grouped by category |
| ▶️ **Universal Playback** | HLS (M3U8), DASH (MPD), MP4, MKV |
| 👆 **Gesture Controls** | Swipe left = brightness, swipe right = volume |
| 🔄 **Auto-Retry** | Automatic reconnect on stream failure |
| 🖼️ **Fallback Screen** | Beautiful error UI with Retry + Support buttons |
| 💧 **MiTV Watermark** | Semi-transparent logo in player bottom-right |
| ⚡ **Zero-lag switching** | ExoPlayer with smart buffer management |
| 🗂️ **Multi-Playlist** | Add and manage multiple M3U sources |

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **Media:** AndroidX Media3 (ExoPlayer) — HLS · DASH · Progressive
- **DI:** Hilt
- **DB:** Room + DataStore Preferences
- **Images:** Coil 3
- **Architecture:** MVVM + Clean Architecture
- **CI/CD:** GitHub Actions → signed APK

---

## 🚀 Quick Start

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17+
- Android SDK 35

### 1. Clone
```bash
git clone https://github.com/YOUR_USERNAME/MiTV_Player.git
cd MiTV_Player
```

### 2. Open in Android Studio
File → Open → select the `MiTV_Player` folder

### 3. Sync Gradle
Let Android Studio sync. All dependencies are in `gradle/libs.versions.toml`.

### 4. Run
Connect a device or start an emulator, then press ▶ Run.

---

## 📦 GitHub Actions CI/CD

Automatic signed APK builds on every push to `main`.

### Setup Secrets
Go to your repo → **Settings → Secrets → Actions** and add:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i release.keystore` output |
| `KEYSTORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | Your key alias |
| `KEY_PASSWORD` | Your key password |

### Generate a Keystore (first time)
```bash
keytool -genkeypair \
  -alias mitv-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore release.keystore \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=MiTV Player, OU=Mobile, O=MiTV, L=City, ST=State, C=US"

# Encode to base64 for GitHub secret:
base64 -i release.keystore | pbcopy   # macOS
base64 -w 0 release.keystore          # Linux
```

### Trigger a Build
Push to `main` or go to **Actions → MiTV Player Build → Run workflow**.

---

## 📂 Project Structure

```
MiTV_Player/
├── app/src/main/
│   ├── java/com/mitv/player/
│   │   ├── data/
│   │   │   ├── Models.kt          # Channel, Category, Playlist, AppSettings
│   │   │   ├── M3uParser.kt       # Fast M3U parser
│   │   │   └── Repository.kt      # Room + DataStore repository
│   │   ├── player/
│   │   │   └── MiVideoPlayer.kt   # Media3 ExoPlayer engine
│   │   ├── di/
│   │   │   └── AppModule.kt       # Hilt DI module
│   │   ├── ui/
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt       # All 4 theme color palettes
│   │   │   │   ├── Theme.kt       # MaterialTheme + LocalAccentColor
│   │   │   │   └── Type.kt        # Typography
│   │   │   ├── screens/
│   │   │   │   ├── DashboardScreen.kt   # Channel browser + search
│   │   │   │   ├── PlayerScreen.kt      # Full-screen player + gestures
│   │   │   │   └── SettingsScreen.kt    # Theme, playlists, buffer settings
│   │   │   └── components/
│   │   │       ├── ChannelCard.kt       # Channel list item with logo
│   │   │       ├── CategoryChip.kt      # Horizontal category filter chips
│   │   │       └── FallbackView.kt      # Animated error/retry screen
│   │   ├── MainActivity.kt        # NavHost + theme state
│   │   └── MiTVApplication.kt     # @HiltAndroidApp
│   └── res/
│       ├── values/strings.xml
│       ├── values/themes.xml
│       └── xml/network_security_config.xml
├── gradle/libs.versions.toml      # Version catalog
├── .github/workflows/
│   └── android_build.yml          # CI/CD → signed APK
└── README.md
```

---

## 🎨 Adding a Custom Theme

1. Add colors to `Color.kt`
2. Create a `ColorScheme` in `Theme.kt`
3. Add enum value to `AppTheme` in `Models.kt`
4. Map it in `getColorScheme()` and `MiTVTheme()`

---

## 📋 Adding M3U Playlists

1. Open the app → tap **+** in the top bar
2. Enter a name and your M3U URL (e.g. `http://yourserver.com/playlist.m3u`)
3. Channels load instantly, grouped by category

---

## 🐛 Troubleshooting

| Problem | Solution |
|---|---|
| Stream doesn't play | Check the URL is accessible; try toggling Auto-Retry in Settings |
| Logo not loading | Verify `tvg-logo` URL is reachable |
| App crashes on build | Make sure JDK 17 is selected in Android Studio |
| Slow channel list | Use a smaller M3U or increase buffer size in Settings |

---

## 📄 License

```
MIT License — MiTV Player
Free to use, modify, and distribute.
```
