package com.example.timestampcamera.data

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import com.example.timestampcamera.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.IOException
import java.util.Locale

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val speed: Float = 0.0f,
    val address: String = "",
    val houseNumber: String = "",
    val street: String = "",
    val subDistrict: String = "",
    val district: String = "",
    val province: String = "",
    val country: String = "",
    val postalCode: String = ""
)

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<LocationData> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(10000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.lastOrNull()?.let { location ->
                    val address = getAddressFromLocation(location)
                    trySend(
                        LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude,
                            speed = location.speed,
                            address = address.addressLine,
                            houseNumber = address.houseNumber,
                            street = address.street,
                            subDistrict = address.subDistrict,
                            district = address.district,
                            province = address.province,
                            country = address.country,
                            postalCode = address.postalCode
                        )
                    )
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    fun getCompassUpdates(): Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        val accelerometerReading = FloatArray(3)
        val magnetometerReading = FloatArray(3)
        
        val sensorEventListener = object : SensorEventListener {
            // Low-pass filter factor (smaller = more smoothing/latency)
            val alpha = 0.05f 

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Apply Low-Pass Filter to Accelerometer
                    accelerometerReading[0] = alpha * event.values[0] + (1 - alpha) * accelerometerReading[0]
                    accelerometerReading[1] = alpha * event.values[1] + (1 - alpha) * accelerometerReading[1]
                    accelerometerReading[2] = alpha * event.values[2] + (1 - alpha) * accelerometerReading[2]
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    // Apply Low-Pass Filter to Magnetometer
                    magnetometerReading[0] = alpha * event.values[0] + (1 - alpha) * magnetometerReading[0]
                    magnetometerReading[1] = alpha * event.values[1] + (1 - alpha) * magnetometerReading[1]
                    magnetometerReading[2] = alpha * event.values[2] + (1 - alpha) * magnetometerReading[2]
                }
                
                val rotationMatrix = FloatArray(9)
                val orientationAngles = FloatArray(3)
                
                if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuthInRadians = orientationAngles[0]
                    val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                    // Normalize to 0-360
                    val azimuth = (azimuthInDegrees + 360) % 360
                    trySend(azimuth)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
        }
        
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        
        awaitClose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? {
        return try {
            val location = com.google.android.gms.tasks.Tasks.await(fusedLocationClient.lastLocation)
            location
        } catch (e: Exception) {
            null
        }
    }

    data class AddressComponents(
        val addressLine: String = "",
        val houseNumber: String = "",
        val street: String = "",
        val subDistrict: String = "",
        val district: String = "",
        val province: String = "",
        val country: String = "",
        val postalCode: String = ""
    )

    private fun getAddressFromLocation(location: Location): AddressComponents {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val houseNumber = address.featureName ?: ""
                val street = address.thoroughfare ?: ""
                val subDistrict = address.subLocality ?: "" // Sub-district/Tambon
                val district = address.subAdminArea ?: "" // District/Amphoe
                val province = address.adminArea ?: "" // Province/Changwat
                val country = address.countryName ?: ""
                val postalCode = address.postalCode ?: ""
                
                // Construct full address line for fallback
                val parts = listOf(houseNumber, street, subDistrict, district, province, postalCode).filter { it.isNotEmpty() }
                val fullAddress = parts.joinToString(", ")
                
                return AddressComponents(fullAddress, houseNumber, street, subDistrict, district, province, country, postalCode)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
             e.printStackTrace()
        }
        return AddressComponents(addressLine = context.getString(R.string.unknown_location))
    }

    companion object {
        fun toDMS(latitude: Double, longitude: Double): String {
            return "${convertCoordinate(latitude, true)} ${convertCoordinate(longitude, false)}"
        }

        private fun convertCoordinate(coordinate: Double, isLatitude: Boolean): String {
            val absCoord = kotlin.math.abs(coordinate)
            val degrees = absCoord.toInt()
            val minutesDecimal = (absCoord - degrees) * 60
            val minutes = minutesDecimal.toInt()
            val seconds = (minutesDecimal - minutes) * 60

            val direction = if (isLatitude) {
                if (coordinate >= 0) "N" else "S"
            } else {
                if (coordinate >= 0) "E" else "W"
            }

            return String.format(Locale.US, "%d°%02d'%02.0f\"%s", degrees, minutes, seconds, direction)
        }
    }
}
