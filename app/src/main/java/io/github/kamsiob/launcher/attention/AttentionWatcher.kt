package io.github.kamsiob.launcher.attention

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/** Which watched states are currently raised, read without any privileged permission. */
data class AttentionState(
    val ringerSilent: Boolean = false,
    val ringerVibrate: Boolean = false,
    val dndOn: Boolean = false,
    val batteryPercent: Int = 100,
    val batteryLow: Boolean = false,
    val airplaneOn: Boolean = false,
    val noNetwork: Boolean = false,
    val storageNearlyFull: Boolean = false,
    val batteryOptimizationOn: Boolean = false,
) {
    val anythingRaised: Boolean
        get() = ringerSilent || ringerVibrate || dndOn || batteryLow || airplaneOn ||
            noNetwork || storageNearlyFull || batteryOptimizationOn
}

/**
 * Watches the states that quietly silence a phone and feeds the attention
 * queue. Everything here reads public system state; where a broadcast exists
 * it listens, and the rest is re-checked on every resume through [refresh].
 */
class AttentionWatcher(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val polled = MutableStateFlow(pollNow())

    /** Called from onResume and after any repair, so the lamp never goes stale. */
    fun refresh() {
        polled.value = pollNow()
    }

    private fun pollNow(): AttentionState {
        val ringer = audioManager.ringerMode
        val filter = notificationManager.currentInterruptionFilter
        val battery = batteryPercent()
        return AttentionState(
            ringerSilent = ringer == AudioManager.RINGER_MODE_SILENT,
            ringerVibrate = ringer == AudioManager.RINGER_MODE_VIBRATE,
            dndOn = filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN,
            batteryPercent = battery,
            batteryLow = battery in 1..15 && !isCharging(),
            airplaneOn = Settings.Global.getInt(
                context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0
            ) == 1,
            noNetwork = !hasNetwork(),
            storageNearlyFull = storageNearlyFull(),
            batteryOptimizationOn = !powerManager.isIgnoringBatteryOptimizations(context.packageName),
        )
    }

    private fun batteryPercent(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 1..100) level else 100
    }

    private fun isCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun hasNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun storageNearlyFull(): Boolean {
        val stat = StatFs(Environment.getDataDirectory().path)
        val free = stat.availableBytes
        val total = stat.totalBytes
        if (total <= 0) return false
        return free < total / 20 || free < 500L * 1024 * 1024
    }

    /** Broadcast driven re-polls, folded into the same state. */
    private fun broadcasts(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { trySend(Unit) }
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_DEVICE_STORAGE_LOW)
            addAction(Intent.ACTION_DEVICE_STORAGE_OK)
        }
        context.registerReceiver(receiver, filter)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(Unit) }
            override fun onLost(network: Network) { trySend(Unit) }
            override fun onCapabilitiesChanged(n: Network, c: NetworkCapabilities) { trySend(Unit) }
        }
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().build(), networkCallback
        )
        awaitClose {
            context.unregisterReceiver(receiver)
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    val state: Flow<AttentionState> = merge(
        polled,
        broadcasts().map { pollNow() },
    )

    /** One tap repairs. Each returns true when the direct fix was possible. */
    fun repairRinger(): Boolean = runCatching {
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        refresh()
        audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }.getOrDefault(false)

    /**
     * Whether this app can turn Do Not Disturb off itself. It needs a special
     * access the app never requests, so on an ordinary device this is false and
     * the lamp key has to offer a handoff instead of a fix.
     */
    fun canToggleDnd(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    fun repairDnd(): Boolean {
        if (!notificationManager.isNotificationPolicyAccessGranted) return false
        return runCatching {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            refresh()
            true
        }.getOrDefault(false)
    }
}
