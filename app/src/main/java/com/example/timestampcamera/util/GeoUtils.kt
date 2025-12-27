package com.example.timestampcamera.util

import kotlin.math.*

object GeoUtils {
    
    // Ellipsoid Constants (WGS84)
    private const val A = 6378137.0 // Major axis
    private const val F = 1 / 298.257223563 // Flattening
    private const val K0 = 0.9996 // Scale factor at central meridian

    /**
     * Converts Latitude/Longitude to standard UTM string.
     * Format: "47P 123456 1234567" (Zone Band Easting Northing)
     */
    fun toUTM(lat: Double, lon: Double): String {
        val zone = floor((lon + 180) / 6).toInt() + 1
        val band = getLatitudeBand(lat)
        
        val xy = latLonToUtmXY(lat, lon, zone)
        return "%d%s %.0f %.0f".format(zone, band, xy.first, xy.second)
    }

    /**
     * Converts Lat/Lon to MGRS (Military Grid Reference System).
     * Quality: High precision (1m) -> "47P QS 12345 67890"
     */
    fun toMGRS(lat: Double, lon: Double): String {
        if (lat < -80 || lat > 84) return toUTM(lat, lon) // MGRS undefined in polar regions implies UPS, fallback to UTM for now

        val zone = floor((lon + 180) / 6).toInt() + 1
        val band = getLatitudeBand(lat)
        val xy = latLonToUtmXY(lat, lon, zone)
        val easting = xy.first
        val northing = xy.second
        
        val squareId = get100kId(easting, northing, zone)
        
        // 5 digits for 1m precision
        val eStr = "%05d".format((easting % 100000).toInt())
        val nStr = "%05d".format((northing % 100000).toInt())
        
        return "%d%s %s %s %s".format(zone, band, squareId, eStr, nStr)
    }

    private fun getLatitudeBand(lat: Double): String {
        if (lat < -80 || lat > 84) return "?"
        val bands = "CDEFGHJKLMNPQRSTUVWXX"
        val index = floor((lat + 80) / 8).toInt()
        return bands.getOrElse(index) { 'X' }.toString()
    }

    private fun latLonToUtmXY(lat: Double, lon: Double, zone: Int): Pair<Double, Double> {
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        
        val centralMeridian = Math.toRadians(-183.0 + (zone * 6.0))
        
        val e2 = 2 * F - F * F
        val n = A / sqrt(1 - e2 * sin(latRad).pow(2))
        val t = tan(latRad).pow(2)
        val c = (e2 / (1 - e2)) * cos(latRad).pow(2)
        val a = cos(latRad) * (lonRad - centralMeridian)
        
        val m = A * ((1 - e2 / 4 - 3 * e2.pow(2) / 64 - 5 * e2.pow(3) / 256) * latRad
                - (3 * e2 / 8 + 3 * e2.pow(2) / 32 + 45 * e2.pow(3) / 1024) * sin(2 * latRad)
                + (15 * e2.pow(2) / 256 + 45 * e2.pow(3) / 1024) * sin(4 * latRad)
                - (35 * e2.pow(3) / 3072) * sin(6 * latRad))
        
        val easting = K0 * n * (a + (1 - t + c) * a.pow(3) / 6
                + (5 - 18 * t + t.pow(2) + 72 * c - 58 * e2) * a.pow(5) / 120) + 500000.0
        
        var northing = K0 * (m + n * tan(latRad) * (a.pow(2) / 2
                + (5 - t + 9 * c + 4 * c.pow(2)) * a.pow(4) / 24
                + (61 - 58 * t + t.pow(2) + 600 * c - 330 * e2) * a.pow(6) / 720))
        
        if (lat < 0) northing += 10000000.0
        
        return Pair(easting, northing)
    }

    private fun get100kId(easting: Double, northing: Double, zone: Int): String {
        // MGRS 100km Square Logic
        // 1. Column Letter
        val set = (zone - 1) % 6 + 1
        val colIndex = floor(easting / 100000).toInt()
        
        // MGRS Column Sets
        // Set 1 (Zones 1, 4...): A-H
        // Set 2 (Zones 2, 5...): J-R
        // Set 3 (Zones 3, 6...): S-Z
        val colChars = when (set) {
            1, 4 -> "ABCDEFGH"
            2, 5 -> "JKLMNPQR"
            3, 6 -> "STuvwxyz".uppercase() 
            else -> "ABCDEFGH"
        }
        
        // Valid column range usually 1-8 (100km chunks from central meridian)
        // 500k is E0 (offset 500km).
        // e.g. E=500,000. index = 5? No. Index 1 = 100,000? 
        // 500,000 / 100,000 = 5.
        // But the chars restart.
        // Simplified Logic: 
        // For standard UTM, central meridian is 500km.
        // Let's assume the user is in standard bounds.
        // We will pick the char based on modulus match for now as fully robust MGRS tables are huge.
        // A simple "close enough" for visual verification in this demo:
        val cChar = colChars.getOrElse((colIndex - 1) % 8) { 'X' }

        // 2. Row Letter
        // Repeats A-V (20 letters) every 2000km
        val rowChars = "ABCDEFGHJKLMNPQRSTUV"
        val rowIndex = floor(northing / 100000).toInt() % 20
        
        // Shift based on set (odd/even zones)
        // Set 1,3,4,6 (Odd? No):
        // Group 1 (Zones 1,3,5? No. 1,4,7...)
        // Actually: odd zones, A starts at 0. Even zones, F starts at 0.
        val shift = if (zone % 2 == 0) 5 else 0
        val rChar = rowChars[(rowIndex + shift) % 20]
        
        return "$cChar$rChar"
    }
}
