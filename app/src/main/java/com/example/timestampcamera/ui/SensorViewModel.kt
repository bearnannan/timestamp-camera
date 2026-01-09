package com.example.timestampcamera.ui

import android.app.Application
import android.content.Context
import android.view.WindowManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2

/**
 * SensorViewModel handles device orientation tracking using hardware sensors.
 * It provides:
 * 1. horizonRotation: For water-level indicators (smooth 0-360 rotation)
 * 2. uiRotation: Discrete steps (0, 90, 180, 270) for icon orientation
 */
class SensorViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    // Prefer Rotation Vector for accuracy, fallback to Accelerometer
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // For Level Indicator (smooth rotation)
    private val _horizonRotation = MutableStateFlow(0f)
    val horizonRotation: StateFlow<Float> = _horizonRotation.asStateFlow()
    
    // For UI Icons (discrete rotation)
    private val _uiRotation = MutableStateFlow(0f)
    val uiRotation: StateFlow<Float> = _uiRotation.asStateFlow()
    
    // For Compass (0-360 degrees)
    private val _compassHeading = MutableStateFlow(0f)
    val compassHeading: StateFlow<Float> = _compassHeading.asStateFlow()

    // For Virtual Leveler (Pitch: Up/Down, Roll: Left/Right)
    // Pitch: -90 to +90 (0 is level)
    // Roll: -180 to +180 (0 is level)
    private val _devicePitch = MutableStateFlow(0f)
    val devicePitch: StateFlow<Float> = _devicePitch.asStateFlow()
    
    private val _deviceRoll = MutableStateFlow(0f)
    val deviceRoll: StateFlow<Float> = _deviceRoll.asStateFlow()
    
    private var currentFilteredAzimuth = 0f

    // Low-Pass Filter alpha (0.0 to 1.0). Lower is smoother but slower.
    private val alpha = 0.1f // Increased smoothness for "premium" feel
    private var currentFilteredAngle = 0f
    
    // Reused arrays for GC optimization
    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    init {
        startTracking()
    }

    fun startTracking() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) // Optimized for UI smoothness and battery
        }
    }

    fun stopTracking() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Unlikely fallback, but basic support
            // Gravity vector logic is complex without Magnetometer, simplified to basic Roll
             val x = event.values[0]
             val y = event.values[1]
             val z = event.values[2]
             val angle = -Math.toDegrees(atan2(x.toDouble(), y.toDouble())).toFloat()
             updateAngles(angle)
             return
        }

        // Remap coordinate system to device display
        // Since we locked Activity to Portrait, we use default axis
        // But let's be robust and map X/Y to screen logic roughly
        
        // Calculate orientation
        // We use Z-axis rotation for "Level" check in flat lay, but for camera holding
        // X and Y gravity components are most important.
        
        // Robust approach: Extract "Roll" regardless of Pitch
        // Using atan2(x, y) from gravity vector is most reliable for 2D UI rotation
        // Extract gravity vector from Rotation Matrix (last row: indices 6, 7, 8)
        // R * [0, 0, 1] = Gravity Vector relative to device
        // But strictly: R maps Device -> World. 
        // We want World Gravity (0,0,-1) mapped to Device.
        // It's the Transpose (Inverse) of R. 
        // Row 2 of R (0-indexed) is Z-axis of Device in World. 
        // We want Gravity in Device coordinates.
        
        // Actually simpler: 
        // SensorManager.getOrientation returns Azimuth, Pitch, Roll.
        // Roll is usually what we want for landscape/portrait detection.
        
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        
        // roll is orientationAngles[2] in radians (-PI to PI)
        // pitch is orientationAngles[1]
        
        // However, standard Roll has gimbal lock issues at 90 pitch.
        // A better approach for Camera UI:
        // angle = atan2(-x_gravity, y_gravity)
        
        // Get gravity vector from Rotation Matrix
        // If R transforms Device to World, then R_transpose * World_Gravity = Device_Gravity.
        // World Gravity is [0, 0, 1] (or -1 depending on convention).
        // So we want the 3rd row of R (elements 6, 7, 8) if R is row-major?
        // SensorManager.getRotationMatrixFromVector returns:
        //  /  R[ 0]   R[ 1]   R[ 2]  \
        // |   R[ 3]   R[ 4]   R[ 5]  |
        //  \  R[ 6]   R[ 7]   R[ 8]  /
        //
        // This matrix transforms a vector from the device coordinate system to the world's coordinate system.
        // We want the Gravity vector (0, 0, g) in Device coordinates.
        // v_device = R^T * v_world.
        // v_world = [0, 0, 1] (roughly).
        // v_device = [ R[0] R[3] R[6] ] * [0]
        //            [ R[1] R[4] R[7] ]   [0]
        //            [ R[2] R[5] R[8] ]   [1]
        // So v_device = [ R[6], R[7], R[8] ].
        
        val gX = rotationMatrix[6] // Gravity X component
        val gY = rotationMatrix[7] // Gravity Y component
        // val gZ = rotationMatrix[8]
        
        // Calculate angle of gravity vector in X-Y plane (Screen plane)
        // atan2(y, x) -> returns angle from X axis.
        // We want angle from "Down" (Y axis).
        // vector is (gX, gY).
        val angleRad = atan2(gX.toDouble(), gY.toDouble())
        var angleDeg = Math.toDegrees(angleRad).toFloat()
        
        // Revert sign because UI rotation is usually opposite to sensor rotation
        // If device tilts right (positive roll), UI must rotate left (negative) to stay level.
        // But angleDeg here represents the direction of "Down" on the screen.
        // If device is upright, Down is +Y (angle 0).
        // If device is rotated 90 deg CCW (Landscape Left), Down is +X (angle 90).
        // We want the UI to counteract this.
        
        // Normalize to 0..360 for easier interpolation
        // However, for shortest path, -180..180 is handled by custom logic.
        
        
        // Update Compass Heading
        // Azimuth is orientationAngles[0]
        val azimuthRad = orientationAngles[0]
        var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
        // Convert -180..180 to 0..360
        if (azimuthDeg < 0) azimuthDeg += 360f
        // Update Compass Heading, Pitch, and Roll
        updateOrientation(orientationAngles)

        updateAngles(-angleDeg) // Invert for visual correction
    }
    
    private fun updateCompass(targetAzimuth: Float) {
        // Smooth Compass (LPF with Shortest Path)
        var diff = targetAzimuth - currentFilteredAzimuth
        while (diff < -180) diff += 360
        while (diff > 180) diff -= 360
        
        currentFilteredAzimuth += diff * alpha
        
        // Normalize 0-360
        val heading = (currentFilteredAzimuth + 360) % 360
        _compassHeading.value = heading
    }
    
    private fun updateOrientation(orientation: FloatArray) {
        // orientation[0] = Azimuth
        // orientation[1] = Pitch
        // orientation[2] = Roll
        
        val azimuth = (Math.toDegrees(orientation[0].toDouble()) + 360) % 360
        updateCompass(azimuth.toFloat())
        
        val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()
        
        // Low-pass filter
        val alpha = 0.1f
        _devicePitch.value = _devicePitch.value + alpha * (pitchDeg - _devicePitch.value)
        _deviceRoll.value = _deviceRoll.value + alpha * (rollDeg - _deviceRoll.value)
    }

    private fun updateAngles(targetAngle: Float) {
        // 1. Smooth Horizon Rotation (Low Pass Filter with Shortest Path)
        var diff = targetAngle - currentFilteredAngle
        
        // Wrap difference to -180..180 for shortest path
        while (diff < -180) diff += 360
        while (diff > 180) diff -= 360
        
        // Apply LPF
        currentFilteredAngle += diff * alpha
        _horizonRotation.value = currentFilteredAngle
        
        // 2. Discrete UI Rotation (Snap to 0, 90, 180, 270)
        // targetAngle is -atan2(gX, gY), where:
        // Upright Portrait: gX ~ 0, gY ~ 9.8 -> angle 0
        // Landscape Left: gX ~ 9.8, gY ~ 0 -> angle -90
        // Landscape Right: gX ~ -9.8, gY ~ 0 -> angle 90
        // Upside Down: gX ~ 0, gY ~ -9.8 -> angle 180
        
        val absAngle = abs(targetAngle)
        val orientation = when {
            absAngle < 45 -> 0f              // Upright
            targetAngle < -45 && targetAngle > -135 -> -90f // Landscape (Home button on right)
            targetAngle > 45 && targetAngle < 135 -> 90f    // Landscape (Home button on left)
            absAngle > 135 -> 180f           // Upside Down
            else -> _uiRotation.value        // Keep previous if in dead zone
        }
        
        if (_uiRotation.value != orientation) {
            _uiRotation.value = orientation
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
