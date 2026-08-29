package io.github.kamsiob.launcher.data

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.Collator
import java.util.concurrent.ConcurrentHashMap

/**
 * Everything the launcher knows about other apps comes through LauncherApps,
 * the sanctioned API for exactly this. No AccessibilityService, anywhere.
 */
class AppsRepository(private val context: Context) {

    data class AppEntry(
        val label: String,
        val packageName: String,
        val activity: String,
        val user: UserHandle,
        val isWorkProfile: Boolean,
    ) {
        val key: String get() = "$packageName/$activity"
    }

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    /** All launchable apps across profiles, alphabetized for the current locale. */
    fun launchableApps(): List<AppEntry> {
        val collator = Collator.getInstance()
        val self = Process.myUserHandle()
        return userManager.userProfiles.flatMap { user ->
            launcherApps.getActivityList(null, user).map { info ->
                AppEntry(
                    label = info.label.toString(),
                    packageName = info.applicationInfo.packageName,
                    activity = info.name,
                    user = user,
                    isWorkProfile = user != self,
                )
            }
        }
            .filter { it.packageName != context.packageName }
            .sortedWith(compareBy(collator) { it.label })
    }

    fun entryFor(tile: SavedTile): AppEntry? {
        val pkg = tile.packageName ?: return null
        return launchableApps().firstOrNull {
            it.packageName == pkg && (tile.activity == null || it.activity == tile.activity)
        }
    }

    fun launch(entry: AppEntry) {
        val component = android.content.ComponentName(entry.packageName, entry.activity)
        launcherApps.startMainActivity(component, entry.user, null, null)
    }

    fun icon(entry: AppEntry, sizePx: Int): ImageBitmap = iconCache.getOrPut(entry.key) {
        val info: LauncherActivityInfo? = launcherApps
            .getActivityList(entry.packageName, entry.user)
            .firstOrNull { it.name == entry.activity }
        val drawable: Drawable? = info?.getIcon(0)
        (drawable ?: context.packageManager.defaultActivityIcon)
            .toBitmap(width = sizePx, height = sizePx)
            .asImageBitmap()
    }

    /** Emits whenever apps are installed, removed, or changed. */
    fun changes(): Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) { trySend(Unit) }
            override fun onPackageAdded(packageName: String?, user: UserHandle?) { trySend(Unit) }
            override fun onPackageChanged(packageName: String?, user: UserHandle?) { trySend(Unit) }
            override fun onPackagesAvailable(p: Array<out String>?, u: UserHandle?, r: Boolean) { trySend(Unit) }
            override fun onPackagesUnavailable(p: Array<out String>?, u: UserHandle?, r: Boolean) { trySend(Unit) }
        }
        launcherApps.registerCallback(callback)
        awaitClose { launcherApps.unregisterCallback(callback) }
    }
}
