# HANDOFF

Read this file fully at the start of every session. Do not read the other documents in full; search them for the sections you need.

Update and commit this file at every commit, before any pause, when context runs low, when anything fails, and whenever a decision is made.

---

## Current state

**Status:** Stage 1 is built, running, and verified on a real Pixel 8 running API 37. Stage 2, messaging, is built and verified end to end on an emulator, including a reply that a second app actually received. Every Stage 1 and Stage 2 screen from `MASTER_SPEC.md` section 8 exists and was exercised on a device, not only compiled.

The user has since asked for all four stages before the APK is delivered, which supersedes the original instruction to stop after Stage 1. Stages 3 and 4 are in progress.

Three review passes followed, each with every finding adversarially verified before anything was changed.

The first found five defects where the interface asserted something the code did not guarantee, and enlarged every icon at the user's request. The second was an eight lens sweep over dead ends, states, voice, TalkBack, lifecycle, launcher behavior, visuals and feedback: 46 confirmed. The third took six angles the others never looked at, RTL, configuration changes, hostile data, privacy, release readiness and what accumulates over months: 23 confirmed.

The most consequential across all three: an emergency text silently dead on Android 10 and 11; an arranging session that discarded your work on Back or Home while the spec promised it would not; a keypad that could not dial a repeated digit; Settings controls at half the app's own touch floor; an alarm playing on the ringtone stream so a phone on vibrate would ring in silence; a corrupt preferences file that would crash the launcher on every start with no way back in; a crash on any phone with a work profile; and cloud backup carrying the favorites and the emergency contact off the device while the README promised nothing leaves it.

**Stage:** Stage 1 complete. Stage 2, messaging, has not been started.

**Version:** 0.1.0. A first minor version rather than 0.0.1 because the app is a coherent working launcher rather than a fragment, and rather than 1.0.0 because 1.0.0 is reserved for the Play release after all four stages are verified.

**App name:** VisionLauncher. Application ID `io.github.kamsiob.launcher`. Both settled by the user on August 29, 2026. See `DECISIONS.md` D20.

**Repository:** https://github.com/Kamsiob/VisionLauncher, public, AGPL-3.0, with the portfolio's topics and merge settings. Project board at https://github.com/users/Kamsiob/projects/4 with 16 specification issues, each carrying acceptance criteria in checkable terms. Every commit is SSH signed.

**Toolchain:** AGP 9.3.1, Kotlin 2.4.10, Gradle 9.6.1, minSdk 29, targetSdk 36, compileSdk 37. targetSdk 36 was checked against Play's requirement for new apps from August 31, 2026, not assumed.

---

## What is built and verified on the device

Home with the navy masthead, the Young Serif clock, the day part mark, the date line, the status pill, the fixed tiles, and More apps. The attention queue with ringer, Do Not Disturb, battery, airplane mode, network, storage, and battery optimization, each a plain sentence with a repair key where a one tap fix exists. Call with favorites, contacts, and the red Emergency key that opens a screen rather than dialing. The 96dp keypad. Emergency with the honesty line. Alarms. Settings with three text steps and three themes. See and hear better. Helper settings with favorites and the emergency person. Choose your apps, complete, including the action sheet, the trade, Put it first, taking an app off with the lamp undo, Add an app, and the Keep it preview. The threshold pattern before every handoff. Onboarding on both paths including the battery step.

Specifically verified on the device, with evidence:

- The one tap ringer repair moves the phone from vibrate to normal with no handoff. Confirmed by reading `mode_ringer` before and after.
- Turning airplane mode on raised the queue from 2 items to 3 live, with no restart, and correctly showed airplane mode instead of also saying no network.
- Saving an alarm registers an exact `RTC_WAKEUP` alarm clock with a show intent, so it reaches the system's next alarm indicator.
- Three rapid taps on one keypad key register one digit. The tremor debounce holds, and deliberate typing still works.
- TalkBack runs, focus lands on each tile as one unit, and the accessibility tree shows every clickable node carrying its own label.
- At 200 percent font scale nothing clips and the grid drops to one column so no label breaks mid word. The worst case, the largest text step on top of 200 percent, was tested too and holds.
- An in place upgrade preserves settings, and it was an upgrade rather than a fresh install that exposed the last word breaking bug.
- The one tap battery optimization repair opens the system request dialog.
- A fresh install after uninstall shows first run with no leftover state.
- The home role request opens the system dialog with the right name and icon. It was cancelled deliberately, see the note below.
- Revoking contacts while running does not crash; the Call screen says what it cannot do and keeps dialing and Emergency working.
- 36 rapid navigation inputs, process death mid navigation, and rotation all survived with no crash.
- The em dash gate was proven by planting an em dash, watching the build fail, and removing it.
- The merged manifest carries no INTERNET permission, and a build gate now fails if one ever appears.
- "Put my screen back" was announcing success while changing nothing. Reproduced, fixed, and the full round trip re-verified: keep a change, restore it, and the layout genuinely returns.
- The Emergency alert key promised a text it could not send, because SEND_SMS and location were checked but never requested. Both are now requested when the helper chooses the person, and the key's subtitle states what is actually possible. Verified by denying both and reading "Calls only, because sending a text was not allowed".
- "Call always stays first" was breakable by choosing Call as a move destination. Reproduced, fixed in HomeLayout so no screen can bypass it, and re-verified.
- Threshold dismissal is per destination and survives process death; "Bring back all warnings" makes a dismissed destination warn again.
- Every icon in the app was enlarged, twice, at the user's request, and the stroke weight was reduced in step so the shapes do not fuse.

**The app is not the home screen on the Pixel 8.** The role request was tested and then cancelled on purpose, because Messages, Magnifier, and Photos belong to later stages and making an incomplete launcher the daily home screen would be disruptive. Set it deliberately when Stage 2 and Stage 3 land.

---

## Stage 2, messaging, and how it was verified

Grid 07, 08 and 09 are built: the inbox, reading a message, and replying. A
second app was written for the emulator purely to post notifications, because
the pipeline cannot be tested against anything else. Verified there, with
evidence:

- A message posted by another app appears in the inbox with sender, time, source app in words, and the text.
- `MessagingStyle` notifications, which is what real messaging apps send, are read through the style rather than the flat compatibility fields. In a group chat the row says "Priya, in Book club" instead of naming the conversation as the person.
- A group summary notification is filtered, while both of its children store. Confirmed against `flags=GROUP_SUMMARY` in `dumpsys`.
- An ongoing notification and a non-message notification are both rejected.
- A redacted message stores with the sender and the time, and the reading screen says the phone is hiding the content and offers the source app.
- A phrase reply was received by the other app, confirmed by its own log: `GOT_REPLY[On my way]`.
- With the notification gone, the same key says "The reply did not go out. Open Chatter and send it there." rather than appearing to succeed.
- Messages outlive their notifications. Clearing the source app entirely left the inbox intact and correctly dropped only the reply action.

Two defects were found by driving the real UI, neither visible in the code:

- Opening a message dismissed its notification, which destroyed the reply action, so reading a message removed the ability to answer it. See `DECISIONS.md` D43.
- Sending a reply left the launcher showing a blank screen, because clearing the open message made two screens fire a back navigation as they were disposed and the two extra pops emptied the navigation graph.

## Stage 3, seeing, and how it was verified

Grid 10, 11 and 12: the magnifier, the reader, and photos. All three tiles now
open real screens, so the "not built yet" interstitial has been deleted, which
is what `DECISIONS.md` D23 said would happen stage by stage.

- The magnifier runs CameraX in a dark interface whatever theme is chosen, with zoom as two large keys rather than a slider, a torch key that says so when the phone has no light, and Hold still to freeze a frame. Verified running on the emulator with a screenshot of the live preview.
- The frozen frame is held in memory and never written to storage. A magnified pill bottle is not a photograph anybody wants keeping.
- Optical character recognition is Tesseract, not ML Kit. ML Kit reads better and was tried first, but it will not initialize without Google's telemetry uploader, which is the one thing that put an INTERNET permission in the merged manifest. See `DECISIONS.md` D44 for the crash that proved it.
- Recognition is proven by an instrumented test that runs on the device: it draws a medicine label to a bitmap, recognizes it through the shipping code path, and asserts the drug name, the dose and the instruction all come back. A blank frame recognizes nothing rather than returning empty text.
- Recognition runs off the main thread, because Tesseract on a full camera frame takes a second or two and doing that on the main thread would freeze the launcher.
- The five contrast filters are one full width key that names the current one and cycles, not a row of five. See `DECISIONS.md` D45.
- Photos reads the media store, excludes screenshots and sticker folders, and captions each picture with which one of how many, the folder it came from where that names something, and when. Verified with pushed images: "Photo 1 of 3, from WhatsApp Images, 2:41 PM" for one in a named folder and "Photo 2 of 3" with no claimed sender for one in Pictures, and the screenshot correctly absent.
- A photo is decoded at screen size rather than at full resolution, because three fifty megabyte bitmaps in a row would end the process.

Two defects found by looking at the running screen rather than the code: the
contrast filters scrolled sideways with the last one past the edge, and the
photo caption was announced twice by a screen reader because both the image and
the pill below it carried it.

**Not yet verified end to end:** recognition through the camera rather than
through a bitmap. The emulator's virtual scene ignores the poster flag that
would put readable text in front of the lens, so the camera to recognizer link
has been verified in its two halves rather than in one pass. Do this on the
Pixel by pointing it at a real label.

## Stage 4, the helper and the finish

Grid 13 and the rest of grid 15. Today, the reply phrase editor, the helper
code, the printable sheet, the setup file, and three translations.

- Today holds cards written by the person or their helper. Green appears only on completion, and a completion is stamped with the day it belongs to so a card marked done yesterday is not still green this morning.
- Today is also a home tile now, so it is reachable without going through helper settings.
- The setup file is readable JSON a helper can save and load, covering favorites, the emergency person, the phrases, the cards, the theme, the text size and the home layout. Verified by saving it, reading the file off the device, and checking every field.
- The printable sheet is a self contained HTML page with no external anything, so it prints the same from a phone with no network. Verified by generating one and reading it.
- The helper code is four digits on the same keypad as dialing. It is deliberately not a security feature and the screen says so.
- Translated to Spanish, simplified Chinese and Arabic. 335 strings each, every format specifier checked, and the whole app driven in Arabic and Spanish on the emulator.

Defects found in this stage, none of them visible in the code:

- The dial pad mirrored in Arabic, so it read 3 2 1. See `DECISIONS.md` D46.
- The setup file carried today's completions, so loading it on another phone would show a pill as already taken that nobody took.
- A Today card's spoken label ended with "tap to mark it done" on the target that opens the editor.
- Android lint had never been run on this project. It found a genuine crash: `InputStream.readNBytes` is API 33 and this app runs from 29, so loading a setup file would have died on an Android 10 phone. It also found that Photos would ignore Android 14's "select photos" partial grant and keep asking for access it already had.

---

## What the second review pass changed

Roughly fifteen commits, each verified on the device. The themes, in the order they matter:

**Sentences that were not true.** The emergency alert promised a text on Android versions where the call to obtain the SMS manager returns null, so it threw and was swallowed. "Turn Do Not Disturb off" could never turn it off, because the toggle needs an access the app never requests. The battery onboarding step told people to find a control the next screen does not have. Arranging promised to keep your work and threw it away. Each is now either true or differently worded.

**Things that silently did nothing.** Four screens ended in blank space when their data was absent. The Messages and Photos escape hatches swallowed a failure to resolve. A tile whose app was uninstalled vanished from the layout with nothing said. Two unguarded intent launches could have crashed the launcher itself.

**Touch and feedback.** One debounce window served the whole app, so dialing 555 lost a press and the swallowed press gave no haptic at all. Four haptics confirmed before knowing the outcome. The Settings text size keys were half the app's own key floor.

**TalkBack past labels.** `liveRegion` appeared once in the entire app, so nothing that changed ever spoke. The move destination announcement had been written and never wired up. Outlined announced as a radio button while being a combinable toggle.

**Performance, measured rather than guessed.** Opening the app list dropped roughly eighteen frames because the enumeration ran during composition; the 95th percentile went from 150ms to 97ms. Cold start painted the light palette and could flash Onboarding at a returning user, since DataStore had not answered yet.

## Not done, and worth doing next

- The alarm stops ringing when it leaves the foreground, which is the lesser of two failures rather than the right answer. Issue #20.
- `AttentionWatcher` does disk and binder work on the main thread on every resume.
- Nineteen strings are unreachable, several of them fossils of features that were wired differently in the end.
- Icons do not scale with the user's text step, deferred with reasons in issue #18.

## Next actions

1. Icons no longer scale with the user's text step; they are fixed dp. Doing it safely needs the app icon bitmap cache keyed by size, a cap so a scaled app icon does not overrun the row key, and the reflow threshold recomputed. Tracked in issue #17's follow up. This is small and worth doing before Stage 2 grows the surface.
2. Start Stage 2, the messaging pipeline, from issue #14. Build it defensively: the notification listener, Room persistence, the unified inbox, reading a message, reply by voice and phrases, redaction handling, the missing reply action case, and the open the app escape hatch.
3. Delete the not built interstitial for Messages in the same commit that ships the inbox.
4. Localization has not started. Every user facing string is already in `strings.xml` with plurals and formatted arguments, so no sentence is built by concatenation. Arabic RTL has never been tested.

---

## Open questions for the user

- Whether the printable setup sheet and the settings file export stay in v1 or move to v1.1. Both are currently in Stage 4.
- When to actually set VisionLauncher as the home screen on a real device. It is a Stage 2 or Stage 3 decision, not a Stage 1 one.

## One thing that needs the user's own hand

**Vigilant mode is not enabled and cannot be set from here.** It is an account level setting with no API, at https://github.com/settings/keys, under "Flag unsigned commits as unverified". Every commit in this repository is SSH signed and GitHub reports all of them as verified, so turning vigilant mode on costs nothing here and makes any future unsigned commit visibly suspect across the whole account. Worth doing once.

---

## Decisions made this session

D20 superseded with the settled name and application ID. D21 signed commits from the first commit with no history rewriting. D22 one application ID with no debug suffix, so exactly one copy exists per device. D23 unbuilt tiles are honest rather than hidden. D24 the tile grid drops to one column at large font scales. D25 Settings is reached from More apps. D26 every key carries its own TalkBack label. D27 adding an app happens inside the arranging session. D28 the threshold only promises the Home return when it holds the home role. D29 the zero network promise is a build gate rather than a sentence. D30 the restore point is the layout before the change, so "Put my screen back" stops confirming an undo it never performed. D31 the Emergency key states what this phone can actually do, and the permissions behind the promise are requested when the promise is made. D32 the Call lock is enforced on both sides of a trade. D33 the 911 key fills the dialer and says so. D34 the icons were too small and the grid was corrected rather than followed. D35 arranging never loses work whichever way the session ends. D36 the first frame knows the real settings. D37 the status bar matches what is behind it. D38 prose about numbers goes stale one change later.

All are recorded in full in `DECISIONS.md`.

---

## Known risks being carried

- The notification listener pipeline is the fragile part of the app, and it is entirely unbuilt. OEM battery optimizers kill listeners, and Android 15 and later redact sensitive notification content with no exemption available to a launcher. Every message path must degrade visibly, never silently.
- CameraX preview filtering, focus, and freeze frame behavior vary by device and will need real device iteration.
- Tesseract OCR accuracy on real medication labels is unproven. Test early with actual bottles, not screenshots.
- Soft shadow edge contrast may be insufficient for the low vision audience. The Outlined theme is the mitigation and it works well on the device. If testing shows the default is inadequate, make Outlined the default rather than weakening it.
- The one column fallback at large font scales is a judgment call made without user testing. If a real user finds the single column worse than a smaller two column grid, D24 is the entry to revisit.

---

## Verification state

- Real device verification: Pixel 8, API 37. Every Stage 1 screen opened and exercised.
- Alarms verified end to end: scheduled through the app, fired on their own at the minute, rang on the alarm stream with the ringer on vibrate, stopped, and showed the undo strip when taken off.
- Every device setting changed during testing was restored afterward: font scale, color inversion, TalkBack, airplane mode, and the ringer, which was found on vibrate and put back on vibrate.
- TalkBack pass: done on Home and the arranging screens, with the accessibility tree inspected on Call, the keypad, Settings, Helper settings, and Add an app. Not yet swept screen by screen with TalkBack actually speaking on every screen.
- 200 percent font scale pass: done on Home. Not yet swept across every screen.
- Arabic RTL pass: done twice on the device by setting the app's locale to Arabic, which mirrors the layout while leaving the untranslated strings in place. The layout mirrors correctly throughout. Two defects found and fixed, recorded in D39. Translations themselves are still Stage 4.
- Color inversion: checked against the requirement rather than the pixels, because `screencap` captures before the compositor inverts. Nothing carries meaning by color alone, so inversion cannot destroy meaning. A human eye on an inverted screen would still be worth having.
- Color correction modes: none.
- Screenshots captured: yes, in `docs/screenshots`, all from the running app on the device.
- Unit tests: 22, covering the home layout operations, the Call lock invariant, the restore semantics, and the next alarm arithmetic, run against the code the screens actually call.
- Build gates: the em dash gate and the no INTERNET permission gate, both proven to fail when violated.

---

## Where the build is

`~/Desktop/VisionLauncher-0.1.0.apk`, a **signed release build**, 3.3MB, verified end to end on the device.

It is signed with a local test key, `visionlauncher-test.jks`, which is gitignored along with `keystore.properties`. That key is for sideloading only and must not become the Play upload key. A checkout without those two files still builds; the release variant simply comes out unsigned rather than failing.

Release was verified rather than assumed, because R8 can break things a debug build never shows. All three serialized stores survive minification: the home layout, the favorites, and the alarms each persisted across a force stop and restart. Cold start is about 148ms against 435ms for the debug build. Exactly one copy of the app exists on the Pixel 8, and the previous export was deleted rather than left beside it.

Every device setting borrowed for testing was put back: font scale, color inversion, TalkBack, airplane mode, the battery optimization allowance, and the ringer, which was found on vibrate and is on vibrate again. The Pixel 8's home app is still the Pixel Launcher, deliberately. There is no debug application ID suffix, by D22, so a debug and a release build collide deliberately rather than coexisting.
