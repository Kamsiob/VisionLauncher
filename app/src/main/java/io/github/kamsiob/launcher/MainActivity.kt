package io.github.kamsiob.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kamsiob.launcher.attention.AttentionWatcher
import io.github.kamsiob.launcher.data.AppsRepository
import io.github.kamsiob.launcher.data.BuiltIn
import io.github.kamsiob.launcher.data.Settings
import io.github.kamsiob.launcher.nav.SystemDestination
import io.github.kamsiob.launcher.ui.apps.MoreAppsScreen
import io.github.kamsiob.launcher.ui.home.HomeScreen
import io.github.kamsiob.launcher.ui.home.NotBuiltScreen
import io.github.kamsiob.launcher.ui.theme.LauncherTheme
import io.github.kamsiob.launcher.ui.threshold.ThresholdScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

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
                    settings = settings,
                    homePressed = homePressed,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing Home while inside the launcher relaunches it; snap back to
        // the home screen so Home always means home.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            lifecycleScope.launch { homePressed.emit(Unit) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::attentionWatcher.isInitialized) attentionWatcher.refresh()
    }
}

object Routes {
    const val HOME = "home"
    const val MORE_APPS = "moreapps"
    const val NOT_BUILT = "notbuilt/{feature}"
    const val THRESHOLD = "threshold/{dest}"

    fun notBuilt(feature: BuiltIn) = "notbuilt/${feature.id}"
    fun threshold(dest: SystemDestination) = "threshold/${dest.id}"
}

@Composable
fun LauncherNav(
    activity: MainActivity,
    app: LauncherApplication,
    apps: AppsRepository,
    settings: Settings,
    homePressed: MutableSharedFlow<Unit>,
) {
    val navController = rememberNavController()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val layout by app.layoutStore.layout
        .collectAsStateWithLifecycle(initialValue = app.layoutStore.defaultLayout)

    LaunchedEffect(homePressed) {
        homePressed.collect {
            navController.popBackStack(Routes.HOME, inclusive = false)
        }
    }

    val goHome: () -> Unit = { navController.popBackStack(Routes.HOME, inclusive = false) }
    val handoff: (SystemDestination) -> Unit = { dest ->
        if (dest.id in settings.dismissedThresholds) {
            dest.launch(activity)
        } else {
            navController.navigate(Routes.threshold(dest))
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                layout = layout,
                apps = apps,
                watcher = activity.attentionWatcher,
                onOpenFeature = { feature ->
                    when (feature) {
                        BuiltIn.CAMERA -> launchCamera(activity)
                        BuiltIn.MESSAGES, BuiltIn.MAGNIFIER, BuiltIn.PHOTOS ->
                            navController.navigate(Routes.notBuilt(feature))
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
                onHome = goHome,
                onLaunched = goHome,
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
                goHome()
            } else {
                ThresholdScreen(
                    destination = dest,
                    onContinue = {
                        navController.popBackStack()
                        dest.launch(activity)
                    },
                    onStay = { navController.popBackStack() },
                    onDismissForever = {
                        scope.launch {
                            app.settingsStore.dismissThreshold(dest.id)
                        }
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
