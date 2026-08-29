# DESIGN

Living document. Update it in the same commit as any change that makes it wrong.

## The measurement authority

`design/design-grid-v4.html` is the authority for every measurement, spacing value, size, and layout in this app. Prose loses measurements. Where this document and the grid disagree, the grid wins, and this document gets corrected.

Open the grid in a browser. In that file 1px equals 1dp and the frames are 412dp wide, so values can be read directly off the CSS.

## What the design is trying to be

Premium, calm, and dignified. It serves an 85 year old with macular degeneration and a 45 year old with congenital low vision at the same time, and neither should feel categorized by it.

Two failure modes to avoid in equal measure. Clinical and institutional, which reads as a medical device and gets rejected on sight. And childish or oversimplified, which condescends. The research is direct about this: the design factors that make older adults perceive stigma are lack of aesthetic appeal, poor affordance, and accentuated social signifiers, and obvious instructions are named as one of those signifiers. Assistive devices get abandoned at high rates, and aesthetics is a documented reason.

## Color

- `paper` #F4EEE1, screen background. Warm and matte, never pure white, which cuts glare for cataract users while keeping positive polarity for the astigmatism majority.
- `card` #FDFAF2, key and card faces.
- `ink` #2A2723, text and borders. Warm near black, not pure black.
- `ink-soft` #5A544A, secondary text, still high contrast.
- `navy` #2E4A66, the masthead, primary key fills, screen titles, and all built in feature icons.
- `navy-deep` #243C54, dark theme masthead and primary key borders.
- `cream` #F6EFDF, text on navy.
- `lamp` #F4E4B9, the attention banner and the undo strip.
- `green` #35704E, reserved. Only "all is well" and "done".
- `red` #AF3B2B, reserved. Only the emergency surfaces.

Dark theme: `dark-bg` #1B1D20, `dark-card` #282B31, `dark-text` #F1EADC, `dark-navy` #22303F, `dark-accent` #9FBEDD.

Rules:

- Color never carries meaning alone. Every colored signal is backed by an icon and words.
- Lamp yellow is the app's only caution voice. There is no red for errors, warnings, or destructive actions, because red belongs to emergency alone. Taking an app off, a failed send, and any other recoverable problem all use the lamp treatment with an explicit undo.
- Third party apps keep their real icons. Built in features draw navy line icons. That difference quietly tells the user what belongs to this app and what belongs to the phone.
- Navy on paper computes to roughly 8:1, clearing AAA. Green and red on paper clear AA at all sizes. Verify with a checker before any compliance claim.

## Type

- **Young Serif**, SIL OFL. The clock and the six short headings on onboarding, the threshold, and the Keep it screen. It gives the product warmth and a face without touching legibility anywhere it matters. Latin only: outside Latin, including the Arabic-Indic numerals an Arabic locale gives the clock, it falls to the system serif and those screens keep their words while losing their face. See `DECISIONS.md` D16.
- **Atkinson Hyperlegible Next**, SIL OFL. Everything else. Screen titles use weight 800 in navy. Purpose built for low vision, with disambiguated character shapes.
- **Atkinson Hyperlegible Mono**, SIL OFL. Timestamps, the status pill, and metadata rows.

Scale, in sp: clock 94, screen titles 34, tile labels 28, key labels 25 to 26, body 24, row metadata 16 to 17, status pill 19.

Three user text size steps sit on top of the system font scale, never replacing it. Everything in sp including line heights. Bundle fonts as files in `res/font`; the Downloadable Fonts API is banned because it needs network.

An honest note on the font choice. There is no peer reviewed evidence that Atkinson Hyperlegible outperforms a standard sans for low vision reading speed. The evidence says print size, contrast, and letter spacing dominate and that font effects are small. Atkinson is a defensible, license clean choice with real character disambiguation merit, and it must never be marketed as clinically superior.

## Depth and separation

One separation device per element, never two. An element gets a shadow or a border, never both, and never a border plus a background plus a rule.

Three themes:

- **Light**, default. Keys are soft shadowed raised cards with no borders.
- **Dark**, by choice. Same layout, deeper navy masthead, shadows adapted.
- **Outlined**, by choice, combinable with dark. 3dp ink borders replace shadows entirely.

Outlined exists because soft shadows carry far less edge contrast than borders, and contrast sensitivity loss in glaucoma, AMD, and cataract is exactly what erases a soft shadow. It targets the 3:1 non text contrast threshold that a shadow generally cannot meet. Dark is never the default, because roughly a third of adults have astigmatism and get halation from light on dark, while dark genuinely helps cataract and photophobia users. It is offered as an obvious choice, not buried.

## Shape and space

Corner radius 20dp on keys and cards, 30dp on the phone frame, 16dp on the lamp and prompt bar, 999dp on the status pill. Screen padding 28dp top, 24dp sides, 30dp bottom. Gaps between keys 12 to 14dp.

The home grid is two columns, the Home and Back keys sit side by side, and so do the three Look cards in Settings. At or above a combined text scale of 1.3, counting the system font scale and the user's text step together, all three reflow to one column, because a 128dp tile cannot hold "Magnifier" and a third of the screen cannot hold "Outlined" at a readable size. Tile order never changes.

Where the layout cannot give way, the type does: a keypad stays three columns, so "Erase" and "Clear" step down in size instead. The rule across the app is that a phrase may wrap across lines, a single word never splits, and the layout gives way before the type does. See `DECISIONS.md` D24.

## Icons

Glyph sizes were raised above the grid's original values on August 29, 2026, after the app was seen on a device, then raised again. The grid argued its touch targets from the aging literature but let the glyphs inside them inherit conventional sizes. The values, which the grid also carries: tile glyph 80dp, row icon 64dp, or 58dp where a metadata line sits under the label, key glyph 40, 48, or 64dp by key height, Home and Back 44dp, lamp 48dp, day part mark 42dp, status dot 20dp, threshold mark 88dp, dial pad digits 50sp.

The stroke came down from 2.7 to 2.2 in the same change, and that number is not decoration. A stroke in viewport units scales with the icon, so the old weight at the new sizes would have thickened every line by a quarter and closed the gaps inside the camera and the photo frame. At 2.2 the absolute stroke lands within a tenth of a dp of what it was at every size in `Dimens`. Change a size without rechecking that arithmetic and the shapes fuse.

Two rankings are deliberate. A third party app icon stays at 60dp, smaller than the 80dp built in line icon, because that difference is what tells the two apart, and both sit in one 80dp slot so the two tile kinds cannot differ in height. And the lamp glyph at 48dp outranks the day part mark at 42dp, because the lamp backs a colored signal that must never carry meaning alone while the sun and moon are decoration.

## Touch

- Home tiles 128dp tall, about 20mm. With the corrected glyph they settle around 148dp.
- Key floor 88dp, about 14mm. Keypad keys 96dp. Small keys 72dp.
- These are far above Android's 48dp minimum, deliberately. Older and tremor affected users need substantially larger targets, and the aging literature shows meaningfully higher error rates at standard sizes.
- No swipe, drag, long press, double tap, or pinch is required anywhere in the app. Older adults prefer tapping to direct manipulation, and dragging is exactly where tremor turns into misfires.
- Enlarged touch slop so a small drag still counts as a tap. Debounced repeat taps. Respect the system touch and hold delay.

## Voice and copy

- Plain sentences. The status pill and the lamp speak in complete sentences, not fragments or status codes. "All is well." "Your ringer is off. You will not hear calls."
- Labels say what the person does, not what the software does. "Hold still", not "freeze frame". "Take it off the home screen", not "remove" or "delete".
- Placement language, never destruction language. Nothing in this app is ever deleted or uninstalled.
- Instructional footnotes are banned. A note on a screen has to earn its place by being one of three things: a brand promise ("Everything happens on this phone. Nothing is sent anywhere."), a Play policy requirement ("Not medical advice."), or a statement of where something will happen ("The new app lands at the bottom of your home screen."). If it is none of those, delete it and let the interface prove the point instead.
- Reassurance belongs at first run and in Settings, not sprinkled on every screen.
- No em dashes. American English.

## The signature elements

These three are what the design is remembered by. Do not dilute them.

1. **The navy masthead.** One bold move at the top of the home screen carrying the serif clock. It anchors the screen so everything below can be calm rather than empty.
2. **The day part mark.** A small drawn sun over a horizon line that becomes a moon over the same line in the evening. It is decoration that is also the orientation in day feature, which is real support for mild cognitive change.
3. **The sentence that speaks.** The status pill and the lamp, which turn the phone from an object with mysterious states into something that tells you plainly what is wrong and offers to fix it.

## Banned

Colored accent bars. Left border cards. Purple. Gradients. Glassmorphism. Inter. All caps section labels. Serif italic as an accent. Sparkles. Generic cards at uniform weight with no hierarchy. Any element carrying two separation devices. Emoji in the interface.

## Verification checklist for any screen

- Does it match the grid measurements.
- Does it survive 200 percent font scale with nothing clipped, overlapped, or broken mid word.
- Is every element labeled for TalkBack with a sensible focus order.
- Does every colored signal also have an icon and words.
- Is every target at or above the floor with adequate gaps.
- Does it work in all three themes.
- Does every note on it qualify as a promise, a policy requirement, or a statement of place.
- Does it mirror correctly in Arabic.
