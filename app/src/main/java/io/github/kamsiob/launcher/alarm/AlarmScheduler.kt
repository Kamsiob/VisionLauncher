package io.github.kamsiob.launcher.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Alarms fire from a killed process and survive reboot, which is the whole
 * point of an alarm clock. Exact alarms are used because an alarm that drifts
 * is not an alarm.
 */
object AlarmScheduler {

    private fun intentFor(context: Context, alarm: Alarm): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            alarm.id,
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ID, alarm.id)
                putExtra(AlarmReceiver.EXTRA_HOUR, alarm.hour)
                putExtra(AlarmReceiver.EXTRA_MINUTE, alarm.minute)
                putExtra(AlarmReceiver.EXTRA_LABEL, alarm.label)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun nextTrigger(alarm: Alarm, from: Calendar = Calendar.getInstance()): Long {
        val next = (from.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (next.timeInMillis <= from.timeInMillis) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    fun schedule(context: Context, alarm: Alarm) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val pending = intentFor(context, alarm)
        if (!alarm.enabled) {
            manager.cancel(pending)
            return
        }
        val trigger = nextTrigger(alarm)
        val canExact = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
        if (canExact) {
            manager.setAlarmClock(
                AlarmManager.AlarmClockInfo(trigger, pending),
                pending,
            )
        } else {
            // Without the exact permission the alarm still fires, just with
            // the system's own leeway. Silently dropping it would be worse.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    /**
     * Cancels an alarm and any snooze standing behind it.
     *
     * A snooze is scheduled under a derived id so it can fire once without
     * joining the saved list. Nothing tracked that id, so turning an alarm off
     * or taking it off left its snooze to ring anyway, from an alarm the person
     * had already dismissed.
     */
    fun cancel(context: Context, alarm: Alarm) {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(intentFor(context, alarm))
        manager.cancel(intentFor(context, alarm.copy(id = snoozeIdFor(alarm.id))))
    }

    /** The id a snooze of this alarm is scheduled under. */
    fun snoozeIdFor(id: Int): Int = SNOOZE_ID_BASE + id

    private const val SNOOZE_ID_BASE = 100_000

    fun rescheduleAll(context: Context, alarms: List<Alarm>) {
        alarms.forEach { schedule(context, it) }
    }
}
