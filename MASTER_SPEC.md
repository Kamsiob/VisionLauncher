# MASTER_SPEC

Living document. Update it in the same commit as any change that makes it wrong.

Name: VisionLauncher. Application ID: `io.github.kamsiob.launcher`. Both settled, see `DECISIONS.md` D20.

## 1. What this is

An Android home screen replacement for two overlapping audiences:

- Aging adults with the ordinary visual, motor, and cognitive changes of later life.
- People of any age with usable but impaired vision, many of whom actively reject anything framed as a senior product.

It is not a screen reader product and it does not compete with TalkBack. It is a visually premium launcher engineered around low vision and aging.

## 2. Non-negotiables

- Zero data collection. No accounts, no logins, no analytics, no telemetry, no ads, no subscriptions, no hosted backend.
- Everything runs on the device. Network access only for features the user explicitly opts into, and only for those features.
- AGPLv3. Bundled content and models carry their own license and correct attribution.
- Monetization is an optional Buy Me a Coffee link only, labeled "Support this work", present in both Settings and About.
- Honest limits are a feature. Where the app cannot do something, it says so plainly in the interface.
- No em dashes in anything a person reads. American English spelling throughout, including code comments and commit messages.

## 3. Architecture decisions that are already settled

Do not revisit these without an explicit instruction. Reasoning is in `DECISIONS.md`.

- No AccessibilityService anywhere in the app. Google Play excludes launchers from the `isAccessibilityTool` exemption, so declaring one risks rejection and takedown.
- The app does not take the default SMS role. Taking it would drop users off RCS onto plain SMS and MMS, degrading family photos, group chats, and encryption.
- The app does not become the default dialer. Calling uses `ACTION_CALL` and `ACTION_DIAL` behind a custom front end.
- Messaging is read through `NotificationListenerService` and replies are sent by firing the notification's own `RemoteInput` action, which preserves RCS and each app's native delivery.
- Messages the listener sees are persisted locally so the inbox has real scrollback.
- No QR or cloud configuration transfer in v1. Caregiver transfer is a printable sheet and a local settings file only.
- No bundled keyboard in v1. The reply screen answers the keyboard problem with on-device voice dictation, preset phrases, and the system keyboard as a fallback.

## 4. Platform targets

- Kotlin, Jetpack Compose, Material 3 as a base only. The visual identity is custom and defined in `DESIGN.md`.
- `minSdk 29`. `targetSdk 36`, or the highest stable level Play currently requires. Verify Play's requirement at submission time rather than trusting this number.
- Single module to start. Split only if it becomes necessary.
- Test devices: Pixel 10 Pro XL as daily driver, Pixel 8 as the lower memory device. Emulator only for destructive tests.

## 5. Screens

Numbers match the frames in `design/design-grid-v4.html`. The grid is the measurement authority.

### 5.1 Home (grid 01, 02, 03, 24)

Fixed layout. Nothing auto reorders, ever. No swiping between pages. No app drawer by default.

- Navy masthead: serif clock, day part line with the sun or moon mark, date line. The date line carries an optional weather glance, off by default, on device or opt in network.
- Day part thresholds: morning until 12:00, afternoon until 17:00, evening after. Sun mark for morning and afternoon, moon for evening and night. The mark is drawn over the same horizon line in both states.
- Status pill: green tinted, speaks a full sentence. "All is well." when nothing is wrong.
- Attention lamp replaces the status pill when anything is wrong. See 5.2.
- Six tiles, two columns, 128dp tall. Default set: Call, Messages, Magnifier, Camera, Photos, and one open slot. Built in features draw navy line icons. Third party apps use their real icons from `LauncherApps`.
- "More apps" key at the bottom opens the full alphabetical list with a search field. Search is present for the helper's benefit and is not required to use the list.

### 5.2 The attention queue (grid 02)

One lamp banner, many items, each a plain sentence, each with a repair key where a one tap fix genuinely exists. When more than one item is present, show the count: "2 things need attention".

Watched states, all readable without privileged permissions:

| State | How | Repair |
|---|---|---|
| Ringer silent or vibrate only | `AudioManager.getRingerMode()` | One tap where permitted, otherwise handoff to sound settings |
| Do Not Disturb on | `NotificationManager.getCurrentInterruptionFilter()` | Needs `ACCESS_NOTIFICATION_POLICY` to toggle, otherwise handoff |
| Battery low | `BatteryManager` or the battery broadcast | None, informational |
| Airplane mode on | `Settings.Global.AIRPLANE_MODE_ON` | Handoff only, apps cannot toggle this |
| No network, no Wi-Fi, no mobile data | `ConnectivityManager.NetworkCallback` | Handoff |
| Storage nearly full | `StorageManager` | Handoff |
| Notification access revoked | Check the enabled listeners setting | Handoff to notification access settings |
| Battery optimization re-enabled for this app | `PowerManager.isIgnoringBatteryOptimizations()` | Handoff, see 5.11 |

The last two matter most. They are how the app defends its own messaging pipeline and tells the user why messages stopped instead of failing silently.

Do not use a table like the one above in any user facing text. It is here for the build only.

### 5.3 Call (grid 04)

- Photo favorites at the top, each with the relationship underneath the name. Favorites are chosen in Helper settings.
- All contacts, alphabetical, large rows.
- Dial a number opens the keypad.
- Emergency key at the bottom, the only red in the app. It opens the Emergency screen. It never dials directly, so a stray tap cannot place a call.

### 5.4 Emergency (grid 05)

- Call 911: hands off to the system emergency flow. Never reimplement emergency calling.
- Alert Sarah, or whoever the helper set: places a call to that contact and sends a text with the current location.
- Honesty line stays on the screen: both need a cell signal, and this is not a monitored medical alert service.
- Do not build or claim fall detection. There is no third party API for it.

### 5.5 Keypad (grid 06)

96dp keys. Erase and Clear are words, not glyphs. Large readable dialed number above.

### 5.6 Messages (grid 07, 08, 09)

The most fragile part of the app. Build it defensively.

- Read messages via `NotificationListenerService`. Parse sender, time, source app label, and body.
- Persist every message seen into a local Room database. The inbox reads from the database, not from the live shade, so scrollback works and dismissed notifications do not vanish.
- Never transmit stored messages anywhere. This is a hard rule.
- Show the source app in words on every row: "9:12 AM, WhatsApp, sent a photo".
- Reply screen: "Speak your reply" as the primary path, six one tap preset phrases as the zero effort path, "Type instead" opening the system keyboard as the fallback.
- Spoken text is shown large for confirmation before sending. Never send without confirmation.
- Send by firing the notification's `RemoteInput` action. If no reply action exists on that notification, hide the reply options and offer to open the source app instead.
- Expect and handle: dismissed notifications, grouped and bundled notifications, reboots, listener kills, and Android 15 and later sensitive content redaction. When content is redacted, show the sender and time and say the content is hidden, then offer to open the source app. Never show an empty row with no explanation.
- "Open a message app" is always present as the reliable escape hatch.

### 5.7 Magnifier and Reader (grid 10, 11)

Dark interface, since it is used against bright physical objects.

- CameraX live preview. Zoom minus and plus as large keys, not a slider.
- Light toggles the torch.
- Hold still captures a still frame so a shaking hand can rest. Zoom and pan work on the frozen frame.
- Reader runs OCR on the frozen frame, shows the recognized text large, and reads it aloud.
- Contrast filters on the preview and the frozen frame: normal, grayscale, high contrast, inverted, yellow on black.
- Privacy sentence stays on the reader screen.

### 5.8 Photos (grid 12)

Reads the device media store. One photo at a time, edge to edge. Back and Next as large keys. Caption answers who sent it and when where that information exists. No albums, no editing, no pinch.

### 5.9 Today (grid 13)

User or helper entered medication and appointment cards. One tap marks done. Green appears only on completion. Keep the line "Written by you or your helper. Not medical advice." This is both honest and the correct Play policy posture. Do not add medical content, dosing advice, or drug information.

### 5.10 Settings and Helper settings (grid 14, 15)

Simple tier, one screen:

- Text size, three steps, on top of the system font scale.
- Look: Light, Dark, Outlined. Outlined replaces shadows with 3dp ink borders and can combine with dark.
- Choose your apps.
- Put my screen back.
- See and hear better.
- Helper settings, optionally behind a PIN.

See and hear better (grid 15) is a guided tour of Android's own accessibility stack, each row using the threshold pattern:

- Make everything bigger: display size and font size settings.
- Magnify the screen: `Settings.ACTION_ACCESSIBILITY_SETTINGS`, magnification.
- Hearing aids: Bluetooth and hearing device settings. Android 15 and later support hearing aids over ASHA and LE Audio.
- Written captions: Live Caption.
- Louder, clearer sound: Sound Amplifier if installed, otherwise the Play listing.
- Your medical info: the system Emergency Information or Medical ID screen.

Helper settings holds: favorites and relationships, the six reply phrases, the PIN, restore all threshold warnings, print a setup sheet, save my setup as a file, load a setup from a file, and About with the support link.

### 5.11 Onboarding (grid 22, 23)

Order matters.

1. "You can't break anything." Two paths: setting up my own phone, or helping someone set up theirs.
2. Set as home screen, using `RoleManager` with `ROLE_HOME` and `createRequestRoleIntent`. Fall back to `Settings.ACTION_HOME_SETTINGS`.
3. Permissions, one at a time, each with a plain reason: contacts, phone, camera, notification access.
4. Keep messages arriving: the battery optimization step. Explain that Android puts apps to sleep and that this stops messages appearing. Request the exemption with `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Allow skipping, and say plainly what will happen if skipped.
5. Helper path only: favorites, reply phrases, PIN, printable sheet.

The battery step decides whether messaging still works in month six on a Samsung or Xiaomi. Treat it as a first class requirement, and re-check the state later so the lamp can raise it if the exemption is lost.

### 5.12 Choose your apps (grid 16, 17, 18, 19, 20)

Arranging happens on the real home grid, never on an abstract list.

- A prompt bar sits at the top with a permanent Done chip, so the exit is always one visible tap.
- Tap an app: it lifts with a navy ring, everything else dims, an action sheet offers Move it, Put it first, Take it off the home screen, Never mind.
- Move it: tap a destination tile and the two trade places with one slow animation. Tapping the lifted app again cancels.
- Put it first jumps an app to the top spot in one tap and also solves any off screen destination.
- Taking an app off shows a lamp colored undo strip: "WhatsApp was taken off. Put it back." Nothing is ever uninstalled. Language is always placement, never deletion.
- Call cannot be moved or removed. Attempting it says "Call always stays first."
- Pressing Home mid arrange keeps completed changes and says so.
- Finishing shows a miniature preview with Keep it or Put it back the way it was. Every Keep snapshots the layout.
- Settings carries a standing "Put my screen back" that restores the last kept layout.
- Add an app is one flat alphabetical list with real icons, opened from inside the arranging session so the new tile joins the working layout and is confirmed by the same Keep. New apps take the first empty spot, or land at the bottom when the grid is full, and the screen says so.

### 5.13 Threshold (grid 21)

Shown before every handoff to a system screen. Says what the next screen is and that it will look different and smaller. The return line is conditional: it promises the Home button only while the app holds `ROLE_HOME`, because otherwise Home lands on whichever launcher does. Per destination dismissible with "Don't warn me about Wi-Fi again". Helper settings can restore all warnings.

### 5.14 Alarms

A big face alarm ships as a built in feature and appears in the Add an app list. Large digits, few options, no snooze maze. The time is set with keys, not a wheel, because a wheel is a drag and nothing in this app requires dragging. Alarms are scheduled with `setAlarmClock` so they survive a killed process, appear in the system's next alarm indicator, and are rescheduled after a reboot.

Settings is reached from the top of the More apps list, not from a home tile, because the six tiles belong to daily use and the grid draws no Settings tile. See `DECISIONS.md` D25.

## 6. Cross cutting build requirements

These are not visible in the grid and are easy to skip. They are not optional.

- **TalkBack.** Every screen fully labeled with correct content descriptions, correct focus order, and sensible traversal. Group cards with `mergeDescendants` where a card is one idea. Test every screen with TalkBack on. This protects users whose vision continues to decline, and a launcher that breaks TalkBack is a launcher that must be uninstalled at the worst moment.
- **Text scaling.** All text in `sp`, all line heights in `sp`, no fixed height text containers. Test every screen at 200 percent system font scale. Nothing may clip, overlap, or become unreachable.
- **Touch.** Tiles 128dp, key floor 88dp, gaps 12 to 14dp minimum. No swipe, drag, long press, double tap, or pinch is required anywhere. Increase touch slop so a small drag still registers as a tap. Debounce repeat taps within a short window so tremor does not fire an action twice. Respect the system touch and hold delay setting.
- **Haptics.** A distinct confirmation haptic on every committed action, because hearing loss travels with vision loss and audio alone is not enough.
- **Motion.** Minimal and functional. Respect the system remove animations setting. The only deliberate animation is the slow tile trade during arranging.
- **Localization.** English, Spanish, Chinese, Arabic. Arabic needs RTL mirroring from the start. Noto fallbacks for glyph coverage. Young Serif is Latin only and is used for clock digits only, so it is safe. Never build sentences by string concatenation; use proper plurals and formatted strings so the status and lamp sentences translate correctly.
- **Color correction and inversion.** Test under Android color inversion and every color correction mode. Semantic colors must survive or be backed by an icon and words. Color never carries meaning alone.
- **Undo.** Anything destructive is reversible and says so.

## 7. Dependencies, all license checked

Only these. Anything else needs a license check against AGPLv3 and commercial use before it enters the project.

- Jetpack Compose, CameraX, Room, WorkManager: Apache 2.0. Fine.
- Atkinson Hyperlegible Next and Atkinson Hyperlegible Mono: SIL OFL 1.1. Bundle as font files in `res/font`. Fine.
- Young Serif: SIL OFL 1.1. Bundle as a font file. Clock digits only.
- sherpa-onnx for offline text to speech: Apache 2.0. Fine. Verify the license of each specific voice model before shipping it, and rule out any voice whose model or training data license is unclear or non commercial.
- Tesseract via a maintained Android wrapper for OCR: Apache 2.0. Preferred OCR engine.
- ML Kit Text Recognition v2: free but a proprietary Google binary. Fallback only if Tesseract accuracy proves inadequate on real medication labels, and if used, disclose the dependency in the README.
- On device speech recognition: `SpeechRecognizer.createOnDeviceSpeechRecognizer()` on API 33 and above. Below 33, hide the voice key and show phrases and Type instead. Do not fall back to network speech recognition, ever.

Never use the Downloadable Fonts API. It requires network.

## 8. Build order

Ship nothing until a stage is genuinely finished and verified on a real device.

**Stage 1, the spine.** Home screen with masthead, day part mark, and fixed tiles. `ROLE_HOME` registration. `LauncherApps` app listing. More apps with search. Call with favorites, contacts, keypad. Emergency. Alarms. Settings simple tier with all three themes. Choose your apps, complete. Threshold pattern. Onboarding including the battery step. The full attention queue. TalkBack labeling and 200 percent scale testing as you go, not afterward.

**Stage 2, messaging.** Notification listener, local persistence, unified inbox, reading a message, reply by voice and phrases, redaction and missing reply action handling, the open the app escape hatch.

**Stage 3, seeing.** Magnifier with CameraX, freeze frame, torch, contrast filters. Reader with Tesseract and sherpa-onnx. Photos. See and hear better.

**Stage 4, the helper and the finish.** Helper settings, phrase editor, PIN, printable setup sheet, save and load setup file, Today, About with the support link, localization including Arabic RTL, full acceptance testing.

## 9. Known risks

- The notification pipeline is the fragile part. OEM battery optimizers kill listeners. Android 15 and later redact sensitive notification content and the exempting permission is not available to a launcher. Design every message path to degrade visibly rather than silently.
- CameraX preview filtering, freeze frame zoom, and focus behavior vary across devices and are the most likely place to need real device iteration.
- Tesseract accuracy on curved or low contrast labels is the main technical unknown in Stage 3. Test on real medication bottles early.
- Soft shadows carry less edge contrast than borders. The Outlined theme exists because of this. If testing with low vision users shows the default shadows are insufficient, make Outlined the default rather than weakening it.
