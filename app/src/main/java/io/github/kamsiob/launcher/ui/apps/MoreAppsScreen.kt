package io.github.kamsiob.launcher.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.data.AppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.RowKey
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle

/**
 * Grid 20 pattern. The full alphabetical app list behind More apps. Search
 * exists for the helper's benefit; the list is fully usable without it.
 */
@Composable
fun MoreAppsScreen(
    apps: AppsRepository,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    onLaunched: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    // The initial value must not be the query itself. Enumerating every
    // launchable activity, resolving each label and sorting the result took a
    // measured 150ms at the 95th percentile, and as a produceState initial
    // value that ran during composition on the main thread, so opening this
    // screen dropped roughly eighteen frames. It runs on the IO dispatcher now
    // and the screen appears immediately with its list arriving a moment later.
    val allApps by produceState(initialValue = emptyList<AppsRepository.AppEntry>(), apps) {
        value = withContext(Dispatchers.IO) { apps.launchableApps() }
        apps.changes().collect {
            value = withContext(Dispatchers.IO) { apps.launchableApps() }
        }
    }
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val iconPx = with(density) { Dimens.appIcon.roundToPx() }
    val shown = if (query.isBlank()) allApps else allApps.filter {
        it.label.contains(query.trim(), ignoreCase = true)
    }

    val searchDescription = stringResource(R.string.a11y_search_field)
    ScreenFrame(scrollable = false) {
        TopBar(onHome = onHome)
        ScreenTitle(stringResource(R.string.more_apps_title))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            textStyle = bodyStyle(size = TypeScale.keyLabelSmall, weight = FontWeight.Medium),
            placeholder = {
                Text(
                    text = stringResource(R.string.more_apps_search_hint),
                    style = bodyStyle(size = TypeScale.keyLabelSmall, weight = FontWeight.Medium),
                    color = palette.textSoft,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
                focusedContainerColor = palette.card,
                unfocusedContainerColor = palette.card,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.hairline,
                cursorColor = palette.accent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = searchDescription },
        )
        if (query.isNotBlank() && shown.isEmpty()) {
            NoteText(stringResource(R.string.more_apps_none_found, query.trim()))
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap),
        ) {
            // Settings lives here rather than on a home tile, because the home
            // grid holds what the person uses daily and this is the one door
            // that has to exist somewhere findable.
            if (query.isBlank()) {
                item(key = "settings") {
                    RowKey(
                        label = stringResource(R.string.settings_title),
                        icon = LineIcons.accessibilitySun,
                        onClick = onSettings,
                    )
                }
            }
            items(shown, key = { it.key + it.isWorkProfile }) { entry ->
                val label = if (entry.isWorkProfile) {
                    stringResource(R.string.more_apps_work_profile, entry.label)
                } else {
                    entry.label
                }
                RowKey(
                    label = label,
                    appIcon = apps.icon(entry, iconPx),
                    onClick = {
                        apps.launch(entry)
                        onLaunched()
                    },
                )
            }
        }
    }
}
