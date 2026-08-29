# VisionLauncher

An Android home screen replacement built around low vision and aging. Large fixed
tiles that never reorder themselves, a serif clock over a navy masthead, and a status
line that speaks in complete sentences: "All is well," or "Your ringer is off. You
will not hear calls," with a repair key when a one tap fix exists.

Everything runs on the phone. No account, no cloud, no analytics, no telemetry, no
ads, no subscriptions. Network access exists only for features you explicitly turn
on, and the only one of those planned is a weather line that is off by default.

Built by Kamsiob.

---

## Who it is for

Two overlapping audiences, served by one design:

- Aging adults living with the ordinary visual, motor, and cognitive changes of later
  life.
- People of any age with usable but impaired vision, many of whom reject anything
  framed as a senior product.

It is not a screen reader product and it does not compete with TalkBack. It is a
visually premium launcher engineered so that large targets, plain sentences, and a
layout the hands can memorize are the design, not an accessibility mode bolted on.
Every screen is labeled for TalkBack and tested at 200 percent font scale, because
vision that is impaired today may be more impaired next year.

## What it deliberately cannot do

These are design decisions, not missing features. The reasoning lives in
`DECISIONS.md`.

- **It does not collect anything.** There is nowhere for your data to go. Messages
  the app sees are stored only on the phone and are never transmitted.
- **It does not take over SMS.** Becoming the default SMS app would silently drop
  you off RCS onto plain SMS, degrading family photos, group chats, and encryption.
  Instead, messages are read from their notifications and replies travel through
  each app's own delivery.
- **It does not become the default dialer.** Calling uses the phone's own call
  system behind a large button front end.
- **It does not reimplement emergency calling.** The Emergency screen hands off to
  the phone's own emergency flow. It does not claim fall detection, because no
  honest way to provide it exists for a third party app.
- **It is not a medical device.** The Today screen shows reminders written by you or
  your helper. It contains no medical content, no dosing advice, and no drug
  information.
- **It does not use an AccessibilityService.** Play policy excludes launchers from
  the accessibility tool exemption, so everything is built on narrower, sanctioned
  APIs.
- **It never rearranges itself.** Nothing on the home screen moves unless you move
  it, and every change can be undone.

## Where things stand

Pre Stage 1: the specification and design are complete, the Android project is being
set up. Nothing is installable yet. `HANDOFF.md` carries the current state,
`MASTER_SPEC.md` the screen by screen specification, `DESIGN.md` the visual system,
and `design/design-grid-v4.html` is the measurement authority for every screen.

## License

AGPL-3.0. See `LICENSE`. Bundled fonts are Atkinson Hyperlegible Next, Atkinson
Hyperlegible Mono, and Young Serif, all under the SIL Open Font License 1.1.

## Support this work

If this launcher helps you or someone you care for, you can support it at
[buymeacoffee.com/kamsiob](https://buymeacoffee.com/kamsiob). Nothing in the app is
ever paid or locked.
