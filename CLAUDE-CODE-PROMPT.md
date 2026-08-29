# Prompts

Copy the block under MASTER PROMPT and paste it into Claude Code with this folder open. Nothing else needs pasting; the prompt tells Claude Code what to read.

The later sections hold prompts for follow up sessions.

---

## MASTER PROMPT

Copy everything between the lines.

---

You are building an Android launcher from a complete specification package that is already in this folder. Read `HANDOFF.md` in full first. Then read `MASTER_SPEC.md` and `DESIGN.md` in full, since this is the first session and you need the whole picture. Search `DECISIONS.md` and `RESEARCH.md` for specific sections as questions come up rather than loading them whole.

Open `design/design-grid-v4.html` and read it as a file. It contains 24 screens at 1px equals 1dp on 412dp wide frames, so every size, spacing value, radius, and color can be read directly from the CSS. That file is the measurement authority for this entire project. Where prose in `DESIGN.md` disagrees with the grid, the grid wins and you correct the prose.

Before writing any code, ask me two questions and wait for the answers: the app name, and the application ID. The current placeholders are `Launcher by Kamsiob` and `io.github.kamsiob.launcher`. The application ID is permanent once published so it has to be right.

Once I have answered, work in this order without stopping to check in between items.

**First, the repository.** Initialize git. Create a public GitHub repository under github.com/kamsiob, matching the settings, topics, description style, and structure of my other repositories. Add the full AGPLv3 license text. Write a README that is a living document, not marketing: what the app is, who it is for, what it deliberately cannot do, the license, and the "Support this work" link to buymeacoffee.com/kamsiob. Commit every file in this folder including all of the specification documents and the design grid, because they are part of the project. Delete `START-HERE.md` as part of that first commit; it was setup instructions for me and has served its purpose. Set up signed commits using SSH signing with vigilant mode, from this point forward only, never rewriting history. Create a project board and open issues as specifications with acceptance criteria stated in checkable terms.

**Second, the foundation.** Set up the Android project in Kotlin with Jetpack Compose. minSdk 29. targetSdk 36, or whatever Play currently requires; check rather than trusting that number. Bundle Atkinson Hyperlegible Next, Atkinson Hyperlegible Mono, and Young Serif as font files in `res/font`. Never use the Downloadable Fonts API, it requires network. Build the design token layer directly from the grid: colors, type scale, spacing, radii, the three themes including Outlined, and the shared components, which are the appliance key, the tile, the masthead, the status pill, the attention lamp, the row key, and the threshold screen.

**Third, Stage 1.** Build it in the order given in `MASTER_SPEC.md` section 8. Do not move to Stage 2 in this session; Stage 1 alone is substantial.

While building, these are not optional and are not a cleanup pass afterward. Label every element for TalkBack with correct content descriptions and focus order as you build each screen. Put all text in `sp` including line heights, and check each screen at 200 percent font scale as you finish it. Keep every touch target at or above the floor in `DESIGN.md`. Enlarge touch slop and debounce repeat taps. Respect the system remove animations and touch and hold delay settings. Give every committed action a distinct confirmation haptic.

Some standing rules for how I work. Pick the semver version number yourself and state the number and your reasoning in one line; I do not track versions. Keep exactly one copy of this app on my machine and delete old builds and test versions proactively; never install a second or parallel copy for any reason. Run destructive or data affecting tests on an emulator, not on my real device, and ask first if that is genuinely impossible. Do not end a turn while work remains; finishing one item means immediately starting the next in the same turn. Check the real system clock with a date command after each item rather than estimating elapsed time. If you run out of build work before we run out of time, spend the remainder on user acceptance testing: real user journeys, all three themes, largest fonts, screen reader, offline and online, fresh and upgraded installs, and then try to break it.

Keep the documents alive. With every commit, update `MASTER_SPEC.md`, `DESIGN.md`, and `HANDOFF.md` so they describe the app as it currently is. Correct superseded instructions rather than leaving them next to their replacements. Record real decisions in `DECISIONS.md` as they happen. Check whether any change made something in the README wrong and fix it in the same commit; the capabilities and limitations section is the part most likely to quietly become a lie. Once screens are visually working, capture real screenshots from the running app on the device, and recapture affected screenshots in the same pass as any material screen change.

Two writing rules that apply to everything, including code comments, commit messages, documentation, and all user facing text. No em dashes anywhere. American English spelling throughout: behavior, color, verify, analyze, summarize, prioritize, optimize, judgment, license, defense, gray, catalog, canceled, toward, recognize.

Close this session by updating `HANDOFF.md` to reflect exactly where things stand, committing and pushing everything to GitHub, deleting any previously exported build on my desktop, and exporting a fresh copy.

---

## Follow up session prompt

Use this at the start of any later session.

---

Read `HANDOFF.md` in full to see where we left off. Search `MASTER_SPEC.md` and `DESIGN.md` for the sections relevant to today's work rather than reading them whole. Treat `design/design-grid-v4.html` as the measurement authority for anything visual.

Continue from the next actions listed in `HANDOFF.md`. Same standing rules as always: pick the version number yourself and state it in one line, one copy of the app on the machine only, destructive tests on an emulator, do not end a turn while work remains, no em dashes, American English.

Close by updating `HANDOFF.md`, updating any spec or design document that this session made wrong, committing and pushing to GitHub, deleting the old exported build on my desktop, and exporting a fresh copy.

---

## Useful mid session prompts

**When a screen looks wrong:**
Compare this screen against its frame in `design/design-grid-v4.html`. The grid is the authority. List every measurement that differs, then fix them.

**Before calling any stage done:**
Run an acceptance pass on everything built so far. Real user journeys end to end. All three themes including Outlined combined with dark. 200 percent font scale on every screen. TalkBack on every screen, checking labels and focus order. Airplane mode and no signal. Fresh install and upgrade install. Then deliberately try to break it and report what broke.

**When something cannot be built as specified:**
Tell me plainly what the platform will not allow, what the closest achievable behavior is, and what the honest in-app wording should say about the limit. Do not quietly substitute something that looks similar but behaves differently.

**When you want to add a dependency:**
State the license, whether it works fully offline, whether commercial use is permitted, and whether it is compatible with AGPLv3, before adding it. If it fails any of those, do not add it and say so.
