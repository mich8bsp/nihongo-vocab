# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

Nihongo Vocab — an Android app for passive Japanese vocabulary practice via
notifications. Full design: `DESIGN.md`. Implementation plan and current
progress: `TODO.md`.

## Keep DESIGN.md and TODO.md in sync — this is critical

This project is developed across many sessions, possibly stopping mid-task
on a token limit. `DESIGN.md` and `TODO.md` are the source of truth for
resuming, so:

- **Start of session**: read `DESIGN.md` and `TODO.md` first, before
  touching code, to reconstruct current state.
- **After finishing a TODO item**: check it off in `TODO.md` immediately —
  don't batch checkoffs for later in the session.
- **When a design decision is made or changed during implementation**
  (something not covered in `DESIGN.md`, or that contradicts it), update
  `DESIGN.md` immediately so it always reflects the app as actually being
  built, not just as originally planned.
- If a session ends mid-item, leave a short note under that TODO item
  (e.g. sub-bullet) describing exactly what's left, so the next session
  doesn't have to re-derive it from a half-finished diff.

Never let these two files drift from the real code/design state.

## Building/testing from the CLI in this environment

The default shell has no `java`/`gradle` on `PATH` and no `ANDROID_HOME`,
even though Android Studio is installed — its bundled JDK isn't on the
default `PATH`. Don't conclude the build can't be verified; use:

```
JAVA_HOME=/home/michael/Devl/tools/android-studio/jbr ./gradlew <task>
```

`local.properties` (gitignored) already has `sdk.dir` pointing at
`/home/michael/Android/Sdk`, written by Android Studio. `./gradlew test`
and `./gradlew assembleDebug` both work fine with just `JAVA_HOME` set
this way — no need to punt build verification to the user by default.

## Testing standards

Everything should be tested in a reasonable manner. Concretely:

- Non-trivial logic (branches, parsing, streak/pool state transitions,
  anything answer-checking or mastery/auto-advance related) gets a real
  test — prefer a plain JVM unit test (`app/src/test`) over an
  instrumented one wherever the logic doesn't actually need a Context/
  Room/Android framework class, since JVM tests run in `./gradlew test`
  without a device and stay fast.
- Tests should be terse but meaningful: cover the real branches/edge
  cases (e.g. streak reset on wrong answer, mastery at exactly 3, a pool
  with no "next") without padding — no speculative cases, no testing
  the framework itself, no restating what the type system already
  guarantees.
- Trivial one-liners and pure UI layout don't need tests.
- Run `./gradlew test` (see above for the `JAVA_HOME` needed in this
  environment) before considering a TODO item done, not just after the
  user reports back from a device.
