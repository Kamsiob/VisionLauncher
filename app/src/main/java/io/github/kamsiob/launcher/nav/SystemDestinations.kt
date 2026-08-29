package io.github.kamsiob.launcher.nav

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import io.github.kamsiob.launcher.R

/**
 * Every handoff to a system screen goes through the threshold pattern, and
 * every destination the app can hand off to is named here. The threshold is
 * per destination dismissible, keyed by [id].
 */
enum class SystemDestination(val id: String, @param:StringRes val labelRes: Int) {
    SOUND("sound", R.string.dest_sound_settings),
    DND("dnd", R.string.dest_dnd_settings),
    AIRPLANE("airplane", R.string.dest_airplane_settings),
    NETWORK("network", R.string.dest_network_settings),
    STORAGE("storage", R.string.dest_storage_settings),
    BATTERY("battery", R.string.dest_battery_settings),
    DISPLAY_SIZE("display_size", R.string.dest_display_settings),
    ACCESSIBILITY("accessibility", R.string.dest_accessibility_settings),
    HEARING("hearing", R.string.dest_hearing_settings),
    CAPTIONS("captions", R.string.dest_caption_settings),
    SOUND_AMPLIFIER("sound_amplifier", R.string.dest_sound_amplifier),
    MEDICAL_INFO("medical_info", R.string.dest_medical_info),
    HOME_SETTINGS("home_settings", R.string.dest_home_settings);

    fun intent(context: Context): Intent = when (this) {
        SOUND -> Intent(Settings.ACTION_SOUND_SETTINGS)
        DND -> Intent("android.settings.ZEN_MODE_SETTINGS")
        AIRPLANE -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        NETWORK -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
        STORAGE -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        BATTERY -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        DISPLAY_SIZE ->
            if (Build.VERSION.SDK_INT >= 33) Intent("android.settings.TEXT_READING_SETTINGS")
            else Intent(Settings.ACTION_DISPLAY_SETTINGS)
        ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        HEARING -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        CAPTIONS -> Intent(Settings.ACTION_CAPTIONING_SETTINGS)
        SOUND_AMPLIFIER -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        MEDICAL_INFO -> Intent("android.settings.EMERGENCY_INFORMATION")
        HOME_SETTINGS -> Intent(Settings.ACTION_HOME_SETTINGS)
    }

    /** Fires the handoff, falling back to the main settings screen if the
     *  device lacks the specific one. */
    fun launch(context: Context) {
        val primary = intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (primary.resolveActivity(context.packageManager) != null) {
            context.startActivity(primary)
        } else {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    companion object {
        fun fromId(id: String?): SystemDestination? = entries.firstOrNull { it.id == id }
    }
}
