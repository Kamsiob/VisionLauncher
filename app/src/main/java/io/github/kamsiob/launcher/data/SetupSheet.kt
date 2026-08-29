package io.github.kamsiob.launcher.data

import android.content.Context
import io.github.kamsiob.launcher.R

/**
 * The printable setup sheet.
 *
 * A page a helper can print and leave by the phone, or post to a relative who
 * is doing the setting up. Written as HTML because every Android phone can open
 * and print one, and because a printed page is the one format that still works
 * when the phone is the thing that has gone wrong.
 *
 * Deliberately contains no codes, no numbers that could be misused, and no
 * medical content. Phone numbers are shown because that is the point of the
 * sheet, and the person printing it already has them.
 */
object SetupSheet {

    fun build(context: Context, setup: Setup, appName: String): String {
        val favorites = setup.favorites.joinToString("\n") { favorite ->
            val relation = favorite.relationship.takeIf { it.isNotBlank() }
                ?.let { " <span class=\"rel\">${escape(it)}</span>" }.orEmpty()
            "      <li><b>${escape(favorite.name)}</b>$relation<br>${escape(favorite.number)}</li>"
        }.ifEmpty { "      <li class=\"none\">Nobody has been added yet.</li>" }

        val emergency = setup.emergencyContact?.let { contact ->
            "<p><b>${escape(contact.name)}</b><br>${escape(contact.number)}</p>"
        } ?: "<p class=\"none\">Nobody has been chosen yet.</p>"

        val phrases = setup.replyPhrases.joinToString("\n") {
            "      <li>${escape(it)}</li>"
        }.ifEmpty { "      <li class=\"none\">The usual six are in use.</li>" }

        val cards = setup.todayCards.joinToString("\n") { card ->
            "      <li><b>%d:%02d</b> ${escape(card.what)}</li>".format(card.hour, card.minute)
        }.ifEmpty { "      <li class=\"none\">Nothing is written down.</li>" }

        // Inline styles and no external anything, so the sheet prints the same
        // on a phone with no network as on a computer with one.
        return """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${escape(appName)} setup</title>
<style>
  body { font-family: Georgia, "Times New Roman", serif; color: #16181C;
         max-width: 40em; margin: 2em auto; padding: 0 1.5em; line-height: 1.5; }
  h1 { font-size: 1.9em; margin-bottom: 0.1em; }
  h2 { font-size: 1.25em; margin-top: 1.6em; border-bottom: 2px solid #16181C;
       padding-bottom: 0.2em; }
  .sub { color: #55595F; margin-top: 0; }
  ul { padding-left: 1.2em; }
  li { margin-bottom: 0.6em; }
  .rel { color: #55595F; font-style: italic; }
  .none { color: #55595F; font-style: italic; }
  .note { margin-top: 2.5em; padding-top: 1em; border-top: 1px solid #C9C2B4;
          color: #55595F; font-size: 0.9em; }
  @media print { body { margin: 0; max-width: none; } }
</style>
</head>
<body>
  <h1>${escape(appName)}</h1>
  <p class="sub">How this phone is set up. Keep this page somewhere findable.</p>

  <h2>People on the Call screen</h2>
  <ul>
$favorites
  </ul>

  <h2>The emergency person</h2>
  $emergency

  <h2>One tap replies</h2>
  <ul>
$phrases
  </ul>

  <h2>Things that happen today</h2>
  <ul>
$cards
  </ul>

  <p class="note">
    Written by you or your helper. Not medical advice.
    Everything on this phone stays on this phone: this app has no internet
    permission, which anybody can check in the phone's own app info screen.
  </p>
</body>
</html>
""".trimIndent()
    }

    /** HTML escaping, because a contact called "Mum &amp; Dad" must not break the page. */
    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
