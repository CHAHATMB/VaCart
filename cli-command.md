# Android CLI Cheat Sheet

A comprehensive reference for building, debugging, and installing Android apps from the command line on connected devices.

---

## 1. Device Management (`adb`)

| Task | Command (PowerShell / CMD) | Description |
| :--- | :--- | :--- |
| **List Devices** | `adb devices` | Lists all connected physical devices and emulators. |
| **Target Specific Device** | `adb -s <DEVICE_ID> <command>` | Directs ADB commands to a specific device if multiple are connected. |
| **Restart ADB Server** | `adb kill-server && adb start-server` | Restarts the ADB daemon when connection issues occur. |
| **Wireless ADB (TCP/IP)** | `adb tcpip 5555`<br>`adb connect <DEVICE_IP>:5555` | Enables wireless debugging over your local Wi-Fi network. |
| **Disconnect Wireless** | `adb disconnect <DEVICE_IP>:5555` | Disconnects the wireless ADB session. |

---

## 2. Building the App (Gradle)

> **Note:** On Windows PowerShell / CMD, use `.\gradlew.bat`. On Linux / macOS / Git Bash, use `./gradlew`.

| Task | Command | Artifact Output |
| :--- | :--- | :--- |
| **Clean Project** | `.\gradlew.bat clean` | Cleans previous build caches and directories. |
| **Build Debug APK** | `.\gradlew.bat assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| **Build Release APK** | `.\gradlew.bat assembleRelease` | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| **Build App Bundle (.aab)** | `.\gradlew.bat bundleRelease` | `app/build/outputs/bundle/release/app-release.aab` |
| **Run Unit Tests** | `.\gradlew.bat test` | `app/build/reports/tests/` |
| **Run Connected Tests** | `.\gradlew.bat connectedAndroidTest` | Runs instrumentation tests on connected device. |

---

## 3. Install & Run on Connected Device

### Option A: Gradle All-in-One (Build + Install)

```powershell
# Build and install debug APK directly onto connected device
.\gradlew.bat installDebug

# Build and install release APK (requires signing config)
.\gradlew.bat installRelease

# Uninstall debug APK from device
.\gradlew.bat uninstallDebug
```

### Option B: ADB Direct Install

```powershell
# Install APK (overwrites existing app and preserves data with -r)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Install allowing test-only APKs (-t) and version downgrade (-d)
adb install -r -t -d app/build/outputs/apk/debug/app-debug.apk

# Uninstall app completely
adb uninstall com.example.yourpackagename
```

### Launch the App

```powershell
# Start MainActivity
adb shell am start -n com.example.yourpackagename/.MainActivity

# Force-stop and then launch
adb shell am start -S -n com.example.yourpackagename/.MainActivity
```

---

## 4. Debugging & Logs

### Logcat Monitoring

```powershell
# Stream logs filtered by package name (PowerShell)
adb logcat --pid=$(adb shell pidof -s com.example.yourpackagename)

# Stream logs with Tag filter and priority level (V: Verbose, D: Debug, I: Info, W: Warn, E: Error, F: Fatal, S: Silent)
adb logcat MyTag:D *:S

# Clear existing logcat buffer
adb logcat -c

# Dump crash stack traces
adb logcat -b crash
```

### Attach Debugger

```powershell
# Launch app and wait for debugger attachment
adb shell am start -D -n com.example.yourpackagename/.MainActivity
```

### App State & Diagnostics

```powershell
# Force-stop app process
adb shell am force-stop com.example.yourpackagename

# Clear all app data and cache (resets app to fresh install state)
adb shell pm clear com.example.yourpackagename

# Grant a runtime permission via CLI
adb shell pm grant com.example.yourpackagename android.permission.POST_NOTIFICATIONS

# Revoke a runtime permission
adb shell pm revoke com.example.yourpackagename android.permission.POST_NOTIFICATIONS
```

### Screen Capture & Recording

```powershell
# Take a screenshot and save locally
adb exec-out screencap -p > screenshot.png

# Record screen video (press Ctrl+C to stop)
adb shell screenrecord /sdcard/recording.mp4
adb pull /sdcard/recording.mp4 ./
adb shell rm /sdcard/recording.mp4
```

---

## 5. Useful Automation One-Liners

### Build, Install, and Launch:
```powershell
.\gradlew.bat installDebug; adb shell am start -n com.example.yourpackagename/.MainActivity
```

### Clean, Rebuild, Install, and Follow Logs:
```powershell
.\gradlew.bat clean installDebug; adb shell am start -n com.example.yourpackagename/.MainActivity; adb logcat --pid=$(adb shell pidof -s com.example.yourpackagename)
```
