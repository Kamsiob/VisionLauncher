package io.github.kamsiob.launcher.data

import android.content.Context
import io.github.kamsiob.launcher.ui.theme.Look
import io.github.kamsiob.launcher.ui.theme.TextStep

/**
 * The handful of settings the very first frame needs, mirrored somewhere that
 * can be read synchronously.
 *
 * DataStore is the source of truth and stays so. It emits asynchronously, which
 * meant the first composed frames always used the defaults: the light palette
 * and onboardingDone = false. Someone who chose Dark got a cream flash on every
 * cold start, and someone who had finished first run could see the navigation
 * graph built with Onboarding as its start destination before the real value
 * arrived. Neither is acceptable on a launcher, which is the first thing a
 * person sees after unlocking.
 *
 * SharedPreferences is the right tool here precisely because it is synchronous
 * and tiny. It is written from one place, on every emission of the real
 * settings, so an install that predates this file is corrected on its first
 * launch rather than flashing forever.
 */
class BootSettings(context: Context) {

    private val prefs = context.getSharedPreferences("boot_settings", Context.MODE_PRIVATE)

    fun mirror(settings: Settings) {
        prefs.edit()
            .putString(KEY_LOOK, settings.look.name)
            .putBoolean(KEY_OUTLINED, settings.outlined)
            .putInt(KEY_TEXT_STEP, settings.textStep.ordinal)
            .putBoolean(KEY_ONBOARDED, settings.onboardingDone)
            .apply()
    }

    /** What the first frame should assume. Correct unless this is a first run. */
    fun read(): Settings = Settings(
        look = prefs.getString(KEY_LOOK, null)
            ?.let { runCatching { Look.valueOf(it) }.getOrNull() }
            ?: Look.LIGHT,
        outlined = prefs.getBoolean(KEY_OUTLINED, false),
        textStep = TextStep.entries.getOrElse(prefs.getInt(KEY_TEXT_STEP, 0)) { TextStep.ONE },
        onboardingDone = prefs.getBoolean(KEY_ONBOARDED, false),
    )

    private companion object {
        const val KEY_LOOK = "look"
        const val KEY_OUTLINED = "outlined"
        const val KEY_TEXT_STEP = "text_step"
        const val KEY_ONBOARDED = "onboarding_done"
    }
}
