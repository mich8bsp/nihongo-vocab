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
