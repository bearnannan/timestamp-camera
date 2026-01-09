package com.example.timestampcamera.util

import com.example.timestampcamera.data.AddressResolution
import com.example.timestampcamera.data.LocationData

object AddressFormatter {

    fun formatAddress(data: LocationData, resolution: AddressResolution): String {
        return when (resolution) {
            AddressResolution.NONE -> ""
            AddressResolution.COUNTRY -> data.country
            AddressResolution.PROVINCE -> data.province
            AddressResolution.DISTRICT -> data.district
            AddressResolution.STREET -> data.street
            AddressResolution.HOUSE_NO -> data.houseNumber
            AddressResolution.HOUSE_NO_STREET -> {
                joinParts(" ", data.houseNumber, data.street)
            }
            AddressResolution.FULL_ADDRESS_NO_ZIP -> {
                joinParts(" ", data.houseNumber, data.street, data.subDistrict, data.district, data.province)
            }
            AddressResolution.FULL_ADDRESS -> {
                val base = joinParts(" ", data.houseNumber, data.street, data.subDistrict, data.district, data.province, data.postalCode)
                smartSplit(base)
            }
            AddressResolution.FULL_ADDRESS_BREAK_ZIP -> {
                val base = joinParts(" ", data.houseNumber, data.street, data.subDistrict, data.district, data.province)
                if (data.postalCode.isNotBlank()) "$base\n${data.postalCode}" else base
            }
        }
    }

    private fun joinParts(separator: String, vararg parts: String): String {
        return parts.filter { it.isNotBlank() }.joinToString(separator)
    }

    private fun smartSplit(text: String): String {
        if (text.length <= 30) return text
        
        // 1. Try splitting at "Province" or "District" keywords if present? 
        // Or simply finding a comma if formatted with commas
        val commaIndex = text.indexOf(", ")
        if (commaIndex != -1 && commaIndex < 40) {
            return text.substring(0, commaIndex + 1) + "\n" + text.substring(commaIndex + 2)
        }
        
        // 2. Space splitting for Thai addresses
        // Try to finding a space near the middle (e.g. roughly chars 20-35)
        val mid = text.length / 2
        val range = (mid - 10).coerceAtLeast(10)..(mid + 10).coerceAtMost(text.length - 1)
        
        // Prefer splitting before district/subdistrict/province
        // But simply finding last space in the first 35 chars works too
        val spaceIndex = text.lastIndexOf(' ', 35)
        if (spaceIndex > 15) {
             return text.substring(0, spaceIndex) + "\n" + text.substring(spaceIndex + 1)
        }

        return text
    }
}
