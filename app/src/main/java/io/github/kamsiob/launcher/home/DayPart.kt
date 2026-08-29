package io.github.kamsiob.launcher.home

import android.content.Context
import android.text.format.DateFormat
import io.github.kamsiob.launcher.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The day part logic behind the masthead. Text thresholds: morning until
 * 12:00, afternoon until 17:00, evening after. The mark is the sun during
 * daylight hours and the moon in the evening and at night, drawn over the
 * same horizon line in both states.
 */
object DayPart {

    fun dayPartText(context: Context, calendar: Calendar = Calendar.getInstance()): String {
        val weekday = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
        val res = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> R.string.day_part_morning
            in 12..16 -> R.string.day_part_afternoon
            else -> R.string.day_part_evening
        }
        return context.getString(res, weekday)
    }

    /** The moon shows in the evening and through the night's small hours. */
    fun isMoon(calendar: Calendar = Calendar.getInstance()): Boolean {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour >= 17 || hour < 6
    }

    fun clockText(context: Context, date: Date = Date()): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }

    fun dateText(date: Date = Date()): String {
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "MMMMd")
        return SimpleDateFormat(pattern, locale).format(date)
    }
}
