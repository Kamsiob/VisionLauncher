# RESEARCH

The evidence behind the design. Read this when a choice seems arbitrary, or before proposing to change one.

Confidence is marked plainly. Some of this is well established, some is contested, and saying which is which is part of the point.

## Visual aging, the universal baseline

Independent of any disease, aging brings presbyopia, reduced contrast sensitivity, reduced light transmission through a yellowing lens, increased glare sensitivity, slower dark adaptation, blue and yellow discrimination loss, and a reduced useful field of view. Every user over 70 has some of this. **Confidence: high.**

Consequences in the app: warm off white rather than pure white, high contrast throughout, no meaning carried by color alone, no reliance on peripheral cues.

## The four major low vision conditions want contradictory things

- **Macular degeneration**: central blind spot, preserved periphery, faces hard to recognize. Wants very large elements and content that can be viewed off center.
- **Glaucoma**: peripheral loss progressing to tunnel vision, central acuity preserved late. Wants compact central layouts and nothing critical at the edges.
- **Diabetic retinopathy**: patchy loss and vision that fluctuates day to day. Wants redundancy in every channel and settings that are quick to change.
- **Cataract**: light scatter, glare, contrast loss, often photophobia. Wants glare control, and genuinely benefits from dark themes.

**Confidence: high.** These conflict directly. AMD wants content off center, glaucoma wants it centered. Cataract wants light on dark, astigmatism wants the opposite. The only coherent answer is a high contrast, large, generously spaced, center weighted baseline plus a small number of switches the user sets once. Which is why this app has three themes and three text sizes and then holds still.

## Font choice matters much less than the marketing claims

There is no published controlled study showing Atkinson Hyperlegible produces faster reading or better acuity than a standard sans for low vision readers. The strongest analogous evidence, testing purpose built macular degeneration fonts against standard faces, found no advantage over Courier and identified inter letter spacing rather than letterform distinctiveness as the significant predictor. Related work found no significant difference in maximum reading speed across fonts, and the authoritative systematic review found the research has not produced consistent findings. The same dissociation between preference and performance appears in the dyslexia font literature. **Confidence: high that the effect is small; the specific claims for individual fonts are unproven.**

Consequence: invest in print size, contrast, and spacing. Choose a license clean face with disambiguated characters and stop there. Never make a clinical claim about the typeface.

Above the critical print size there is a wide fluent range where reading speed is at maximum, so once text is comfortably large, fine typographic details matter little. **Confidence: high.**

## Contrast math

WCAG 2.x contrast is known to be flawed: it overstates contrast for dark pairs and understates it for light pairs, and it cannot give reliable dark mode guidance. APCA models perception better and is the candidate method for WCAG 3, but is not yet ratified and its validation is still under review. **Confidence: high on the flaw, medium on APCA as the replacement.**

Consequence: design to APCA where possible, treat WCAG AAA as a floor rather than the target, and verify with a real checker before making any compliance claim.

Separately, WCAG 1.4.11 requires 3:1 contrast for user interface component boundaries. A soft shadow generally cannot meet this against a light background; a border can. This is the entire reason the Outlined theme exists. **Confidence: high.**

## Dark mode is contested

Dark mode helps some cataract and photophobia users by cutting total light output. It hurts the large fraction of adults with astigmatism, commonly cited around one in three, through halation, where light text appears to bleed into a dark background because the dilated pupil admits more aberrated light. For extended reading the eye generally prefers dark on light. **Confidence: medium to high.**

Consequence: light default, dark offered as an obvious choice, never forced, never buried.

## Touch targets need to be much larger than the platform minimum

Android's 48dp minimum covers roughly 94 percent of adult fingertips for the general population. Research on one handed thumb use found roughly 9.2mm sufficient for discrete tasks in the general population, and studies of elderly thumb performance found small targets around 3mm cause rapid fatigue and slower tapping compared to 9mm. Summaries of the aging literature report substantially higher error rates for users over 65 at standard sizes and recommend meaningfully larger targets. Tremor research shows oscillation causes both overshoot and duplicate inputs, both mitigated by larger targets and wider spacing. **Confidence: high on the direction, medium on any specific number.**

Consequence: 128dp tiles, 88dp key floor, wide gaps, and empirical validation with tremor affected users before treating these numbers as final.

## Touch failure modes in older adults

Documented: unintentional drag during a tap, long dwell misread as a long press, double tap timing difficulty, difficulty initiating and completing swipes, difficulty with multi touch and pinch, dry skin reducing capacitance and causing missed touches, tremor causing repeat taps. **Confidence: high.**

Consequence: enlarged touch slop, debounced repeats, and no swipe, drag, long press, double tap, or pinch required anywhere.

## Older adults prefer tapping to dragging

A gesture usability study found older participants prefer click to designs over direct manipulation dragging, and reviews of the area favor control tapping over gestures generally. **Confidence: medium to high.**

Consequence: the entire arranging model. Tap the app, tap the destination, they trade places.

## Flat design tests badly with older adults

Older adults perform worst with flat design, meaning simplified shapes without shadows, while skeuomorphic and skeuominimalist treatments with real world depth cues improve both performance and the ability to identify what is clickable. **Confidence: medium.**

Consequence: keys are raised cards, not flat rectangles. But note the limit of this evidence. It concerns affordance perception in older adults, and it does not license removing borders for low vision users who depend on edge contrast. Both findings are respected by having a shadow default and an Outlined alternative.

## Procedural memory outlasts episodic memory

Working memory and error recovery decline with age, while procedural memory, the memory of how to do a familiar sequence, is much better preserved. **Confidence: high.**

Consequence: a fixed spatial layout that the hands learn is worth more than an adaptive or predictive one, however elegant. This is the single most load bearing finding in the whole design.

## Fear of breaking the phone drives abandonment

Seniors abandon smartphones largely from fear of making mistakes and feeling foolish rather than inability to learn. **Confidence: high.**

Consequence: "You can't break anything." is the first sentence of the product, undo exists on everything, and no action is a dead end.

## Stigma and aesthetics drive assistive technology abandonment

Abandonment of assistive devices is high, with older work finding roughly 29 percent of devices completely abandoned and abandonment concentrated in the first year. Research on assistive technology aesthetics found devices with modern aesthetics and no negative symbolism were accepted where traditional assistive aesthetics were rejected, and concluded aesthetics must be treated as co-equal with function. Separate work identifies the specific factors that induce stigma perception in older adults: lack of aesthetic appeal, poor affordance, and accentuated social signifiers, with obvious instructions named as a signifier. **Confidence: medium to high.**

Consequence: premium visual design is a functional requirement, not vanity. And it is why instructional footnotes were purged from every screen.

## Hearing loss travels with vision loss

Vision and hearing loss frequently co-occur in aging, and hearing aid abandonment is itself very high. **Confidence: high.**

Consequence: never rely on audio alone for feedback. Haptics are a primary confirmation channel. And the See and hear better screen covers hearing aid pairing and captions, which no competitor in this category addresses.

## Elder fraud

Reported losses among victims 60 and older reached billions of dollars in recent FBI reporting with sharp year over year increases, and these are reported figures that undercount actual fraud. **Confidence: high on the direction.**

Consequence: reduce surface area where the helper wants it, never open unknown links silently, never ask for payment or personal data, model trustworthy behavior with no ads or upsells, and be honest that a launcher cannot block scam calls at the system level.

## What the competition does and does not do

BIG Launcher is the category leader: very large color coded buttons, an SOS that texts GPS location, a caregiver PIN, app hiding, and support for a large number of languages. Its users complain about paywalled features, dated design, and, revealingly, about features Android simply forbids apps to do, like locking the volume or Wi-Fi. The most requested unmet need in the whole category is the accidentally silenced ringer, which no app can fix by locking, but which any app can detect and offer to repair. That is where the attention queue came from.

Purpose built hardware like the RAZ Memory Cell Phone shows where this audience goes as cognition declines: a single always on screen of large photo contacts, a dedicated emergency key, no app store, and full caregiver control. It costs hundreds of dollars plus a subscription and runs a cloud portal.

Apple's Assistive Access is the most serious recent platform attempt. It reduces the phone to a handful of large apps, merges Phone and FaceTime, offers an explicit choice between a row layout and a large grid layout, and includes emoji only and video message options for people who cannot type. The layout choice and the no typing reply patterns are both worth matching.

Minimalist launchers like Niagara prove premium design sells in this space, but their adaptive lists, small type, and gesture reliance are wrong for this audience, and their data sharing is the opposite of this app's stance.

## The platform limits that shape everything

A launcher cannot control the system Settings interface, the notification shade internals, the status bar, the quick settings panel, text inside other apps, the keyboard, system dialogs, or permission prompts. It cannot lock volume, Wi-Fi, or airplane mode. **Confidence: high.**

Consequence: the threshold pattern. Say what the next screen is, warn that it looks different, promise the way back, then let go. Turning the platform's limits into stated, trustworthy behavior is itself a differentiator, because the alternative is silent failure.

## What still needs testing with real users

Nothing below is settled by the literature. Test these before treating the design as validated.

1. Whether the fixed layout reduces anxiety over repeated sessions compared to the person's current phone.
2. Error and repeat tap rates at the chosen target sizes, with tremor and arthritis users.
3. Typeface and size preference and actual reading speed at large sizes, across all four low vision conditions.
4. Whether the default soft shadows provide enough edge separation, or whether Outlined should become the default.
5. Whether the light default and the discoverability of the dark switch work for both astigmatism and photophobia users.
6. Whether the two tap trade places model is understood without explanation.
7. Whether the attention queue banner gets noticed and understood.
8. Whether the magnifier and read aloud combination gets adopted, and whether freeze frame beats live view for shaky hands.
9. Whether both audiences, the 85 year old and the 45 year old with congenital low vision, feel the product is for them.
