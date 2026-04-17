# 🔑 MiTV Player — Complete Setup Guide

## Step 1: Import into Android Studio

1. Open **Android Studio** (Ladybug 2024.2.1+)
2. Click **File → Open**
3. Select the `MiTV_Player` folder
4. Wait for Gradle sync to complete (~2-5 min first time)
5. Press **▶ Run** to build and install on your device

---

## Step 2: Generate a Release Keystore

Open a terminal and run:

```bash
keytool -genkeypair \
  -alias mitv-release-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore release.keystore \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=MiTV Player, OU=Dev, O=MiTV, L=City, S=State, C=US"
```

**Save these values — you'll need them for GitHub Secrets:**
- Keystore file: `release.keystore`
- Store password: `YOUR_STORE_PASSWORD`
- Key alias: `mitv-release-key`
- Key password: `YOUR_KEY_PASSWORD`

---

## Step 3: Push to GitHub

```bash
cd MiTV_Player
git init
git add .
git commit -m "feat: initial MiTV Player project"
git remote add origin https://github.com/YOUR_USERNAME/MiTV_Player.git
git push -u origin main
```

---

## Step 4: Add GitHub Secrets

Go to your repo on GitHub:
**Settings → Secrets and variables → Actions → New repository secret**

Add these 4 secrets:

| Secret Name | Value |
|---|---|
| `KEYSTORE_BASE64` | Base64 of your keystore (see below) |
| `KEYSTORE_PASSWORD` | Your store password |
| `KEY_ALIAS` | `mitv-release-key` |
| `KEY_PASSWORD` | Your key password |

**How to get KEYSTORE_BASE64:**
```bash
# macOS
base64 -i release.keystore | pbcopy   # copies to clipboard

# Linux
base64 -w 0 release.keystore

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))
```

---

## Step 5: Trigger a Build

Option A — Push to main (auto-triggers):
```bash
git push origin main
```

Option B — Manual trigger:
1. Go to **Actions** tab in your GitHub repo
2. Click **🚀 MiTV Player — Build & Sign APK**
3. Click **Run workflow**
4. Select `release`
5. Click **Run workflow**

---

## Step 6: Download Your APK

1. Go to **Actions** tab
2. Click the latest workflow run
3. Scroll to **Artifacts**
4. Download `MiTV-Player-release-XXXXXXXX`
5. Install on your Android device

> **Enable "Install from unknown sources"** in your device settings if prompted.

---

## 📋 Adding Your First M3U Playlist

1. Open **MiTV Player**
2. Tap **+** in the top bar
3. Enter a name (e.g. "My IPTV")
4. Paste your M3U URL (e.g. `http://yourserver.com/get.php?...`)
5. Tap **Add**
6. Channels load instantly, grouped by category

---

## 🎨 Customizing Themes

The app ships with 4 themes. To add your own:

1. Open `app/src/main/java/com/mitv/player/ui/theme/Color.kt`
2. Add your color palette
3. Open `Theme.kt` and add a new `ColorScheme`
4. Add a new entry to `AppTheme` enum in `Models.kt`

---

## 🐛 Common Issues

### Build fails: "SDK not found"
- Open `local.properties` and add:
  ```
  sdk.dir=/path/to/your/Android/sdk
  ```

### Streams not loading
- Check that `android:usesCleartextTraffic="true"` is in `AndroidManifest.xml`
- Verify the M3U URL is reachable from your network

### Hilt injection error
- Make sure `MiTVApplication` is referenced in `AndroidManifest.xml` as `android:name=".MiTVApplication"`

### Room migration error
- The app uses `fallbackToDestructiveMigration()` — uninstall and reinstall if DB schema changed
