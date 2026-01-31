package com.example.timestampcamera.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

object FileUtils {
    /**
     * Sanitizes a string for use as a folder name.
     * Replaces any character that is NOT a letter, number, Thai character (\u0E00-\u0E7F), space, or underscore with '_'.
     * If the result is empty or blank, returns a default name based on the current date.
     */
    fun sanitizeFolderName(input: String): String {
        // Regex to match allowed characters:
        // [a-zA-Z0-9] : alphanumeric
        // \u0E00-\u0E7F : Thai characters
        // \s : whitespace
        // _ : underscore
        // - : hyphen
        val allowedPattern = Regex("[^a-zA-Z0-9\\u0E00-\\u0E7F\\s_\\-]")
        
        val sanitized = input.replace(allowedPattern, "_").trim()
        
        return if (sanitized.isBlank()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            dateFormat.format(Date())
        } else {
            // Trim leading/trailing spaces/underscores for cleaner look
            sanitized.trim { it <= ' ' || it == '_' }
        }
    }
}
