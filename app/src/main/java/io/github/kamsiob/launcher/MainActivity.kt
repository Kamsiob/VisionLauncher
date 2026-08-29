package io.github.kamsiob.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kamsiob.launcher.alarm.Alarm
import io.github.kamsiob.launcher.alarm.AlarmScheduler
import io.github.kamsiob.launcher.alarm.AlarmStore
import io.github.kamsiob.launcher.attention.AttentionWatcher
import io.github.kamsiob.launcher.data.AppsRepository
import io.github.kamsiob.launcher.data.BuiltIn
import io.github.kamsiob.launcher.data.ContactsRepository
import io.github.kamsiob.launcher.data.EmergencyContact
import io.github.kamsiob.launcher.data.Favorite
import io.github.kamsiob.launcher.data.Settings
import io.github.kamsiob.launcher.nav.SystemDestination
import io.github.kamsiob.launcher.ui.alarm.AlarmEditScreen
import io.github.kamsiob.launcher.ui.alarm.AlarmListScreen
import io.github.kamsiob.launcher.ui.apps.MoreAppsScreen
import io.github.kamsiob.launcher.ui.arrange.ArrangeScreen
import io.github.kamsiob.launcher.ui.call.CallScreen
import io.github.kamsiob.launcher.ui.call.ContactsScreen
import io.github.kamsiob.launcher.ui.call.KeypadScreen
import io.github.kamsiob.launcher.ui.emergency.EmergencyScreen
import io.github.kamsiob.launcher.ui.home.HomeScreen
import io.github.kamsiob.launcher.ui.home.NotBuiltScreen
import io.github.kamsiob.launcher.ui.onboarding.OnboardingScreen
import io.github.kamsiob.launcher.ui.settings.AboutScreen
import io.github.kamsiob.launcher.ui.settings.HelperScreen
import io.github.kamsiob.launcher.ui.settings.PickContactScreen
import io.github.kamsiob.launcher.ui.settings.SeeHearScreen
import io.github.kamsiob.launcher.ui.settings.SettingsScreen
import io.github.kamsiob.launcher.ui.theme.LauncherTheme
import io.github.kamsiob.launcher.ui.threshold.ThresholdScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    lateinit var attentionWatcher: AttentionWatcher
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        attentionWatcher = AttentionWatcher(this)
        val app = application as LauncherApplication
        val apps = AppsRepository(this)
        val contacts = ContactsRepository(this)
        val alarmStore = AlarmStore(this)

        setContent {
            val settings by app.settingsStore.settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            LauncherTheme(
                look = settings.look,
                outlined = settings.outlined,
                textStep = settings.textStep,
            ) {
                LauncherNav(
                    activity = this,
                    app = app,
                    apps = apps,
                    contacts = contacts,
                    alarmStore = alarmStore,
                    settings = settings,
                    homePressed = homePressed,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing Home while inside the launcher relaunches it, so Home
        // always means home rather than leaving the person where they were.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            lifecycleScope.launch { homePressed.emit(Unit) }
        }
    }

    override fun onResume() {
        super.onResume()
        attentionWatcher.refresh()
    }
}

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val MORE_APPS = "moreapps"
    const val CALL = "call"
    const val CONTACTS = "contacts"
    const val KEYPAD = "keypad"
    const val EMERGENCY = "emergency"
    const val ALARMS = "alarms"
    const val ALARM_EDIT = "alarmedit/{id}"
    const val SETTINGS = "settings"
    const val SEE_HEAR = "seehear"
    const val HELPER = "helper"
    const val ABOUT = "about"
    const val PICK_FAVORITE = "pickfavorite"
    const val PICK_EMERGENCY = "pickemergency"
    const val ARRANGE = "arrange"
    const val NOT_BUILT = "notbuilt/{feature}"
    const val THRESHOLD = "threshold/{dest}"

    fun notBuilt(feature: BuiltIn) = "notbuilt/${feature.id}"
    fun threshold(dest: SystemDestination) = "threshold/${dest.id}"
    fun alarmEdit(id: Int) = "alarmedit/$id"
}

@Composable
fun LauncherNav(
    activity: MainActivity,
    app: LauncherApplication,
    apps: AppsRepository,
    contacts: ContactsRepository,
    alarmStore: AlarmStore,
    settings: Settings,
    homePressed: MutableSharedFlow<Unit>,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val layout by app.layoutStore.layout
        .collectAsStateWithLifecycle(initialValue = app.layoutStore.defaultLayout)
    val alarms by alarmStore.alarms.collectAsStateWithLifecycle(initialValue = emptyList())

    // Recomposed after a permission result so the Call screen stops saying it
    // cannot see contacts the moment it can.
    var contactsPermissionGeneration by remember { mutableStateOf(0) }
    val hasContacts = remember(contactsPermissionGeneration) { contacts.hasPermission() }

    LaunchedEffect(homePressed) {
        homePressed.collect {
            navController.popBackStack(Routes.HOME, inclusive = false)
        }
    }

    val goHome: () -> Unit = { navController.popBackStack(Routes.HOME, inclusive = false) }
    val goBack: () -> Unit = { navController.popBackStack() }
    val handoff: (SystemDestination) -> Unit = { dest ->
        if (dest.id in settings.dismissedThresholds) {
            dest.launch(activity)
        } else {
            navController.navigate(Routes.threshold(dest))
        }
    }

    val start = if (settings.onboardingDone) Routes.HOME else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = { helperPath, batterySkipped ->
                    scope.launch {
                        app.settingsStore.setOnboardingDone(helperPath, batterySkipped)
                    }
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                layout = layout,
                apps = apps,
                watcher = activity.attentionWatcher,
                onOpenFeature = { feature ->
                    when (feature) {
                        BuiltIn.CALL -> navController.navigate(Routes.CALL)
                        BuiltIn.ALARMS -> navController.navigate(Routes.ALARMS)
                        BuiltIn.CAMERA -> launchCamera(activity)
                        else -> navController.navigate(Routes.notBuilt(feature))
                    }
                },
                onMoreApps = { navController.navigate(Routes.MORE_APPS) },
                onHandoff = handoff,
            )
        }

        composable(Routes.MORE_APPS) {
            MoreAppsScreen(
                apps = apps,
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onHome = goHome,
                onLaunched = goHome,
            )
        }

        composable(Routes.CALL) {
            CallScreen(
                favorites = settings.favorites,
                hasContactsPermission = hasContacts,
                onAllContacts = { navController.navigate(Routes.CONTACTS) },
                onKeypad = { navController.navigate(Routes.KEYPAD) },
                onEmergency = { navController.navigate(Routes.EMERGENCY) },
                onHome = goHome,
                onContactsPermissionChanged = { contactsPermissionGeneration++ },
            )
        }

        composable(Routes.CONTACTS) {
            ContactsScreen(contacts = contacts, onHome = goHome, onBack = goBack)
        }

        composable(Routes.KEYPAD) {
            KeypadScreen(onHome = goHome, onBack = goBack)
        }

        composable(Routes.EMERGENCY) {
            EmergencyScreen(
                emergencyContact = settings.emergencyContact,
                onSetUpPerson = { navController.navigate(Routes.PICK_EMERGENCY) },
                onHome = goHome,
                onBack = goBack,
            )
        }

        composable(Routes.ALARMS) {
            AlarmListScreen(
                alarms = alarms,
                onToggle = { alarm ->
                    val updated = alarm.copy(enabled = !alarm.enabled)
                    scope.launch { alarmStore.save(updated) }
                    if (updated.enabled) {
                        AlarmScheduler.schedule(activity, updated)
                    } else {
                        AlarmScheduler.cancel(activity, updated)
                    }
                },
                onEdit = { navController.navigate(Routes.alarmEdit(it.id)) },
                onNew = { navController.navigate(Routes.alarmEdit(0)) },
                onHome = goHome,
            )
        }

        composable(Routes.ALARM_EDIT) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: 0
            val existing = alarms.firstOrNull { it.id == id }
            AlarmEditScreen(
                existing = existing,
                onSave = { hour, minute, label ->
                    scope.launch {
                        val alarm = Alarm(
                            id = existing?.id ?: alarmStore.nextId(),
                            hour = hour,
                            minute = minute,
                            label = label,
                            enabled = true,
                        )
                        alarmStore.save(alarm)
                        AlarmScheduler.schedule(activity, alarm)
                    }
                    goBack()
                },
                onDelete = existing?.let {
                    {
                        AlarmScheduler.cancel(activity, it)
                        scope.launch { alarmStore.delete(it.id) }
                        goBack()
                    }
                },
                onHome = goHome,
                onBack = goBack,
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                look = settings.look,
                outlined = settings.outlined,
                textStep = settings.textStep,
                onSetLook = { scope.launch { app.settingsStore.setLook(it) } },
                onSetOutlined = { scope.launch { app.settingsStore.setOutlined(it) } },
                onSetTextStep = { scope.launch { app.settingsStore.setTextStep(it) } },
                onChooseApps = { navController.navigate(Routes.ARRANGE) },
                onRestore = { app.layoutStore.restoreSnapshot() },
                onSeeHear = { navController.navigate(Routes.SEE_HEAR) },
                onHelper = { navController.navigate(Routes.HELPER) },
                onHome = goHome,
            )
        }

        composable(Routes.SEE_HEAR) {
            SeeHearScreen(onHandoff = handoff, onHome = goHome, onBack = goBack)
        }

        composable(Routes.HELPER) {
            HelperScreen(
                favorites = settings.favorites,
                emergencyContact = settings.emergencyContact,
                onSetFavorites = { scope.launch { app.settingsStore.setFavorites(it) } },
                onSetEmergencyContact = { scope.launch { app.settingsStore.setEmergencyContact(it) } },
                onRestoreWarnings = { scope.launch { app.settingsStore.restoreAllThresholds() } },
                onAddFavorite = { navController.navigate(Routes.PICK_FAVORITE) },
                onChooseEmergencyPerson = { navController.navigate(Routes.PICK_EMERGENCY) },
                onAbout = { navController.navigate(Routes.ABOUT) },
                onHome = goHome,
                onBack = goBack,
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(onHome = goHome, onBack = goBack)
        }

        composable(Routes.PICK_FAVORITE) {
            PickContactScreen(
                contacts = contacts,
                title = activity.getString(R.string.helper_add_favorite),
                askRelationship = true,
                onPicked = { name, number, relationship ->
                    scope.launch {
                        app.settingsStore.setFavorites(
                            settings.favorites + Favorite(name, number, relationship)
                        )
                    }
                    goBack()
                },
                onHome = goHome,
                onBack = goBack,
            )
        }

        composable(Routes.PICK_EMERGENCY) {
            PickContactScreen(
                contacts = contacts,
                title = activity.getString(R.string.helper_emergency_person),
                askRelationship = false,
                onPicked = { name, number, _ ->
                    scope.launch {
                        app.settingsStore.setEmergencyContact(EmergencyContact(name, number))
                    }
                    goBack()
                },
                onHome = goHome,
                onBack = goBack,
            )
        }

        composable(Routes.ARRANGE) {
            ArrangeScreen(
                startingLayout = layout,
                apps = apps,
                onKeep = { scope.launch { app.layoutStore.keep(it) } },
                onExit = goBack,
            )
        }

        composable(Routes.NOT_BUILT) { backStackEntry ->
            val feature = BuiltIn.fromId(backStackEntry.arguments?.getString("feature"))
                ?: BuiltIn.MESSAGES
            NotBuiltScreen(feature = feature, onHome = goHome)
        }

        composable(Routes.THRESHOLD) { backStackEntry ->
            val dest = SystemDestination.fromId(backStackEntry.arguments?.getString("dest"))
            if (dest == null) {
                LaunchedEffect(Unit) { goBack() }
            } else {
                ThresholdScreen(
                    destination = dest,
                    onContinue = {
                        navController.popBackStack()
                        dest.launch(activity)
                    },
                    onStay = goBack,
                    onDismissForever = {
                        scope.launch { app.settingsStore.dismissThreshold(dest.id) }
                        navController.popBackStack()
                        dest.launch(activity)
                    },
                    onHome = goHome,
                )
            }
        }
    }
}

private fun launchCamera(activity: MainActivity) {
    val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { activity.startActivity(intent) }
}
