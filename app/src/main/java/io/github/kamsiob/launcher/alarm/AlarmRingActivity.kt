package io.github.kamsiob.launcher.alarm

import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.LauncherApplication
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.data.Settings
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LauncherTheme
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import io.github.kamsiob.launcher.ui.theme.serifStyle
import java.util.Calendar

/**
 * The ringing screen: the time filling the display, Stop, and one optional
 * ten minute delay. No snooze maze.
 */
class AlarmRingActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AlarmReceiver.EXTRA_ID, -1)
        val hour = intent.getIntExtra(AlarmReceiver.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(AlarmReceiver.EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(AlarmReceiver.EXTRA_LABEL).orEmpty()

        startRinging()

        setContent {
            val app = application as LauncherApplication
            val settings by app.settingsStore.settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            LauncherTheme(
                look = settings.look,
                outlined = settings.outlined,
                textStep = settings.textStep,
            ) {
                RingingScreen(
                    hour = hour,
                    minute = minute,
                    label = label,
                    onStop = {
                        stopRinging()
                        finish()
                    },
                    onDelay = {
                        stopRinging()
                        scheduleTenMinuteDelay(id, label)
                        finish()
                    },
                )
            }
        }
    }

    private fun startRinging() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            isLooping = true
            play()
        }
        vibrator = getSystemService(Vibrator::class.java)?.apply {
            vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 700, 700), 0),
            )
        }
    }

    private fun stopRinging() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun scheduleTenMinuteDelay(id: Int, label: String) {
        val later = Calendar.getInstance().apply { add(Calendar.MINUTE, 10) }
        AlarmScheduler.schedule(
            this,
            Alarm(
                id = DELAY_ID_BASE + id,
                hour = later.get(Calendar.HOUR_OF_DAY),
                minute = later.get(Calendar.MINUTE),
                label = label,
                enabled = true,
            ),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
    }

    companion object {
        private const val DELAY_ID_BASE = 100_000
    }
}

@Composable
private fun RingingScreen(
    hour: Int,
    minute: Int,
    label: String,
    onStop: () -> Unit,
    onDelay: () -> Unit,
) {
    val palette = LocalPalette.current
    ScreenFrame(scrollable = false) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "%d:%02d".format(if (hour % 12 == 0) 12 else hour % 12, minute),
            style = serifStyle(size = TypeScale.clock, lineHeightFactor = 1f),
            color = palette.accent,
        )
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = bodyStyle(size = TypeScale.title, weight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = palette.text,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        ApplianceKey(
            label = stringResource(R.string.alarm_stop),
            onClick = onStop,
            style = KeyStyle.PRIMARY,
            minHeight = Dimens.bigKey,
            committing = true,
        )
        ApplianceKey(
            label = stringResource(R.string.alarm_ten_more),
            onClick = onDelay,
            minHeight = Dimens.keyMin,
            committing = true,
        )
    }
}
