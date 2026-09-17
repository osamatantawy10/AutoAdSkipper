package com.autoadskipper.utils

/**
 * Formats an estimated "time saved" duration into something readable at a
 * glance ("3m 20s", "1h 12m", "2d 4h") rather than a raw second count.
 */
object TimeFormatUtils {

    fun formatDuration(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "0s"

        val days = totalSeconds / 86_400
        val hours = (totalSeconds % 86_400) / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60

        return when {
            days > 0 -> if (hours > 0) "${days}d ${hours}h" else "${days}d"
            hours > 0 -> if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            minutes > 0 -> if (seconds > 0) "${minutes}m ${seconds}s" else "${minutes}m"
            else -> "${seconds}s"
        }
    }
}
