# 🌾 Hay Day Bot — Android APK

A **no-root** Hay Day automation bot for Android using:
- **AccessibilityService** → inject taps & swipes
- **MediaProjection** → live screen capture  
- **OpenCV** → template matching to find game elements

---

## 📱 Features

| Task | What it does |
|------|-------------|
| 🌽 Crops | Harvest ready plots, re-plant with priority crop list |
| 🐄 Animals | Collect products, feed hungry animals |
| 🏭 Factory | Collect finished goods, auto-requeue production |
| 📦 Orders | Fill truck deliveries, restock roadside shop |
| 🛡️ Anti-ban | Random delays, tap jitter, periodic idle scrolling |

---

## 🛠️ Build Instructions

### Requirements
- **Android Studio Hedgehog or newer** (free at developer.android.com)
- **JDK 17** (bundled with Android Studio)
- Android SDK 34

### Steps
1. Open Android Studio
2. **File → Open** → select the `HayDayBot/` folder
3. Wait for Gradle sync to complete (~2 min first time)
4. **Build → Build Bundle(s)/APK(s) → Build APK(s)**
5. APK appears at: `app/build/outputs/apk/debug/app-debug.apk`
6. Transfer to your phone and install

> Enable **"Install from unknown sources"** in Android Settings → Security

---

## 📲 First-Time Setup on Phone

### Step 1 — Grant Permissions
Open the app. Three orange buttons will appear:

| Button | Where to go |
|--------|-------------|
| Accessibility | Settings → Accessibility → Hay Day Bot → ON |
| Overlay | Settings → Apps → Hay Day Bot → Display over other apps → ON |
| Screenshot | Tap button → Allow in the popup |

All three turn **green** when granted.

### Step 2 — Add Templates
The bot recognises game elements by comparing small screenshot crops.
You must capture these from YOUR device (pixel coordinates vary by screen size).

**Using the Python bot's capture tool:**
```bash
# Install the companion Python bot first (see haydaybot.zip)
python main.py --capture          # saves logs/capture.png
python main.py --crop crops harvest_ready 120 340 48 48
```

Then copy the `templates/` folder to your phone:
```
Phone storage: /sdcard/Android/data/com.haydaybot/files/templates/
```
The app reads templates from this folder at runtime.

**Minimum required templates:**

| Folder | File | What to crop |
|--------|------|-------------|
| crops | harvest_ready.png | Golden glow on ready plot |
| crops | plot_empty.png | Bare tilled soil |
| crops | seed_wheat.png | Wheat seed icon |
| crops | harvest_btn.png | Green harvest button |
| animals | feed_bubble.png | Speech bubble above hungry animal |
| animals | collect_ready.png | Glowing product above animal |
| animals | feed_btn.png | Blue feed button |
| factory | bakery_done.png | Finished bakery indicator |
| factory | bakery_make.png | Ready-to-produce bakery icon |
| factory | bread.png | Bread product icon |
| orders | order_board.png | Brown order board |
| orders | order_slot.png | Single order card |
| orders | order_fill_btn.png | Green deliver button |

### Step 3 — Run the Bot
1. Open Hay Day
2. Switch back to Hay Day Bot app
3. Select which tasks to enable (toggle switches)
4. Tap **▶ Start Bot**
5. Switch back to Hay Day — the bot runs in background

A persistent notification appears. Tap **Stop** to end the session.

---

## 📁 Project Structure

```
HayDayBot/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/haydaybot/
│   │   ├── ui/
│   │   │   └── MainActivity.kt        ← Permission + Start/Stop UI
│   │   ├── service/
│   │   │   ├── BotAccessibilityService.kt  ← Core scheduler
│   │   │   ├── BotForegroundService.kt     ← Keeps app alive
│   │   │   ├── ScreenshotService.kt        ← MediaProjection capture
│   │   │   ├── InputController.kt          ← Gesture injection
│   │   │   └── VisionEngine.kt             ← OpenCV matching
│   │   ├── tasks/
│   │   │   ├── BaseTask.kt
│   │   │   ├── CropTask.kt
│   │   │   ├── AnimalTask.kt
│   │   │   ├── FactoryTask.kt
│   │   │   └── OrdersTask.kt
│   │   └── utils/
│   │       ├── BotConfig.kt           ← All settings
│   │       ├── BotLogger.kt           ← Live log flow
│   │       └── BotStats.kt            ← Live counters
│   └── res/
│       ├── layout/activity_main.xml
│       ├── xml/accessibility_service_config.xml
│       └── values/
├── build.gradle
└── settings.gradle
```

---

## ⚙️ Customising Settings

Edit `BotConfig.kt` before building to change defaults:

```kotlin
val cropList = listOf("wheat", "corn", "soybean")  // crop priority
val actionDelayMinMs = 400L   // min ms between taps
val cropCycleMs = 300_000L    // check crops every 5 min
```

---

> ⚠️ **Disclaimer**: Using bots violates Supercell's Terms of Service.
> Use at your own risk on a secondary/test account.
