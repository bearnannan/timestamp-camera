package com.example.timestampcamera.util

import android.location.Address
import com.example.timestampcamera.data.LocationFormat

object AddressFormatter {

    fun formatAddress(address: Address?, format: LocationFormat): String {
        if (address == null) return ""
        
        return when (format) {
            LocationFormat.FULL_ADDRESS -> {
                // Combine all lines
                (0..address.maxAddressLineIndex).joinToString(", ") { address.getAddressLine(it) }
            }
            LocationFormat.SHORT_ADDRESS -> {
                // Try to construct a shorter version: Thoroughfare, SubAdmin, Admin
                val parts = mutableListOf<String>()
                if (!address.thoroughfare.isNullOrEmpty()) parts.add(address.thoroughfare)
                else if (!address.featureName.isNullOrEmpty()) parts.add(address.featureName) // Sometimes feature name is house number/place
                
                if (!address.subLocality.isNullOrEmpty()) parts.add(address.subLocality)
                if (!address.locality.isNullOrEmpty()) parts.add(address.locality)
                if (!address.adminArea.isNullOrEmpty()) parts.add(address.adminArea)
                
                if (parts.isEmpty()) {
                    // Fallback to first line if parsed fields are empty
                    address.getAddressLine(0) ?: ""
                } else {
                    parts.joinToString(" ")
                }
            }
            LocationFormat.CITY_ONLY -> {
                val parts = mutableListOf<String>()
                if (!address.locality.isNullOrEmpty()) parts.add(address.locality)
                else if (!address.subAdminArea.isNullOrEmpty()) parts.add(address.subAdminArea)
                
                if (!address.adminArea.isNullOrEmpty()) parts.add(address.adminArea)
                
                parts.joinToString(", ")
            }
            LocationFormat.LAT_LON_ONLY -> {
                // This might be handled by GPS logic, but if requested here:
                ""
            }
            LocationFormat.NONE -> ""
        }
    }
}
