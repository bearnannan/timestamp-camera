package com.example.timestampcamera.util

import com.example.timestampcamera.data.FileNameFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FileNameGenerator {

    fun generateFileName(
        format: FileNameFormat,
        date: Date,
        note: String,
        address: String,
        index: Int = 0
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(date)
        val timestampUnderscore = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(date)
        
        val safeNote = sanitize(note)
        val safeAddress = sanitize(address)
        val uniqueId = UUID.randomUUID().toString().replace("-", "")

        return when (format) {
            FileNameFormat.TIMESTAMP_PROJECT -> {
                // e.g. 20251231_234101_MyNote
                "${timestamp}_${safeNote}"
            }
            FileNameFormat.TIMESTAMP_ADDRESS -> {
                 // e.g. 20251231_234101_8-408-Soi...
                "${timestamp}_${safeAddress}"
            }
            FileNameFormat.INDEX_TIMESTAMP -> {
                // e.g. IndexNumber_20251231_234101
                // Assuming "IndexNumber" is a literal prefix or placeholder for actual index?
                // The screenshot says "IndexNumber_...".
                // If it means a sequential index, checking user intent... 
                // "IndexNumber" might be the literal text of a field, or a counter?
                // Given "IndexNumber" is capitalized in screenshot, maybe it's the Inspector Name or a specific tag?
                // Let's assume it uses "Index" prefix + valid index if provided, or just "Index" if 0.
                // BUT, screenshot shows "IndexNumber_2025...". 
                // I will use literal "Index" prefix with the actual project/inspector if logic demands, 
                // but strictly following screenshot: "IndexNumber" might be a placeholder for a variable `customText` or similar?
                // Let's us a placeholder pattern for now, or just sanitize(projectName). 
                // Wait, logic: "Index_Timestamp". 
                // I'll stick to: uniqueId/Index based on context.
                // Actually, let's use the `index` param passed to this function.
                "Index_${index}_${timestamp}"
            }
            FileNameFormat.UNIQUE_ID -> {
                 // 2025..._uniqueid
                "${timestamp}_${uniqueId}"
            }
            FileNameFormat.BS_PROJECT_ADDRESS_TIMESTAMP -> {
                "${safeNote}_${safeAddress}_${timestamp}"
            }
            FileNameFormat.ADDRESS_TIMESTAMP -> {
                "${safeAddress}_${timestamp}"
            }
            FileNameFormat.BS_PROJECT_TIMESTAMP -> {
                "${safeNote}_${timestamp}"
            }
            FileNameFormat.TAG_BS_PROJECT_TIMESTAMP -> {
                "tag_${safeNote}_${timestamp}"
            }
            FileNameFormat.BS_PROJECT_TIMESTAMP_TAG -> {
               "${safeNote}_${timestamp}_tag"
            }
            FileNameFormat.TIMESTAMP_UNDERSCORE -> {
                timestampUnderscore
            }
        }
    }

    // Replace illegal chars with dashes or underscores
    private fun sanitize(input: String): String {
        if (input.isBlank()) return "Unknown"
        return input.trim()
            .replace(Regex("[^a-zA-Z0-9ก-๙\\-]"), "-") // Allow Thai, Alphanum, Dash
            .replace(Regex("-+"), "-") // Collapse multiple dashes
            .trim('-')
    }
}
