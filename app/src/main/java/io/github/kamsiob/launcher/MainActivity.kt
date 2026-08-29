package io.github.kamsiob.launcher

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
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
import io.github.kamsiob.launcher.messages.MessagesState
import io.github.kamsiob.launcher.nav.SystemDestination
import io.github.kamsiob.launcher.seeing.SeeingState
import io.github.kamsiob.launcher.ui.alarm.AlarmEditScreen
import io.github.kamsiob.launcher.ui.alarm.AlarmListScreen
import io.github.kamsiob.launcher.ui.apps.MoreAppsScreen
import io.github.kamsiob.launcher.ui.arrange.ArrangeScreen
import io.github.kamsiob.launcher.ui.call.CallScreen
import io.github.kamsiob.launcher.ui.call.ContactsScreen
import io.github.kamsiob.launcher.ui.call.KeypadScreen
import io.github.kamsiob.launcher.ui.emergency.EmergencyScreen
import io.github.kamsiob.launcher.ui.home.HomeScreen
import io.github.kamsiob.launcher.ui.messages.MessagesScreen
import io.github.kamsiob.launcher.ui.messages.ReadMessageScreen
import io.github.kamsiob.launcher.ui.messages.ReplyScreen
import io.github.kamsiob.launcher.ui.seeing.MagnifierScreen
import io.github.kamsiob.launcher.ui.seeing.PhotosScreen
import io.github.kamsiob.launcher.ui.seeing.ReaderScreen
import io.github.kamsiob.launcher.ui.onboarding.OnboardingScreen
import io.github.kamsiob.launcher.ui.settings.AboutScreen
import io.github.kamsiob.launcher.ui.settings.HelperScreen
import io.github.kamsiob.launcher.ui.settings.PickContactScreen
import io.github.kamsiob.launcher.ui.settings.SeeHearScreen
import io.github.kamsiob.launcher.ui.settings.SettingsScreen
import io.github.kamsiob.launcher.ui.theme.LauncherTheme
import io.github.kamsiob.launcher.ui.theme.Look
import io.github.kamsiob.launcher.ui.threshold.ThresholdScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    lateinit var attentionWatcher: AttentionWatcher
        private set
    private lateinit var messagesState: MessagesState
    private lateinit var seeingState: SeeingState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        attentionWatcher = AttentionWatcher(this)
        val app = application as LauncherApplication
        val apps = AppsRepository(this)
        val contacts = ContactsRepository(this)
        val alarmStore = AlarmStore(this)
        // Tied to the activity, not to a composition, so a reply in flight
        // survives the recomposition that follows it and the speech engines are
        // built once rather than on every navigation.
        messagesState = MessagesState(this, lifecycleScope)
        seeingState = SeeingState(this, lifecycleScope)

        // Read synchronously so frame one already knows the chosen theme and
        // whether first run is behind us, instead of painting the light palette
        // and building the graph with Onboarding as its start destination.
        val boot = app.bootSettings.read()
        window.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                if (boot.look == Look.DARK) 0xFF1B1D20.toInt() else 0xFFF4EEE1.toInt()
            )
        )

        setContent {
            val settings by app.settingsStore.settings
                .collectAsStateWithLifecycle(initialValue = boot)
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
                    messages = messagesState,
                    seeing = seeingState,
                )
            }
        }
    }

    override fun onDestroy() {
        // The speech engines hold a service connection each. Leaving them bound
        // after the launcher is gone keeps a recognizer alive with a microphone
        // it is no longer entitled to.
        if (::messagesState.isInitialized) messagesState.release()
        if (::seeingState.isInitialized) seeingState.release()
        super.onDestroy()
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
    const val MESSAGES = "messages"
    const val MAGNIFIER = "magnifier"
    const val READER = "reader"
    const val PHOTOS = "photos"
    const val READ_MESSAGE = "readmessage"
    const val REPLY = "reply"
    const val THRESHOLD = "threshold/{dest}"

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
    messages: MessagesState,
    seeing: SeeingState,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val layout by app.layoutStore.layout
        .collectAsStateWithLifecycle(initialValue = app.layoutStore.defaultLayout)
    val alarms by alarmStore.alarms.collectAsStateWithLifecycle(initialValue = emptyList())
    // Held above the NavHost so it survives the pop back to the alarm list.
    var justRemovedAlarm by remember { mutableStateOf<Alarm?>(null) }

    // Recomposed after a permission result so the Call screen stops saying it
    // cannot see contacts the moment it can.
    var contactsPermissionGeneration by remember { mutableStateOf(0) }
    val hasContacts = remember(contactsPermissionGeneration) { contacts.hasPermission() }

    val inbox by messages.messages.collectAsStateWithLifecycle(initialValue = emptyList())
    val unreadToday by messages.unreadToday.collectAsStateWithLifecycle(initialValue = 0)

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

    // The status bar sits over whatever the screen puts under it. On home that
    // is the navy masthead and the icons must be light; everywhere else it is
    // the screen background. It was hardcoded light-background in themes.xml,
    // so the clock and battery were dark over a dark masthead.
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val overDarkTop = route == Routes.HOME || settings.look == Look.DARK
    // The navigation bar sits over the screen background, which is paper in the
    // light theme and dark in the dark one, and follows the app's Look rather
    // than the system night mode. uiMode is in configChanges, so it could never
    // have corrected itself on a system theme change either.
    LaunchedEffect(overDarkTop, settings.look) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = !overDarkTop
        controller.isAppearanceLightNavigationBars = settings.look != Look.DARK
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
                        BuiltIn.MESSAGES -> navController.navigate(Routes.MESSAGES)
                        BuiltIn.MAGNIFIER -> navController.navigate(Routes.MAGNIFIER)
                        BuiltIn.PHOTOS -> navController.navigate(Routes.PHOTOS)
                        BuiltIn.ALARMS -> navController.navigate(Routes.ALARMS)
                        BuiltIn.CAMERA -> launchCamera(activity)
                    }
                },
                onMoreApps = { navController.navigate(Routes.MORE_APPS) },
                onHandoff = handoff,
                onMissingTile = { navController.navigate(Routes.ARRANGE) },
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
                justRemoved = justRemovedAlarm,
                onPutItBack = {
                    justRemovedAlarm?.let { alarm ->
                        scope.launch { alarmStore.save(alarm) }
                        if (alarm.enabled) AlarmScheduler.schedule(activity, alarm)
                        justRemovedAlarm = null
                    }
                },
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
                        justRemovedAlarm = it
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
                onSupport = { openSupportLink(activity) },
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
            // Choosing the person is the moment the alert key's promise is made,
            // so it is the moment to ask for what that promise needs. Asking
            // here rather than during an emergency also means nobody is granting
            // permissions while something is wrong.
            val emergencyPermissions = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { goBack() }
            PickContactScreen(
                contacts = contacts,
                title = activity.getString(R.string.helper_emergency_person),
                askRelationship = false,
                onPicked = { name, number, _ ->
                    scope.launch {
                        app.settingsStore.setEmergencyContact(EmergencyContact(name, number))
                    }
                    emergencyPermissions.launch(
                        arrayOf(
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                    )
                },
                onHome = goHome,
                onBack = goBack,
            )
        }

        composable(Routes.ARRANGE) {
            ArrangeScreen(
                startingLayout = layout,
                apps = apps,
                // lifecycleScope, not the composition's scope. Arranging writes
                // its work out when the screen is disposed, and a scope tied to
                // that composition is already cancelled by then, so the write
                // would be dropped exactly when it matters most.
                onKeep = { activity.lifecycleScope.launch { app.layoutStore.keep(it) } },
                onHome = goHome,
                onExit = goBack,
            )
        }

        composable(Routes.MESSAGES) {
            // Read on every visit rather than remembered. Access can be revoked
            // in system settings while the app is in the background, and a
            // cached "yes" would leave the screen showing an empty inbox with
            // no explanation for why nothing new arrives.
            val hasAccess = remember(navController.currentBackStackEntry) { messages.hasAccess() }
            MessagesScreen(
                messages = inbox,
                unreadToday = unreadToday,
                hasAccess = hasAccess,
                onOpen = { message ->
                    messages.openMessage(message)
                    navController.navigate(Routes.READ_MESSAGE)
                },
                onGrantAccess = { handoff(SystemDestination.NOTIFICATION_ACCESS) },
                onHome = goHome,
            )
        }

        composable(Routes.READ_MESSAGE) { entry ->
            val open = messages.open
            if (open == null) {
                // Reached with nothing to show, which happens if the process
                // was killed while the screen was up and Android restored the
                // back stack without the state behind it.
                //
                // Guarded on this entry being the one on screen. Without the
                // guard, clearing the open message while navigating away made
                // both this screen and the reply screen fire a pop as they were
                // disposed, and the two extra pops emptied the graph and left
                // the launcher showing nothing at all.
                PopWhenCurrent(entry, goBack)
            } else {
                ReadMessageScreen(
                    message = open,
                    speaking = messages.speaking,
                    canSpeak = messages.canSpeakAloud,
                    onReply = { navController.navigate(Routes.REPLY) },
                    onReadAloud = { messages.readAloud() },
                    onStopReading = { messages.stopSpeaking() },
                    onHome = {
                        messages.closeMessage()
                        goHome()
                    },
                    onBack = {
                        messages.closeMessage()
                        goBack()
                    },
                )
            }
        }

        composable(Routes.REPLY) { entry ->
            val open = messages.open
            if (open == null) {
                PopWhenCurrent(entry, goBack)
            } else {
                val context = LocalContext.current
                val defaults = defaultPhrases(context)
                val typed = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val text = result.data
                        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank() && messages.sendReply(text)) goBack()
                }
                val microphone = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted -> if (granted) messages.startDictation() }
                ReplyScreen(
                    senderName = open.sender,
                    phrases = settings.replyPhrases.ifEmpty { defaults },
                    canDictate = messages.canDictate,
                    listening = messages.listening,
                    heard = messages.heard,
                    lastError = messages.lastError,
                    onSpeak = {
                        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            messages.startDictation()
                        } else {
                            microphone.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onSend = { text ->
                        if (messages.sendReply(text)) {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.reply_sent),
                                Toast.LENGTH_LONG,
                            ).show()
                            // Navigate first, clear second. Clearing first
                            // leaves two screens composed with nothing to show.
                            goHome()
                            messages.closeMessage()
                        }
                    },
                    onType = { runCatching { typed.launch(typeReplyIntent()) } },
                    onHome = {
                        messages.closeMessage()
                        goHome()
                    },
                    onBack = {
                        messages.cancelDictation()
                        goBack()
                    },
                )
            }
        }

        composable(Routes.MAGNIFIER) {
            // Read fresh on every visit. The permission can be revoked from
            // system settings while the app sits in the background, and a
            // remembered yes would leave a black rectangle with no explanation.
            var cameraGeneration by remember { mutableStateOf(0) }
            val camera = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { cameraGeneration++ }
            key(cameraGeneration) {
                MagnifierScreen(
                    onRead = { frame ->
                        seeing.readFrame(frame)
                        navController.navigate(Routes.READER)
                    },
                    onGrantCamera = { camera.launch(Manifest.permission.CAMERA) },
                    onHome = goHome,
                )
            }
        }

        composable(Routes.READER) { entry ->
            if (seeing.frozen == null) {
                PopWhenCurrent(entry, goBack)
            } else {
                ReaderScreen(
                    frame = seeing.frozen,
                    text = seeing.recognized,
                    working = seeing.working,
                    speaking = seeing.speaking,
                    canSpeak = seeing.canSpeak,
                    filter = seeing.filter,
                    onFilter = { seeing.filter = it },
                    onRead = { seeing.speakRecognized() },
                    onStop = { seeing.stopSpeaking() },
                    onHome = {
                        goHome()
                        seeing.clearFrame()
                    },
                    onBack = {
                        goBack()
                        seeing.clearFrame()
                    },
                )
            }
        }

        composable(Routes.PHOTOS) {
            var photoGeneration by remember { mutableStateOf(0) }
            val ask = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { photoGeneration++ }
            val hasPhotos = remember(photoGeneration) { seeing.photoStore.hasPermission() }
            // Off the main thread. A media store query walks every picture on
            // the phone, and running it during composition is the same stall
            // the app list had.
            val photos by produceState(initialValue = emptyList(), hasPhotos, photoGeneration) {
                value = withContext(Dispatchers.IO) { seeing.photoStore.load() }
            }
            PhotosScreen(
                photos = photos,
                hasPermission = hasPhotos,
                loadFrame = { seeing.loadPhoto(it) },
                onGrantPhotos = {
                    ask.launch(
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                    )
                },
                onOpenGallery = { openGallery(activity) },
                onSpeakCaption = { seeing.say(it) },
                canSpeak = seeing.canSpeak,
                onHome = goHome,
            )
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

/**
 * Leaves a screen that has nothing left to show, but only while it is the
 * screen actually on top. A destination further down the back stack is being
 * disposed, not visited, and popping on its behalf takes an entry that belongs
 * to somebody else.
 */
@Composable
private fun PopWhenCurrent(entry: NavBackStackEntry, pop: () -> Unit) {
    LaunchedEffect(entry) {
        if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) pop()
    }
}

/**
 * The six phrases from grid 09, read from resources so they translate. Stored
 * phrases replace them once a helper edits the list.
 */
fun defaultPhrases(context: android.content.Context): List<String> = listOf(
    context.getString(R.string.reply_phrase_yes),
    context.getString(R.string.reply_phrase_no),
    context.getString(R.string.reply_phrase_call_me),
    context.getString(R.string.reply_phrase_thank_you),
    context.getString(R.string.reply_phrase_love_you),
    context.getString(R.string.reply_phrase_on_my_way),
)

/**
 * The typed fallback. The speech activity is used rather than a text field
 * because it brings the system keyboard up on its own screen, at the system's
 * own size, without the launcher having to host and resize a text field behind
 * it. People who want to type get the keyboard they already know.
 */
fun typeReplyIntent(): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
    }

/** The one money related link in the app, shared by Settings and About. */
fun openSupportLink(context: android.content.Context) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        android.net.Uri.parse("https://buymeacoffee.com/kamsiob"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** The phone's own gallery, for when there is nothing here to show. */
private fun openGallery(activity: MainActivity) {
    val intent = Intent(Intent.ACTION_VIEW).setType("image/*")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { activity.startActivity(intent) }.isSuccess) return
    val fallback = Intent.makeMainSelectorActivity(
        Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { activity.startActivity(fallback) }
}

private fun launchCamera(activity: MainActivity) {
    val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { activity.startActivity(intent) }
}
