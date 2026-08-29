# HANDOFF

Read this file fully at the start of every session. Do not read the other documents in full; search them for the sections you need.

Update and commit this file at every commit, before any pause, when context runs low, when anything fails, and whenever a decision is made.

---

## Current state

**Status:** Stage 1 is built, running, and verified on a real Pixel 8 running API 37. Every Stage 1 screen from `MASTER_SPEC.md` section 8 exists and was exercised on the device, not only compiled.

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

**The app is not the home screen on the Pixel 8.** The role request was tested and then cancelled on purpose, because Messages, Magnifier, and Photos belong to later stages and making an incomplete launcher the daily home screen would be disruptive. Set it deliberately when Stage 2 and Stage 3 land.

---

## What is not built

Stage 2 messaging, Stage 3 magnifier, reader, and photos, and Stage 4 Today, the printable sheet, the settings file, the PIN, the reply phrase editor, and localization. The Messages, Magnifier, and Photos tiles are present on the home screen and open a screen that says plainly that the part is not built yet, offering the phone's own app where one exists. See `DECISIONS.md` D23. Those interstitials delete themselves stage by stage.

---

## Next actions

1. Start Stage 2, the messaging pipeline, from issue #14. Build it defensively: the notification listener, Room persistence, the unified inbox, reading a message, reply by voice and phrases, redaction handling, the missing reply action case, and the open the app escape hatch.
2. Delete the not built interstitial for Messages in the same commit that ships the inbox.
3. Localization has not started. Every user facing string is already in `strings.xml` with plurals and formatted arguments, so no sentence is built by concatenation. Arabic RTL has never been tested.

---

## Open questions for the user

- Whether the printable setup sheet and the settings file export stay in v1 or move to v1.1. Both are currently in Stage 4.
- When to actually set VisionLauncher as the home screen on a real device. It is a Stage 2 or Stage 3 decision, not a Stage 1 one.

---

## Decisions made this session

D20 superseded with the settled name and application ID. D21 signed commits from the first commit with no history rewriting. D22 one application ID with no debug suffix, so exactly one copy exists per device. D23 unbuilt tiles are honest rather than hidden. D24 the tile grid drops to one column at large font scales. D25 Settings is reached from More apps. D26 every key carries its own TalkBack label. D27 adding an app happens inside the arranging session. D28 the threshold only promises the Home return when it holds the home role.

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
- Arabic RTL pass: none. No translations exist yet.
- Color inversion: checked against the requirement rather than the pixels, because `screencap` captures before the compositor inverts. Nothing carries meaning by color alone, so inversion cannot destroy meaning. A human eye on an inverted screen would still be worth having.
- Color correction modes: none.
- Screenshots captured: yes, in `docs/screenshots`, all from the running app on the device.
- Unit tests: 15, covering the home layout operations and the next alarm arithmetic, run against the code the screens actually call.
- Build gates: the em dash gate and the no INTERNET permission gate, both proven to fail when violated.

---

## Where the build is

`~/Desktop/visionlauncher-0.1.0-stage1.apk`, exported at the end of this session and confirmed to install. Exactly one copy of the app exists on the Pixel 8. There is no debug application ID suffix, by D22, so a debug and a release build collide deliberately rather than coexisting.
