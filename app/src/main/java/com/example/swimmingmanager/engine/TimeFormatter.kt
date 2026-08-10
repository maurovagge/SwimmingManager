package com.example.swimmingmanager.engine

import android.annotation.SuppressLint

object TimeFormatter {
    @SuppressLint("DefaultLocale")
    fun formatLongToTime(timeMs: Int): String {
        val minutes = (timeMs / 60000)
        val seconds = (timeMs % 60000) / 1000
        val hundredths = (timeMs % 1000) / 10

        return if (minutes > 0) {
            String.format("%d:%02d.%02d", minutes, seconds, hundredths)
        } else {
            String.format("%d.%02d", seconds, hundredths)
        }
    }
}