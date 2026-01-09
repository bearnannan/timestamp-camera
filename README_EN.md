# Timestamp Camera Pro (Replica)

> **📘 User Manual**: [Click here to read the detailed User Manual](USER_MANUAL_EN.md)

A fully-featured Android camera application built with **Kotlin** and **Jetpack Compose**.
This app replicates the functionality of popular Timestamp Camera apps, offering advanced watermarking, video recording, and extensive customization.

## Features

### 📸 Camera & Capture
- **Photo & Video Support**: Seamlessly switch between photo and video modes, including **Video Watermarking** (direct embedding of watermarks into videos).
- **Smart Zoom Pills**: Fast zoom shortcuts adaptable to the device's actual lenses (e.g., Ultrawide 0.6x) and standard focal lengths (1x, 2x, 5x), complete with a precision slider for maximum control.
- **Tap to Focus**: Spot Metering and focus by tapping the screen, featuring a status ring indicator.
- **Exposure Control**: Real-time EV adjustment (-2 to +2) with a professional UI.
- **Volume Key Shutter**: Use volume up/down keys to capture photos or start/stop video recording instantly—perfect for one-handed operation.
- **Flash Modes**: Auto, On, Off, Torch (Flashlight), and a white screen flash effect for the front camera.
- **Camera Switch**: Supports both front and rear cameras (with an option to mirror the front camera).
- **Camera Extensions**:
    - **Smart Priority**: Automatically selects the best mode (e.g., uses Night Mode in low light).
    - **Manual Selection**: Manually choose modes (Auto, HDR, Night, etc.).
    - **Standard Mode**: Disables all extensions for the most natural image.
- **Professional UI**: A pro-level interface with solid black controls at the top and bottom for clear visibility and focus on the subject, featuring **360-Degree UI Rotation** that seamlessly rotates icons and menus based on device orientation (Portrait/Landscape/Upside Down).
- **Built-in Gallery**: Instantly view captured photos and videos with delete functionality.

### ☁️ Cloud & Backup
- **Google Drive Auto-Upload**: Link your Google Drive account to automatically upload photos and videos.
- **Smart Folder Organizer**: Automatically creates folders based on capture date for better organization.
- **Robust Retry Mechanism**: A stable backup system with automatic retry (Exponential Backoff) when network failures occur, ensuring no photo is lost.

### 🖼️ Editor Mode
- **Import Image**: Import existing photos from an external gallery to apply watermarks retroactively.
- **High-Resolution Render**: Maintains the original resolution of the imported image (no quality loss).
- **Watermark Reset**: Watermarks in editor mode automatically update to the current time or use the original image time (configurable).

### 📍 Overlay & Watermarking
- **Camera Info Overlay (New)**: Real-time information display on the camera screen.
    - Shows Date/Time, GPS Coordinates, Detailed Address, Altitude, and Speed.
    - **Smart Address Parsing**: Automatically separates Street, District, City, and Province.
    - **Professional Fields**: Dedicated fields for Project Name, Inspector Name, and Tags/Notes.
    - **Toggle Visibility**: Choose to show or hide Address and Coordinates in settings.
- **Compass Overlay (New)**:
    - **Real-time Compass**: Uses Sensor Fusion (Accelerometer + Magnetometer) for accuracy and smoothness.
    - **Customizable Position**: Choose from 9 placement positions (Top-Left, Center, Bottom-Right, etc.).
    - **Realistic Graphics**: The dial and needle rotate according to real direction with degree indicators, designed for **Perfect Balance** to avoid clutter.
    - **Sync Text Size**: Direction text (N, E, S, W) automatically adjusts for readability and matches perfectly between preview and saved watermark.
- **Dynamic Timestamp**: Customizable date/time formats with options for **Language (English/Thai)** and **Era (AD/BE)** in settings.
- **Themes**: "Modern" (Gold gradient/Glass) and "Minimal" (Clean lines).
- **Coordinate Data**: Supports Decimal, DMS, UTM, MGRS, Altitude, and Speed formats.
- **Custom Logo**: Import transparent PNG files to use as brand logos.

### 🛠 Customization & Settings
- **Project & Workflow**:
    - **Data Management**: Project Name, Inspector, Notes, and Tags.
    - **History**: Remembers last entered values for faster workflow.
    - **Custom Fields**: Unlimited custom data fields.
- **Watermark Design**:
    - **Templates**: Modern Pro, Minimal, Classic.
    - **Logo**: Support for custom logo import.
    - **Fonts**: Various font choices (Roboto, Oswald, Inter, etc.).
    - **Text Style**: Adjust color, size, bold, and shadow.
    - **Position**: Select watermark position from 9 anchor points.
- **Display Content**:
    - **Date/Time**: Adjust date format and Era (AD/BE).
    - **Address & Coordinates**: Show/Hide Address, GPS (DMS/UTM/MGRS), and Map.
    - **Insights**: Compass, Altitude, and Speed.
- **Camera Config**:
    - **Resolution**: Select photo resolution and aspect ratio (4:3, 16:9, 1:1).
    - **Assistive Tools**: Grid, Horizon Level, and Timer (3s/10s).
    - **Power Saving**: Black Screen Mode for long video recording (tap to wake).
    - **Storage**: Choose to save to Album or specific folder.
    - **Background Processing**: Background processing system allows continuous shooting without waiting for watermarking to finish.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material3)
- **Camera**: CameraX 1.4.1 (LifecycleCameraController, OverlayEffect)
- **Location**: Android LocationManager (GPS) with Geocoder
- **Sensors**: Android SensorManager (Accelerometer + Magnetometer)
- **Storage**: DataStore Preferences
- **Async**: Coroutines & Flow
- **Cloud Integration**: Google Drive API V3 (for backup)

## Setup & Build
1. Open project in **Android Studio Ladybug** (or newer).
2. Sync Gradle dependencies.
3. Build and Run on a real device (Camera features require hardware).
4. Grant Camera, Microphone, and Location permissions when prompted.
5. **Compass Calibration**: Move device in a figure-8 motion for maximum accuracy.

## Architecture
- **MVVM Pattern**:
  - `CameraViewModel`: Manages camera state, settings, sensor logic, and business logic.
  - `SettingsRepository`: Manages user settings via DataStore.
  - `OverlayUtils`: Handles drawing watermarks on images.
  - `VideoWatermarkUtils`: Handles embedding watermarks into videos using CameraX OverlayEffect.
  - `WatermarkDrawer`: Central logic for drawing watermarks (shared between photo and video).
  - `CameraInfoOverlay`: Composable for displaying info on the camera screen.
  - `CompassManager`: Sensor Fusion logic for the compass.
  - `DriveRepository`: Manages connection and file upload to Google Drive.
- **UI Components**:
  - `CameraScreen`: Main application screen.
  - `CameraPreview`: Wrapper for `PreviewView`.
  - `SettingsBottomSheet`: Comprehensive settings screen.
  - `CompassOverlay`: Compass rendering using Canvas.

## License
MIT License

## For Developers
This project has clear rules and guidelines for AI Agents and developers to maintain code quality and consistency.

### 📜 Rules
Please read and strictly follow these files:
- **[Project Rules](.agent/rules)**: Folder containing all rules.
  - [Android Library Standards](.agent/rules/android-library-standards.md): Library standards (Hilt, Retrofit, Coil, etc.).
  - [Android Style Guide](.agent/rules/android-style-guide.md): Coding guidelines (Kotlin, Jetpack Compose, MVVM).
  - [Code Import Safety](.agent/rules/code-import-safety.md): Import safety and AndroidX usage.
  - [Gradle Build Stability](.agent/rules/gradle-build-stability.md): Gradle configuration for stability.

### 🚀 Workflows
Standard workflows for common tasks:
- **[Workflows Directory](.agent/workflows)**
  - `/add-api-endpoint`: Add a new API connection.
  - `/fix-build-error`: Analyze and fix build errors.
  - `/generate-android-feature`: Generate a full feature (UI + Logic).
  - `/setup-robust-gradle`: Setup robust build.gradle.kts.
