package io.github.kamsiob.launcher

import android.app.Application
import io.github.kamsiob.launcher.data.BootSettings
import io.github.kamsiob.launcher.data.LayoutStore
import io.github.kamsiob.launcher.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LauncherApplication : Application() {
    val settingsStore by lazy { SettingsStore(this) }
    val layoutStore by lazy { LayoutStore(this) }
    val bootSettings by lazy { BootSettings(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // One writer, on every emission, so the boot mirror cannot drift and an
        // install that predates it is corrected on its first launch.
        scope.launch { settingsStore.settings.collect { bootSettings.mirror(it) } }
    }
}
