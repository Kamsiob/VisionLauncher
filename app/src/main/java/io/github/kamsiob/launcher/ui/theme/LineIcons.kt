package io.github.kamsiob.launcher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The navy line icons for built in features, ported path for path from the
 * SVGs in design/design-grid-v4.html: a 24 unit viewport, round caps and
 * joins, no fills. Icons are drawn in black and tinted at the point of use, so
 * one set serves all three themes.
 *
 * The stroke is 2.2 rather than the grid's 2.7 because the icons grew. A
 * stroke in viewport units scales with the icon, so drawing the old weight at
 * the new sizes would have thickened every line by a quarter and closed the
 * gaps inside the camera and the photo frame until they read as blots. At 2.2
 * the absolute stroke lands within a tenth of a dp of what it was before at
 * every size in Dimens: the icons look exactly as bold and simply have more
 * room inside them. Change the sizes and this number without checking that
 * arithmetic and the shapes will fuse.
 */
private fun lineIcon(name: String, vararg paths: String, stroke: Float = 2.2f): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        for (d in paths) {
            addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            )
        }
    }.build()

private fun circle(cx: Float, cy: Float, r: Float): String =
    "M$cx,$cy m${-r},0 a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float): String =
    "M${x + rx},$y h${w - 2 * rx} a$rx,$rx 0 0 1 $rx,$rx v${h - 2 * rx} " +
        "a$rx,$rx 0 0 1 ${-rx},$rx h${-(w - 2 * rx)} a$rx,$rx 0 0 1 ${-rx},${-rx} " +
        "v${-(h - 2 * rx)} a$rx,$rx 0 0 1 $rx,${-rx} z"

object LineIcons {
    val call = lineIcon(
        "call",
        "M5 4c0 8 7 15 15 15v-4l-4-1-2 2c-3-1.5-4.5-3-6-6l2-2-1-4z",
    )
    val messages = lineIcon(
        "messages",
        rect(3f, 5f, 18f, 14f, 2f),
        "M3 7l9 6 9-6",
    )
    val magnifier = lineIcon(
        "magnifier",
        circle(10f, 10f, 6f),
        "M15 15l6 6",
    )
    val camera = lineIcon(
        "camera",
        rect(3f, 7f, 18f, 13f, 2f),
        // The lens was r=4 with only two units clearing the body's top edge,
        // which the stroke closed entirely once the icon was drawn large.
        // Smaller and centered leaves the ring visibly free on both sides.
        circle(12f, 13.5f, 3.5f),
        "M8 7l2-3h4l2 3",
    )
    val photos = lineIcon(
        "photos",
        rect(3f, 4f, 18f, 16f, 2f),
        // At r=2 the sun was four units across and the stroke filled it in.
        circle(9f, 9.5f, 2.8f),
        "M3 17l5-5 4 4 3-3 6 6",
    )
    val alarms = lineIcon(
        "alarms",
        circle(12f, 13f, 8f),
        "M12 9v4l3 2",
        "M5 3L3 5",
        "M19 3l2 2",
    )
    val home = lineIcon(
        "home",
        "M3 11l9-8 9 8",
        "M5 10v10h14V10",
    )
    val sun = lineIcon(
        "sun",
        circle(12f, 14f, 4f),
        "M12 6v2M5 14H3M21 14h-2M6.6 8.6L5.2 7.2M17.4 8.6l1.4-1.4",
        "M3 19h18",
        stroke = 2.1f,
    )
    val moon = lineIcon(
        "moon",
        "M19 14a7 7 0 0 1-9-9 7 7 0 1 0 9 9z",
        "M3 19h18",
        stroke = 2.1f,
    )
    val bellOff = lineIcon(
        "bellOff",
        "M6 8a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6",
        "M10 18a2 2 0 0 0 4 0",
        "M3 3l18 18",
    )
    val bellQuiet = lineIcon(
        "bellQuiet",
        "M6 8a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6",
        "M10 18a2 2 0 0 0 4 0",
    )
    val battery = lineIcon(
        "battery",
        rect(3f, 8f, 16f, 9f, 2f),
        "M19 11h2v3h-2",
        "M6 11v3",
    )
    val airplane = lineIcon(
        "airplane",
        "M10 9L4 11.5v2l6-1v4l-2 2V20l4-1 4 1v-1.5l-2-2v-4l6 1v-2L14 9V5a2 2 0 0 0-4 0z",
    )
    val noNetwork = lineIcon(
        "noNetwork",
        "M1 1l22 22",
        "M16.72 11.06A10.94 10.94 0 0 1 19 12.55",
        "M5 12.55a10.94 10.94 0 0 1 5.17-2.39",
        "M10.71 5.05A16 16 0 0 1 22.58 9",
        "M1.42 9a15.91 15.91 0 0 1 4.7-2.88",
        "M8.53 16.11a6 6 0 0 1 6.95 0",
        "M12 20h.01",
    )
    val storage = lineIcon(
        "storage",
        "M12 2C7 2 3 3.34 3 5s4 3 9 3 9-1.34 9-3-4-3-9-3z",
        "M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5",
        "M3 12c0 1.66 4 3 9 3s9-1.34 9-3",
    )
    val sleep = lineIcon(
        "sleep",
        "M19 14a7 7 0 0 1-9-9 7 7 0 1 0 9 9z",
    )
    val person = lineIcon(
        "person",
        circle(9f, 8f, 3.5f),
        "M3 20c0-3.5 2.5-6 6-6s6 2.5 6 6",
        "M17 8h5M19.5 5.5v5",
    )
    val dialpad = lineIcon(
        "dialpad",
        rect(5f, 3f, 14f, 18f, 2f),
        "M9 7h.01M12 7h.01M15 7h.01M9 11h.01M12 11h.01M15 11h.01M9 15h.01M12 15h.01M15 15h.01",
    )
    val grid = lineIcon(
        "grid",
        rect(4f, 4f, 7f, 7f, 1.5f),
        rect(13f, 4f, 7f, 7f, 1.5f),
        rect(4f, 13f, 7f, 7f, 1.5f),
        rect(13f, 13f, 7f, 7f, 1.5f),
    )
    val restore = lineIcon(
        "restore",
        "M4 12a8 8 0 1 0 3-6.2",
        "M4 4v5h5",
    )
    val accessibilitySun = lineIcon(
        "accessibilitySun",
        circle(12f, 12f, 4f),
        "M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M19.1 4.9L17 7M7 17l-2.1 2.1",
    )
    val lock = lineIcon(
        "lock",
        rect(5f, 10f, 14f, 10f, 2f),
        "M8 10V7a4 4 0 0 1 8 0v3",
    )
    val door = lineIcon(
        "door",
        "M4 4h8v16H4z",
        "M15 12h6",
        "M18 9l3 3-3 3",
    )
    val check = lineIcon(
        "check",
        "M4 12l5 5 11-11",
    )
    val plus = lineIcon(
        "plus",
        "M12 5v14M5 12h14",
    )
    val displaySize = lineIcon(
        "displaySize",
        "M4 18V6M4 18l5-12 5 12M6 14h6",
        "M17 8h4M19 6v4",
    )
    val magnifyPlus = lineIcon(
        "magnifyPlus",
        circle(10f, 10f, 6f),
        "M15 15l6 6",
        "M8 10h4M10 8v4",
    )
    val ear = lineIcon(
        "ear",
        "M7 9a5 5 0 0 1 10 0c0 3-2 4-2 7a3 3 0 0 1-6 0c0-3-2-4-2-7z",
    )
    val captions = lineIcon(
        "captions",
        rect(3f, 6f, 18f, 12f, 2f),
        "M6 14h6M14 14h4M6 11h3",
    )
    val speaker = lineIcon(
        "speaker",
        "M4 10v4h4l5 4V6l-5 4H4z",
        "M17 9a5 5 0 0 1 0 6M19.5 6.5a9 9 0 0 1 0 11",
    )
    val heart = lineIcon(
        "heart",
        "M12 20s-7-4.5-7-9.5A4 4 0 0 1 12 7a4 4 0 0 1 7 3.5c0 5-7 9.5-7 9.5z",
    )
    val shield = lineIcon(
        "shield",
        "M12 3l8 4v6c0 5-3.5 7.5-8 8-4.5-.5-8-3-8-8V7z",
        "M12 8v5M12 16v.2",
    )
}
