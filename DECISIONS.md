# DECISIONS

Settled decisions and, more importantly, rejected alternatives with the reason. If something here gets reopened, there should be a new fact, not a fresh opinion.

Add to this file whenever a real decision is made. Never delete an entry; supersede it with a dated note.

## D1. No AccessibilityService, anywhere

Google Play allows AccessibilityService broadly but only apps that help people with disabilities may set `isAccessibilityTool` and skip prominent disclosure, and the policy explicitly lists launchers as ineligible. Declaring one means the permission declaration form, review, and ongoing takedown risk. Everything this app needs is achievable with narrower sanctioned APIs. If a future feature appears to need an AccessibilityService, treat that as a design smell and build a handoff instead.

## D2. Do not take the default SMS role

RCS is closed to third party apps. Google has kept the RCS API on a private allowlist since 2019 and no third party messaging app has access. Taking `ROLE_SMS` would drop the user to plain SMS and MMS, which means heavily compressed family photos, degraded group chats, no end to end encryption, and broken RCS with iPhone family members. All four of those hurt this audience specifically. Rejected.

## D3. Messaging through NotificationListenerService with RemoteInput replies

The consequence of D2. Reading notifications and firing the notification's own inline reply action keeps every message on its native transport, so RCS, WhatsApp, and Telegram all keep working normally and land in one large text inbox.

Accepted cost: this pipeline is fragile. See D4.

## D4. Messages are persisted locally

The review found that an inbox which forgets anything leaving the notification shade is a usability trap that matches no user's mental model of messages. Every message the listener sees is written to a local database and the inbox reads from there. Never transmitted anywhere.

## D5. Battery optimization onboarding is mandatory

OEM battery optimizers routinely kill notification listeners, and Chinese ROMs are the worst offenders. Without an exemption the messaging feature works in the demo and dies in month six. The onboarding step is a first class requirement, the state is re-checked later, and losing the exemption raises an item in the attention queue.

## D6. No default dialer

`ROLE_DIALER` brings heavier obligations and Play scrutiny for little gain. `ACTION_CALL` and `ACTION_DIAL` behind a custom front end deliver the large button calling experience without becoming responsible for the whole call stack.

## D7. Emergency hands off, never reimplements

The Emergency screen offers a 911 key that opens the system emergency flow and an alert key that calls a chosen contact and texts a location. The app does not implement emergency calling, does not claim fall detection (no third party API exists), and states plainly that both need a signal and that this is not a monitored medical alert service. Competitors overpromise here and fail silently, which is worse than not offering it.

## D8. No keyboard in v1

A large key accessible IME would close the last gap, but building a robust keyboard is substantial hand tuned work beyond AI assisted development, and it carries its own Play and security surface. Rejected for v1. The reply screen answers the same problem with on device voice dictation, six preset phrases, and the system keyboard as a fallback. Revisit only with a hand coding collaborator.

## D9. On device speech recognition only

`createOnDeviceSpeechRecognizer()` on API 33 and above. Below that, hide the voice key. Network speech recognition is never used, in any fallback, under any circumstance, because it would break the zero data promise.

## D10. No QR or cloud config transfer in v1

Deferred by explicit instruction. Caregiver transfer in v1 is a printable setup sheet and a local settings file only, both fully offline. The QR path was designed and is local only, phone to phone, with no server; it is a v1.x candidate, not a canceled idea.

## D11. Fixed layout, no adaptive reordering

Procedural memory survives aging far better than episodic memory, so a layout the hands can learn is worth more than a clever one. This is why the app rejects the adaptive list model that elegant minimalist launchers use. Nothing on the home screen moves unless the user moves it.

## D12. Arranging happens on the real grid, with taps only

An earlier version used a numbered vertical list with up and down keys. Rejected because mapping list position 3 back to a two column grid is a translation the user should never have to do. Drag and drop was rejected outright: tremor turns drags into misfires, and older adults prefer click to designs over direct manipulation.

Final model: tap the app, it lifts, tap the destination, they trade places. Plus Put it first as a one tap shortcut that also solves any off screen destination. Done is permanently visible so nobody gets stranded in the mode.

## D13. Light default, dark by choice, Outlined by choice

Roughly a third of adults have astigmatism and suffer halation on dark themes, while cataract and photophobia users genuinely need dark. There is no single right answer, so light is the default and dark is one obvious tap away.

Outlined was added after the design review found that soft shadows carry far less edge contrast than borders and likely fail the 3:1 non text contrast threshold, which matters enormously for the low vision audience this app exists to serve. If real user testing shows the default shadows are insufficient, make Outlined the default rather than weakening it.

## D14. Red is reserved for emergency, lamp yellow is the caution voice

Reserving red left the design with no way to signal errors and destructive actions, which the review flagged. Resolved by making the lamp treatment the official caution voice: warm yellow, an icon, plain words, and an explicit undo. Nothing in this app is destructive enough to need red.

## D15. Instructional footnotes are banned

Stigma research names obvious instructions as an accentuated social signifier that makes older adults perceive a product as being for the impaired. Notes must qualify as a brand promise, a Play policy requirement, or a statement of where something will happen. Everything else is deleted and the interface proves the point instead.

## D16. Young Serif covers the clock and the short headings

It is a display face, license clean under SIL OFL, and gives the product warmth. Display serifs are a poor choice for labels and body text at large sizes, so screen titles moved to Atkinson at weight 800.

**Corrected, August 29, 2026.** This entry claimed the face was confined to the clock and concluded that "being Latin only is acceptable because it only renders digits". Both halves were wrong from the first commit. `SerifHeading` uses Young Serif for six pieces of ordinary prose: "You can't break anything.", "You're all set.", each onboarding step title, the threshold heading, and "Here is your new home screen". And the clock is not digits-only either: `DayPart.clockText` formats with the default locale, and Arabic locales use Arabic-Indic numerals, which Young Serif has no glyphs for.

The honest argument, which happens to reach the same conclusion: these are six short strings on low traffic screens plus the clock. Outside Latin they fall to the system serif through the Noto chain, so those screens keep their words and lose their face. That is an acceptable trade for a decorative choice, and it is a trade rather than a non-issue. Serif line heights must be checked against the fallback faces during Stage 4, since a taller fallback can clip a wrapped heading.

## D17. Atkinson Hyperlegible is chosen honestly

No peer reviewed study shows it beats a standard sans for low vision reading speed. The evidence says print size, contrast, and spacing dominate. It is chosen for genuine character disambiguation, an open license, and coherence, and it will never be marketed as clinically superior.

## D18. Tesseract is the OCR engine, ML Kit is the fallback

Tesseract is Apache 2.0, fully open source, fully offline, and compatible with AGPLv3. ML Kit is free and easy but a proprietary Google binary, which dents the fully local promise. Use Tesseract; switch only if accuracy on real medication labels proves inadequate, and disclose the dependency if switched.

## D19. Weather is opt in and off by default

The daily use research says weather is the single most wanted glanceable for this audience, so it earns a place on the masthead date line. It stays off by default and opt in because it is the only feature that might touch the network.

## D20. The name is undecided

Placeholder `Launcher by Kamsiob`, application ID `io.github.kamsiob.launcher`. The application ID is permanent once published, so this must be settled before the first Play upload. The `io.github` namespace is used across the portfolio because kamsiob.com returns 406 to automated well known path verification.

**Superseded, August 29, 2026.** The name is VisionLauncher and the application ID is `io.github.kamsiob.launcher`, both chosen by the user in the first build session. The placeholder ID became the real ID deliberately, not by drift: the `io.github` namespace is verifiable through the GitHub account and is already the portfolio convention.

## D21. Signed commits from the first commit, history never rewritten

Every commit is SSH signed with the portfolio signing key, matching the Health Trail setup. Vigilant mode applies at the account level. History is never rewritten to add signatures to old commits; signing starts at the first commit and stays on.

## D22. One application ID, no debug suffix

The portfolio convention elsewhere is a `.debug` suffix so debug and release installs coexist. This project deliberately drops it. The standing rule is exactly one copy of this app per device, ever, and two application IDs is two copies. Debug and release builds collide on purpose; moving between them means uninstalling first, and that is the intended behavior.

## D23. Unbuilt tiles are honest, not hidden

Stage 1 ships the home screen with its full default tile set, but Messages, Magnifier, and Photos belong to later stages. Their tiles open a plain screen that says this part is not built yet and, where the phone has its own way to do the job, offers that instead: the message app for Messages, the gallery for Photos. Hiding the tiles would make the home layout shift as stages land, which breaks the layout the hands are learning. The interstitial deletes itself stage by stage. Camera is not an interstitial; it launches the phone's camera app directly, because the spec has no camera screen of its own.

## D24. The tile grid drops to one column at large font scales

Found by running the app at 200 percent system font scale, not by reading the spec. A 128dp tile in two columns physically cannot hold a word like "Magnifier" at a size this audience can read. Both obvious answers are wrong: breaking the word across two lines produces "Magnifi er", and shrinking the label below the row metadata size defeats the entire point of the app.

So above a combined scale of 1.5, counting the system font scale and the user's own text step together, the home grid and arranging mode both go to one column. The tile order never changes, which is what procedural memory actually holds onto, and every label keeps its full size. This is the same reasoning as D11: the layout the hands learn is worth more than the layout that looks tidier.

Tile labels also carry a step based auto size with the grid's 28sp as the maximum, which handles a long third party app name in the two column case without ever exceeding the specified size.

The same rule applies to the three Look cards in Settings, found the same way: at 200 percent, "Outlined" was clipped to "Outlir". They stack vertically above the same threshold.

Where the layout genuinely cannot give way, the type does. A keypad has to stay three columns wide, so "Erase" and "Clear" step down in size rather than breaking into "Eras e". The rule across the app: a phrase may wrap across lines, a single word never splits, and the layout gives way before the type does.

## D25. Settings is reached from More apps, not from a home tile

The grid puts no Settings tile on the home screen, and the six tiles belong to what a person uses daily. Settings sits pinned at the top of the More apps list instead, above the alphabetical apps, where it is findable without spending a tile. It is hidden from the list while a search is running, because a search for an app name should return apps.

## D26. Every key carries its own TalkBack label, never an inherited one

Found on a real device, not in review. Relying on a key's visible text to merge into its clickable node left the node with no label at all: the accessibility tree showed a clickable button with an empty name for the Done chip, "Add an app", and every other key labeled only by its text. TalkBack would have announced those as an unnamed button.

So every key sets its label explicitly through `clearAndSetSemantics`: the appliance key speaks its label and sublabel, the tile speaks its label, the row key speaks its label and metadata, and the masthead and status pill each collapse to a single focus stop rather than exposing their parts twice. A caller can still override with a fuller sentence, which is how the Emergency key says it opens a screen rather than placing a call.

The lesson generalizes: the accessibility tree is the thing to check, and it has to be checked on the device.

## D27. Adding an app happens inside the arranging session

Found by pressing Done after adding an app and getting no confirmation screen. Add an app was a separate route writing straight to the layout store, which meant the new tile bypassed both the working layout and the Keep it preview that spec 5.12 requires of every finish. It is now a mode inside the arranging screen, like the action sheet, so one session holds every change and one Keep confirms them all.

## D28. The threshold only promises the Home return when it can keep it

"When you're done, press Home and you'll come straight back" is true only while this app holds the home role. Tested on a device where it did not, pressing Home landed on the stock launcher, which is precisely the disorientation the threshold screen exists to prevent. The line is now conditional on `isRoleHeld(ROLE_HOME)`, and otherwise says to come back the usual way. Overpromising on this screen would undercut the one screen whose whole job is honesty about what happens next.

## D29. The zero network promise is a build gate, not a sentence

The app holds no INTERNET permission, so Android refuses any connection it could attempt and a person can check that themselves in the app info screen. A library could introduce the permission through manifest merging without anyone noticing, so the merged manifest is checked on every build and the build fails if it appears.

This turns the README's claim from a promise into a checkable fact, which is the standard the rest of the portfolio holds. When the opt-in weather glance ships, this gate is what has to be changed deliberately, in the same commit that adds the feature and the README sentence describing it.

## D30. The restore point is the layout before the change, not after it

"Put my screen back" was a lie. Every Keep wrote the snapshot and the current layout to the same value, which made restoring a provable no operation, and the screen still announced "Your home screen is back the way it was." Found by arranging a tile, keeping it, tapping the restore key, and watching the layout not move.

A false confirmation is the worst defect this app can carry. Every other promise it makes, the attention queue's repair keys, the undo strip, the honesty line on Emergency, rests on the interface telling the truth about what it just did. One key that confirms an action it did not perform undermines all of them.

The snapshot now holds the layout as it was *before* the most recent Keep, so the restore key undoes the last arranging session. The restore also reports whether it changed anything, and the screen says "Your home screen has not changed" when there was nothing to undo, rather than confirming an undo that did not happen. Five tests pin the semantics.

## D31. The Emergency key says what this phone can actually do

The alert key promised "Calls and texts where you are" while `SEND_SMS` and the location permissions were only ever checked, never requested. Onboarding asks for contacts and phone and nothing else, so on a real device the alert placed the call and the text silently never sent.

That is precisely the failure D7 was written to avoid, on the one screen where overpromising is unforgivable, and it survived because the code checked a permission it never asked for. A check without a request is not a safeguard, it is a silent failure with a conditional in front of it.

Two changes. The permissions are requested at the moment the promise is made, when the helper chooses the person to alert, which is also the moment nobody is in an emergency. And the key's subtitle is computed from what is actually granted: "Calls and texts where you are" when both are available, "Calls and sends a text" with messaging but no location, and "Calls only, because sending a text was not allowed" when the text cannot be sent. Refusing at setup, or revoking later, changes the words on the key.

The general lesson, and the third instance of it this session after D30 and the Add an app note: search the interface for sentences that assert something the code does not guarantee, then either guarantee it or change the sentence.

## D32. The Call lock is enforced on both sides of a trade

"Call always stays first" was breakable. The arranging screen guarded the tile being picked up but not the tile chosen as the destination, so tapping Photos, then Move it, then Call's spot traded them and left Call in position five while the screen still promised it could not happen.

The guard now lives in `HomeLayout.swap`, which refuses a trade when either side is locked, so the invariant holds at the data layer and no screen can violate it by forgetting a branch. The arranging screen keeps the app lifted and shows "Call always stays first." so the refusal is explained and a different destination can be chosen without starting over.

This is the fourth sentence this session that asserted something the code did not guarantee, after D30, the Add an app note, and D31. The pattern is consistent enough to name as a review technique: read every sentence the interface states as fact, then hunt for the code path that could make it false.

## D33. The 911 key fills the dialer, it does not place the call

`ACTION_DIAL` with `tel:911` opens the phone's dialer with the number entered and waits for the person to press call. `ACTION_CALL` would place it directly. The dial path is deliberate: a stray tap is the failure mode this audience is prone to, which is the same reason the Call screen's red key opens the Emergency screen rather than dialing. One extra press is a smaller cost than an accidental emergency call.

The key's subtitle said "Opens the phone's own emergency call screen", which overstated it, and now says "Opens the phone's own call screen with 911 ready".

Verified by reading the code path rather than by pressing it. Testing this key on a device with a live SIM risks placing a real emergency call, which is not a thing to do to find out what a button does. Test it on an emulator or a device with no SIM.

## D34. The icons were too small, and the grid was corrected rather than followed

The user looked at the running app and said the icons in the buttons were too small. They matched `design/design-grid-v4.html` exactly, so this was not a fidelity bug. The grid itself under-sized them.

The reason is worth writing down, because it explains how a careful design got this wrong. This project argued its touch targets from the aging literature and landed far above Android's minimum: 128dp tiles, an 88dp key floor, 96dp keypad keys. It never made the same argument about the glyphs inside those targets, so they inherited conventional interface sizes. Only the targets got the argument.

The grid is the measurement authority over prose. The user is the authority over the grid. The grid now carries the corrected values and a dated note saying why, so the two never disagree afterward.

**What changed.** Corrected once and then again after the user looked at the first result and asked for more. The final values, which the grid carries: tile glyph 46 to 80, which is 74 percent linear and 202 percent in area. Row icons 40 and 36 to 64 and 58. Home and Back key glyph 28 to 44. In-key glyphs from one flat 32 to 40, 48, or 64 depending on the key's height. Lamp glyph 32 to 48. Day part mark 32 to 42. Status dot 15 to 20. Threshold mark 64 to 88. Dial pad digits 38 to 50sp. Stroke 2.7 to 2.2, which holds the absolute stroke steady while the glyphs grow.

**What deliberately did not change.** The third party app icon stays smaller than the line icon, at 60dp against 80dp. That size difference is the quiet cue telling a built in feature from an installed app, which `DESIGN.md` relies on, and growing both to match would have erased it.

**Three structural repairs the change required.** A tile's glyph and an app's bitmap now sit in one shared 64dp slot, so the two tile kinds are identical in height; previously they differed by 6dp and the rows ragged. The empty spot was a frozen 128dp box that no icon size could reach, so any tile growth would have ragged the default home screen by up to 39dp at the app's own text steps; the grid row now sizes to its tallest child and the empty spot follows. And the Home and Back bar now stacks on the same threshold as everything else.

**One threshold, lowered to 1.3.** The tile grid, the top bar, and the three Look cards in Settings all reflow at a combined scale of 1.3 rather than 1.5. It is one number so the reflow is one behavior the hands learn instead of three. It moved down because 1.3 is the user's own largest text step, and above it a side by side layout starts shrinking labels to fit; reflowing first means the size a person chose is the size they get. This also fixed a bug that predated the icon work: "Home" clipped to "Hom" at a combined scale of 2.30.

**Icon scaling with text was considered and deferred.** Icons stay fixed dp for now. Scaling them turned out to carry real hazards: the app icon bitmap cache is keyed without size, so a scaled bitmap would be drawn larger than it was rasterized and blur at exactly the setting a low vision user chose; a scaled app icon overruns the row key's content budget at the cap; and scaling the Home glyph pulls the label's shrink point below the reflow threshold. Base sizes carry the visible fix. Scaling is a separate change with its own verification, tracked in its own issue.

This was reached through a five lens audit and three adversarial reviewers. The reviewers refuted the first proposal on all three lenses and were right to: they caught the frozen empty spot, the bitmap cache, the row budget overrun, and an arithmetic error in the reflow threshold, and they argued the first numbers were too timid to be noticed at arm's length, which was the entire point.

## D35. Arranging never loses work, whichever way the session ends

Three independent reviewers found the same defect, which is a fair sign of how visible it was once anyone looked. The arranging session lived in a plain `remember`, and only two paths ever wrote it out: the Done chip and an `exitKeepingChanges` helper. The system Back button fell straight through to the navigation graph, and the system Home press popped the back stack from `MainActivity`. Either one tore the screen out of composition with the work unwritten, in silence, with no undo. Meanwhile `MASTER_SPEC` 5.12 promised "Pressing Home mid arrange keeps completed changes and says so" and a comment directly above the helper asserted the same thing.

The sentence the spec promised was also unreachable: `arrange_kept_partial` was rendered inside the browsing branch, and both sub-screens return before reaching it, so no path could display it.

Three changes. Back is now always handled while arranging and steps out one layer at a time rather than leaving; at the outermost layer it keeps the work like Home does. The keep runs in `lifecycleScope` rather than the composition's own scope, because a scope tied to the composition is already cancelled at the moment the work most needs writing. And a `DisposableEffect` is the safety net underneath both: any exit that did not go through Keep it or Put it back still writes the work out, so no future navigation path can reintroduce the silent loss.

The Home key inside the session also went to Settings rather than home, while labeled "Home" and announcing "Go to the home screen". It goes home.

This is the fifth and sixth instance of the pattern D30 named, and the first where the false sentence sat in a code comment as well as the interface.

## D36. The first frame knows the real settings

DataStore emits asynchronously, so the first composed frames always used the defaults: the light palette and `onboardingDone = false`. Someone who chose Dark got a full screen cream flash on every cold start, and the navigation graph was built with Onboarding as its start destination before the real value arrived. On a launcher, which is the first thing a person sees after unlocking, that is not a rare edge.

A small `SharedPreferences` mirror now holds the four values the first frame needs, written from one place on every emission of the real settings so it cannot drift and so an install that predates it is corrected on its first launch. DataStore remains the source of truth; the mirror is a boot cache and nothing reads it after the first frame. SharedPreferences is the right tool here precisely because it is synchronous and tiny, which is the property DataStore deliberately does not have.

The window background is set from the same read before `setContent`, so even the frame before composition is the right color.

## D37. The status bar matches what is behind it, not the system's night mode

`themes.xml` hardcoded `windowLightStatusBar` true, which asks for dark icons. The home screen puts the navy masthead under the status bar, so the clock and battery were dark on dark. The app is edge to edge, so what sits behind the status bar depends on the screen and on the chosen Look, neither of which the system's night mode knows about.

The appearance is now set from the route and the Look together: light icons over the masthead on home and over the dark background everywhere in the dark theme, dark icons over paper.

## D38. Prose about numbers goes stale one change later

`DESIGN.md` and D34 were written to record the first icon correction and then contradicted the second one, because the grid and `Dimens.kt` were updated and the prose was not. A review caught it. Both now carry the final set.

The general lesson, which is cheaper than the habit of restating numbers: prose should say what a value is FOR and where it lives, and let the grid and the tokens carry the value. Where a document does restate a number, it is a copy that has to be maintained, and the next change is the one that forgets.

## D39. Text direction follows the text, layout direction follows the locale

MASTER_SPEC section 6 asks for Arabic with RTL mirroring "from the start", and the requirement had never actually been run. Setting the app's locale to Arabic on a device, which mirrors the layout while leaving the untranslated English strings in place, is a complete test of the half that does not need translations.

The layout mirrors correctly everywhere: the masthead right aligns, the tile grid reverses while keeping Call first in reading order, row icons move to the trailing side, and the date localizes. Compose's start and end handling was doing its job, and nothing in the app had hardcoded a left or a right.

Two things were wrong. Every text style inherited its direction from the layout, so an English sentence in an Arabic layout was laid out right to left and its full stop appeared at the visual left: the attention lamp read ".appearing". Styles now use `TextDirection.Content`, so each run of text follows its own script. That is also what keeps a Latin app name upright inside an Arabic sentence once translations land, which is the case that will actually matter.

And "Outlined" broke into "Outline / d" on the Look card, because D24's rule that a single word never splits had been applied to every key in the app and not to those three cards. It steps down in size instead now.

## D40. The alarm plays on the alarm stream, proven rather than requested

The ringing screen asked for `USAGE_ALARM` by assigning audio attributes to a `Ringtone` after it was built, and the platform ignored it: the player logged as `USAGE_NOTIFICATION_RINGTONE`. An alarm on the ringtone stream follows the ringer, so a phone left on vibrate would have shown the ringing screen in silence. This audience is exactly the one likely to keep a ringer down.

It uses `MediaPlayer` now, which takes the attributes at prepare time and cannot reinterpret them, and the vibration carries matching alarm attributes for the same reason.

Verified rather than assumed. With the device ringer on vibrate, an alarm was scheduled through the app, allowed to fire on its own, and `dumpsys audio` showed one player in `state:started` with `usage=USAGE_ALARM`. Pressing Stop returned zero playing streams.

The whole path is now tested end to end on a device: set an alarm, watch it fire at the minute, see the big face screen, stop it, and see the undo strip when it is taken off.

## D41. The app survives the settings it sends people to change

`configChanges` covered rotation and left out `fontScale` and `density`, which are precisely the two settings "See and hear better" hands people to. The first row of that six row tour opens Android's text and display size sliders, both of which apply live, so every person who used the feature came back to a launcher that had been destroyed and recreated, losing whatever they were doing. A screen built to help someone make text bigger was the screen most likely to throw away their place.

Both are declared now, along with `smallestScreenSize` and `fontWeightAdjustment`, since a display size change reports several bits at once and an undeclared one still triggers the restart. Verified on the device: an arranging session survives both a font scale change and a display size change.

The alarm's ringing screen declares the same set, for a different reason recorded in D42.

## D42. A rotation is not a person leaving

The ringing screen stopped its alarm in `onStop`, which was the right answer to an alarm that could ring with no reachable Stop. It could not tell a configuration change from a real background, so turning the phone on a nightstand destroyed and recreated the activity, and the stop ran on the way out: the alarm was silenced for good by rotating the phone.

`onStop` returns early on `isChangingConfigurations` now, and the ringing screen also declares the configuration changes it can absorb so most of them never restart it at all. `onDestroy` still releases the player on the recreate path, so nothing is orphaned either way.

This was a regression introduced in the same session that fixed the original defect, which is the ordinary cost of fixing something under a deadline and the reason the second review pass was worth running.

## D43. Reading a message does not dismiss its notification

Opening a message marks it read in the launcher's own store and leaves the
system notification exactly where it was.

The first build dismissed the notification on open, on the reasoning that the
message had been dealt with and the shade should not keep nagging. Testing on
the emulator showed what that actually costs: the reply action does not belong
to the launcher, it belongs to the notification, and it dies with it. So
reading a message removed the ability to answer it, and the reading screen
went from offering Reply to saying "This message cannot be answered from here."
Reading is the step immediately before replying. Taking the reply away as a
side effect of reading is close to the worst thing this screen could do.

The shade is the system's surface and the source app's, not the launcher's.
The inbox keeps its own record and does not need to edit somebody else's.
Messaging apps clear their own notifications when a reply goes through.
