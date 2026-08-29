package io.github.kamsiob.launcher

import android.app.Application
import io.github.kamsiob.launcher.data.LayoutStore
import io.github.kamsiob.launcher.data.SettingsStore

class LauncherApplication : Application() {
    val settingsStore by lazy { SettingsStore(this) }
    val layoutStore by lazy { LayoutStore(this) }
}
