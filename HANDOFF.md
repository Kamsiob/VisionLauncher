# HANDOFF

Read this file fully at the start of every session. Do not read the other documents in full; search them for the sections you need.

Update and commit this file at every commit, before any pause, when context runs low, when anything fails, and whenever a decision is made.

---

## Current state

**Status:** First session in progress. Stage 1 builds, installs, and runs on the Pixel 8. Verified on device: first run, the home screen with masthead and tiles, and the attention queue raising two real items with working repair keys.

**Stage:** Stage 1, most screens written, device verification underway. Foundation done: Gradle project (AGP 9.3.1, Kotlin 2.4.10, Gradle 9.6.1, minSdk 29, targetSdk 36 verified against Play's August 31, 2026 requirement, compileSdk 37), three fonts bundled in `res/font` as variable TTFs, the full token layer read from the grid, the shared components (appliance key, tile, row key, masthead, status pill, attention lamp, undo strip, prompt bar, threshold screen), the attention watcher with all Stage 1 states, LauncherApps repository, Home screen with masthead and day part mark, More apps with search, the honest not-built interstitial (D23), and the em dash build gate.

**App name:** VisionLauncher. Application ID `io.github.kamsiob.launcher`. Both settled by the user on August 29, 2026. See `DECISIONS.md` D20.

**Repository:** Being created this session: public GitHub repo at github.com/kamsiob/VisionLauncher, matching the settings, topics, and structure of the other repos in the portfolio.

**Version:** None yet. Claude Code picks the semver number itself and states the number and the reasoning in one line. The user does not track versions.

---

## What exists in this folder

- `MASTER_SPEC.md` what the app does, screen by screen, plus architecture and build order.
- `DESIGN.md` how it looks and behaves.
- `DECISIONS.md` what was settled and what was rejected, with reasons.
- `RESEARCH.md` the evidence base.
- `design/design-grid-v4.html` 24 screens. This is the measurement authority over any prose.
- `CLAUDE-CODE-PROMPT.md` the prompts the user pastes in. Not a build input.
- `START-HERE.md` setup instructions for the user. Delete this in the first commit.

---

## Next actions

1. Ask the user for the app name and the application ID, then apply them everywhere.
2. Init git. Create the public GitHub repo. Commit all of these documents, including the design grid, in the first commit.
3. Set up the Android project: Kotlin, Compose, minSdk 29, targetSdk 36 or the current Play requirement.
4. Bundle the three fonts into `res/font` and build the design token layer from `DESIGN.md` and the grid.
5. Begin Stage 1 in the order given in `MASTER_SPEC.md` section 8.

---

## Open questions for the user

- The app name and application ID.
- Whether the printable setup sheet and the settings file export stay in v1 or move to v1.1. Both are currently in Stage 4.

---

## Decisions log for this session

Nothing yet. Record decisions here as they are made, then move the durable ones into `DECISIONS.md`.

---

## Known risks being carried

- The notification listener pipeline is the fragile part of the app. OEM battery optimizers kill listeners, and Android 15 and later redact sensitive notification content with no exemption available to a launcher. Every message path must degrade visibly, never silently.
- CameraX preview filtering, focus, and freeze frame behavior vary by device and will need real device iteration.
- Tesseract OCR accuracy on real medication labels is unproven. Test early with actual bottles, not screenshots.
- Soft shadow edge contrast may be insufficient for the low vision audience. The Outlined theme is the mitigation. If testing shows the default is inadequate, make Outlined the default rather than weakening it.

---

## Verification state

- Real device verification: none.
- TalkBack pass: none.
- 200 percent font scale pass: none.
- Arabic RTL pass: none.
- Screenshots captured: none.
