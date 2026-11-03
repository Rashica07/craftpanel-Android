package com.kiqa.omniservers.data

/** "12s", "14m 05s", "2h 14m", "3d 4h": short enough for a card. */
fun formatUptime(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val d = s / 86_400
    val h = s % 86_400 / 3_600
    val m = s % 3_600 / 60
    val sec = s % 60
    return when {
        d > 0 -> "${d}d ${h}h"
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${sec.toString().padStart(2, '0')}s"
        else -> "${sec}s"
    }
}
