package io.github.kamsiob.launcher

import io.github.kamsiob.launcher.alarm.Alarm
import io.github.kamsiob.launcher.alarm.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * An alarm that fires at the wrong time is worse than no alarm, and the one
 * piece of that which is pure arithmetic is when the next ring is due.
 */
class AlarmSchedulerTest {

    private fun at(hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 29)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun `an alarm later today rings today`() {
        val now = at(9, 0)
        val next = AlarmScheduler.nextTrigger(Alarm(1, 17, 30), now)
        val expected = at(17, 30)
        assertEquals(expected.timeInMillis, next)
    }

    @Test
    fun `an alarm already past today rings tomorrow`() {
        val now = at(18, 0)
        val next = AlarmScheduler.nextTrigger(Alarm(1, 7, 0), now)
        val expected = at(7, 0).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(expected.timeInMillis, next)
    }

    @Test
    fun `an alarm for this exact minute rings tomorrow, never zero seconds from now`() {
        val now = at(7, 0)
        val next = AlarmScheduler.nextTrigger(Alarm(1, 7, 0), now)
        assertTrue(next > now.timeInMillis)
        val expected = at(7, 0).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(expected.timeInMillis, next)
    }

    @Test
    fun `midnight is handled as the start of a day, not the end of one`() {
        val now = at(23, 30)
        val next = AlarmScheduler.nextTrigger(Alarm(1, 0, 15), now)
        val expected = at(0, 15).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(expected.timeInMillis, next)
    }

    @Test
    fun `the next ring is always in the future`() {
        for (nowHour in 0..23) {
            for (alarmHour in 0..23) {
                val now = at(nowHour, 30)
                val next = AlarmScheduler.nextTrigger(Alarm(1, alarmHour, 30), now)
                assertTrue(
                    "alarm $alarmHour:30 from $nowHour:30 must be in the future",
                    next > now.timeInMillis,
                )
            }
        }
    }
}
