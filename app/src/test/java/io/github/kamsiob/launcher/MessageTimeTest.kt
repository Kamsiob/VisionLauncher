package io.github.kamsiob.launcher

import io.github.kamsiob.launcher.messages.MessageTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MessageTimeTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `start of today is midnight, not the current moment`() {
        val now = at(2026, Calendar.AUGUST, 29, 14, 37)
        val start = MessageTime.startOfToday(now)
        val calendar = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(29, calendar.get(Calendar.DAY_OF_MONTH))
        assertTrue(start <= now)
    }

    @Test
    fun `a message from one minute after midnight counts as today`() {
        val now = at(2026, Calendar.AUGUST, 29, 23, 59)
        val justAfterMidnight = at(2026, Calendar.AUGUST, 29, 0, 1)
        assertTrue(justAfterMidnight >= MessageTime.startOfToday(now))
    }

    @Test
    fun `a message from one minute before midnight does not count as today`() {
        val now = at(2026, Calendar.AUGUST, 29, 0, 30)
        val lastNight = at(2026, Calendar.AUGUST, 28, 23, 59)
        assertTrue(lastNight < MessageTime.startOfToday(now))
    }
}
