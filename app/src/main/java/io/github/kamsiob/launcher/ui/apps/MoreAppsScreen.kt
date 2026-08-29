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
import io.github.kamsiob.launcher.ui.components.RowKey
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens
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
    onHome: () -> Unit,
    onLaunched: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val allApps by produceState(initialValue = apps.launchableApps(), apps) {
        apps.changes().collect { value = apps.launchableApps() }
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap),
        ) {
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
