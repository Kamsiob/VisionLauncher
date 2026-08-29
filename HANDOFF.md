# HANDOFF

Read this file fully at the start of every session. Do not read the other documents in full; search them for the sections you need.

Update and commit this file at every commit, before any pause, when context runs low, when anything fails, and whenever a decision is made.

---

## Current state

**Status:** Stage 1 is built, running, and verified on a real Pixel 8 running API 37. Every Stage 1 screen from `MASTER_SPEC.md` section 8 exists and was exercised on the device, not only compiled.

Two review passes followed. The first found five defects where the interface asserted something the code did not guarantee, and enlarged every icon at the user's request. The second was an eight lens sweep with every finding adversarially verified: 46 confirmed, of which the serious ones are fixed. The most consequential were an emergency text that was silently dead on Android 10 and 11, an arranging session that discarded your work on Back or Home while the spec promised it would not, a keypad that could not dial a repeated digit, and Settings controls at half the app's own touch floor.

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

## What is not built

Stage 2 messaging, Stage 3 magnifier, reader, and photos, and Stage 4 Today, the printable sheet, the settings file, the PIN, the reply phrase editor, and localization. The Messages, Magnifier, and Photos tiles are present on the home screen and open a screen that says plainly that the part is not built yet, offering the phone's own app where one exists. See `DECISIONS.md` D23. Those interstitials delete themselves stage by stage.

---

## What the second review pass changed

Roughly fifteen commits, each verified on the device. The themes, in the order they matter:

**Sentences that were not true.** The emergency alert promised a text on Android versions where the call to obtain the SMS manager returns null, so it threw and was swallowed. "Turn Do Not Disturb off" could never turn it off, because the toggle needs an access the app never requests. The battery onboarding step told people to find a control the next screen does not have. Arranging promised to keep your work and threw it away. Each is now either true or differently worded.

**Things that silently did nothing.** Four screens ended in blank space when their data was absent. The Messages and Photos escape hatches swallowed a failure to resolve. A tile whose app was uninstalled vanished from the layout with nothing said. Two unguarded intent launches could have crashed the launcher itself.

**Touch and feedback.** One debounce window served the whole app, so dialing 555 lost a press and the swallowed press gave no haptic at all. Four haptics confirmed before knowing the outcome. The Settings text size keys were half the app's own key floor.

**TalkBack past labels.** `liveRegion` appeared once in the entire app, so nothing that changed ever spoke. The move destination announcement had been written and never wired up. Outlined announced as a radio button while being a combinable toggle.

**Performance, measured rather than guessed.** Opening the app list dropped roughly eighteen frames because the enumeration ran during composition; the 95th percentile went from 150ms to 97ms. Cold start painted the light palette and could flash Onboarding at a returning user, since DataStore had not answered yet.

## Not done, and worth doing next

- The tile trade animation MASTER_SPEC 5.12 describes does not exist. Issue #19.
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
- Every device setting changed during testing was restored afterward: font scale, color inversion, TalkBack, airplane mode, and the ringer, which was found on vibrate and put back on vibrate.
- TalkBack pass: done on Home and the arranging screens, with the accessibility tree inspected on Call, the keypad, Settings, Helper settings, and Add an app. Not yet swept screen by screen with TalkBack actually speaking on every screen.
- 200 percent font scale pass: done on Home. Not yet swept across every screen.
- Arabic RTL pass: done on the device by setting the app's locale to Arabic, which mirrors the layout while leaving the untranslated strings in place. The layout mirrors correctly throughout. Two defects found and fixed, recorded in D39. Translations themselves are still Stage 4.
- Color inversion: checked against the requirement rather than the pixels, because `screencap` captures before the compositor inverts. Nothing carries meaning by color alone, so inversion cannot destroy meaning. A human eye on an inverted screen would still be worth having.
- Color correction modes: none.
- Screenshots captured: yes, in `docs/screenshots`, all from the running app on the device.
- Unit tests: 22, covering the home layout operations, the Call lock invariant, the restore semantics, and the next alarm arithmetic, run against the code the screens actually call.
- Build gates: the em dash gate and the no INTERNET permission gate, both proven to fail when violated.

---

## Where the build is

`~/Desktop/visionlauncher-0.1.0-stage1.apk`, rebuilt clean and re-exported after the icon work, and confirmed to install. Exactly one copy of the app exists on the Pixel 8, and the previous export was deleted rather than left beside it.

Every device setting borrowed for testing was put back: font scale, color inversion, TalkBack, airplane mode, the battery optimization allowance, and the ringer, which was found on vibrate and is on vibrate again. The Pixel 8's home app is still the Pixel Launcher, deliberately. There is no debug application ID suffix, by D22, so a debug and a release build collide deliberately rather than coexisting.
