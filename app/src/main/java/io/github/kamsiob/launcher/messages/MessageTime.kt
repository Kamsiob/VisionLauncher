package io.github.kamsiob.launcher.messages

import android.content.Context
import android.text.format.DateFormat
import io.github.kamsiob.launcher.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * When a message arrived, said the way a person would say it.
 *
 * Today gets a clock time, yesterday gets the word, and anything older gets a
 * date. A bare timestamp on every row would make the reader do the arithmetic,
 * and the whole point of the inbox is that looking back is easy.
 */
object MessageTime {

    fun relative(context: Context, at: Long, now: Long = System.currentTimeMillis()): String {
        val then = Calendar.getInstance().apply { timeInMillis = at }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return when {
            sameDay(then, today) -> clock(context, at)
            sameDay(then, yesterday) -> context.getString(R.string.time_yesterday)
            else -> date(at)
        }
    }

    /** The full form, for the reading screen, which has room to say all of it. */
    fun full(context: Context, at: Long, now: Long = System.currentTimeMillis()): String {
        val then = Calendar.getInstance().apply { timeInMillis = at }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        return if (sameDay(then, today)) clock(context, at)
        else "${date(at)}, ${clock(context, at)}"
    }

    fun clock(context: Context, at: Long): String {
        val locale = Locale.getDefault()
        val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hma"
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        return SimpleDateFormat(pattern, locale).format(Date(at))
    }

    private fun date(at: Long): String {
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "MMMMd")
        return SimpleDateFormat(pattern, locale).format(Date(at))
    }

    private fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    /** Midnight this morning, which is what "new today" counts from. */
    fun startOfToday(now: Long = System.currentTimeMillis()): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
