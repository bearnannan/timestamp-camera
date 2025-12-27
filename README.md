# Timestamp Camera Pro (Replica)

A fully featured Android Camera application built with **Kotlin** and **Jetpack Compose**.
This app replicates the functionality of popular Timestamp Camera apps, offering advanced watermarking, video recording, and customization.

## Features

### 📸 Camera & Capture
- **Photo & Video Support**: Seamlessly switch between Photo and Video modes.
- **Zoom Control**: Smooth zoom slider and preset "Pill" controls (0.6x, 1x, 2x, etc.).
- **Exposure Control**: Real-time EV slider with professional UI.
- **Flash Modes**: Auto, On, Off, Torch.
- **Camera Switching**: Front and Back camera support.
- **Gallery Integration**: Built-in gallery viewer with delete functionality.

### 📍 Overlay & Watermarking
- **Dynamic Timestamp**: Customizable date/time formats (Thai/English).
- **Visual Themes**: "Modern" (Gold Gradient/Glassy) and "Minimal" (Clean Stroke) layouts.
- **Location Data**: GPS coordinates (Decimal/DMS/UTM/MGRS), Altitude, Speed.
- **Compass HUD**: Professional HUD-style compass tape with gradient visualization.
- **Custom Logo**: Import transparent PNG logos for branding.
- **Custom Text**: Add project name, inspector name, notes, and tags.

### 🛠 Customization & Settings
- **Battery Saver**: "Black Screen" mode for long video recordings (double-tap to wake).
- **Import Mode**: Apply watermarks to existing photos from your gallery.
- **Resolution Control**: User-selectable photo and video resolutions (4K, FHD, HD).
- **Aspect Ratio**: 4:3, 16:9, 1:1 support.
- **Text Styling**: Custom colors, stroke, text size, and shadows.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material3)
- **Camera**: CameraX (LifecycleCameraController)
- **Location**: Android LocationManager (GPS)
- **Persistence**: DataStore Preferences
- **Async**: Coroutines & Flow

## Setup & Build
1. Open the project in **Android Studio Ladybug** (or newer).
2. Sync Gradle dependencies.
3. Build and Run on a physical device (Camera features require hardware).
4. Grant Camera, Microphone, and Location permissions when prompted.

## Architecture
- **MVVM Pattern**:
  - `CameraViewModel`: Manages camera state, settings, and business logic.
  - `SettingsRepository`: Persists user preferences via DataStore.
  - `OverlayUtils`: Handles drawing of timestamp, map, and graphical elements on bitmaps.
- **UI Components**:
  - `CameraScreen`: Main UI orchestrator.
  - `CameraPreview`: Wraps `PreviewView`.
  - `SettingsBottomSheet`: Comprehensive settings configuration.
  - `BatterySaverOverlay`: Power-saving lock screen logic.

## License
MIT License
