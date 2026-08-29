package io.github.kamsiob.launcher.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.support.Haptics
import io.github.kamsiob.launcher.support.debouncedClickable
import io.github.kamsiob.launcher.today.TodayCard
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalOutlined
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import io.github.kamsiob.launcher.ui.theme.monoStyle

/**
 * Grid 13. What is happening today, written by the person or their helper.
 *
 * Green appears only on completion, so the screen is never green while
 * something is still outstanding. The app contains no medical content of its
 * own: every word on a card was typed by somebody. See MASTER_SPEC 5.9.
 */
@Composable
fun TodayScreen(
    cards: List<TodayCard>,
    today: Int,
    formatTime: (Int, Int) -> String,
    onToggleDone: (TodayCard, Boolean) -> Unit,
    onAdd: () -> Unit,
    onEdit: (TodayCard) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.today_title))

        if (cards.isEmpty()) NoteText(stringResource(R.string.today_none))

        cards.forEach { card ->
            TodayCardView(
                card = card,
                done = card.isDoneToday(today),
                formatTime = formatTime,
                onToggleDone = { onToggleDone(card, it) },
                onEdit = { onEdit(card) },
            )
        }

        ApplianceKey(
            label = stringResource(R.string.today_add),
            icon = LineIcons.plus,
            onClick = onAdd,
            minHeight = Dimens.keySmall,
        )

        NoteText(stringResource(R.string.today_not_medical))
    }
}

@Composable
private fun TodayCardView(
    card: TodayCard,
    done: Boolean,
    formatTime: (Int, Int) -> String,
    onToggleDone: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val palette = LocalPalette.current
    val view = LocalView.current
    val outlined = LocalOutlined.current
    val at = formatTime(card.hour, card.minute)
    val doneAt = card.doneAtMinutes?.let { formatTime(it / 60, it % 60) }
    val state = if (done && doneAt != null) {
        stringResource(R.string.a11y_today_done, doneAt)
    } else {
        stringResource(R.string.a11y_today_not_done)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusKey))
            .background(if (done) palette.statusBg else palette.card)
            .then(
                if (outlined) {
                    Modifier.border(3.dp, palette.outline, RoundedCornerShape(Dimens.radiusKey))
                } else {
                    Modifier
                }
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The time and the words are one target that opens the editor, and Done
        // is a separate one below it. Making the whole card clickable would put
        // the Done key inside another clickable, which reads as one confused
        // control to a screen reader instead of two clear ones.
        //
        // This description states the status and stops there. It used to end
        // with "tap to mark it done", which was the Done key's instruction on a
        // target that opens the editor, so the sentence told somebody using a
        // screen reader to do the one thing this tap does not do.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .debouncedClickable(
                    onClickLabel = stringResource(R.string.today_change),
                    role = Role.Button,
                ) { onEdit() }
                .clearAndSetSemantics {
                    contentDescription = "${card.what}, $at. $state"
                },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = at,
                style = monoStyle(size = TypeScale.rowMeta),
                color = palette.note,
            )
            Text(
                text = card.what,
                style = bodyStyle(size = TypeScale.tileLabel, weight = FontWeight.Bold),
                color = palette.text,
            )
        }

        if (done && doneAt != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .debouncedClickable(
                        onClickLabel = stringResource(R.string.today_undo_done),
                        role = Role.Button,
                    ) {
                        Haptics.tap(view)
                        onToggleDone(false)
                    },
            ) {
                Icon(
                    imageVector = LineIcons.check,
                    contentDescription = null,
                    tint = palette.green,
                    modifier = Modifier.size(Dimens.keyIconSmall),
                )
                Text(
                    text = stringResource(R.string.today_done_at, doneAt),
                    style = bodyStyle(size = TypeScale.sublabel, weight = FontWeight.Bold),
                    color = palette.text,
                )
            }
        } else {
            ApplianceKey(
                label = stringResource(R.string.today_done),
                onClick = { onToggleDone(true) },
                style = KeyStyle.PRIMARY,
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
                committing = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
