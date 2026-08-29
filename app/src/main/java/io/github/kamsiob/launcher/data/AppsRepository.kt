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

    /**
     * The entry behind one home tile.
     *
     * This asks LauncherApps for a single package rather than enumerating every
     * launchable activity on the device and searching the result. The
     * enumerating version ran once per third party tile, during composition, on
     * the main thread, and each pass resolved a label for all of them and sorted
     * the lot with a Collator. It was invisible on a phone with a hundred apps
     * and would not have stayed that way.
     */
    fun entryFor(tile: SavedTile): AppEntry? {
        val pkg = tile.packageName ?: return null
        val self = Process.myUserHandle()
        return userManager.userProfiles.firstNotNullOfOrNull { user ->
            launcherApps.getActivityList(pkg, user)
                .firstOrNull { tile.activity == null || it.name == tile.activity }
                ?.let { info ->
                    AppEntry(
                        label = info.label.toString(),
                        packageName = info.applicationInfo.packageName,
                        activity = info.name,
                        user = user,
                        isWorkProfile = user != self,
                    )
                }
        }
    }

    /**
     * Returns false when the app could not be started, which happens when it was
     * uninstalled or its profile was turned off between drawing the tile and
     * pressing it. Unguarded, that throws out of the launcher itself, which is
     * the one app on the phone that has nowhere to fall back to.
     */
    fun launch(entry: AppEntry): Boolean = runCatching {
        val component = android.content.ComponentName(entry.packageName, entry.activity)
        launcherApps.startMainActivity(component, entry.user, null, null)
        true
    }.getOrDefault(false)

    /**
     * The cache key carries the profile and the pixel size as well as the
     * component. A work app and its personal twin share a package name, and the
     * same app is drawn at more than one size, so keying on the component alone
     * hands back the wrong bitmap.
     */
    fun icon(entry: AppEntry, sizePx: Int): ImageBitmap {
        val cacheKey = entry.key + "#" + entry.user.hashCode() + "@" + sizePx
        return iconCache.getOrPut(cacheKey) {
        val info: LauncherActivityInfo? = launcherApps
            .getActivityList(entry.packageName, entry.user)
            .firstOrNull { it.name == entry.activity }
        val drawable: Drawable? = info?.getIcon(0)
        (drawable ?: context.packageManager.defaultActivityIcon)
            .toBitmap(width = sizePx, height = sizePx)
            .asImageBitmap()
        }
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
