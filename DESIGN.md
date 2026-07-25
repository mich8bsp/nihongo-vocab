# Nihongo Vocab — Design Document

Android app for passive Japanese vocabulary practice. Fires notifications
with a random word/kana/kanji; tapping opens a quiz for it. Fully offline,
single user, no backend.

## Core loop

1. App picks a random entry from the currently **enabled pools'** active
   (unmastered) entries and fires a notification showing the word/kana/kanji.
2. Tapping the notification opens the **Quiz screen** for that entry.
3. User answers via free text (no multiple choice — typing the answer
   tests recall better than picking from options).
4. App shows correct/incorrect feedback + the correct answer.
5. User taps "Back to Home" → **Home screen**.
6. Home screen is also what's shown when the app is opened normally (not via
   notification).

## Data model

**Entry**
- `id`
- `text` — the kanji/kana/word as shown in the notification
- `meanings: List<String>` — acceptable English answers (dictionary entries
  often have multiple valid glosses)
- `level` — KANA / N5 / N4 / N3 / N2 / N1. Kana is its own pool, separate
  from N5–N1 vocab/kanji.
- `correctStreak: Int` (0–3) — consecutive correct answers. Any wrong answer
  resets this to 0. At 3, the entry is "mastered" and drops out of the
  notification pool (but stays in the DB).
- `totalCorrect: Int`, `totalWrong: Int` — lifetime counters, never reset.
  Used for the stats screen. Independent of `correctStreak`.

**PoolState** (one row per level)
- `level`
- `enabled: Boolean` — whether this level's entries are eligible for
  notifications right now.

Default: KANA and N5 `enabled = true`, N4–N1 `enabled = false`.

## Pool enable/disable rules

- A pool is **complete** when every entry in it has `correctStreak >= 3`.
- Each pool has a fixed "next" pool for auto-advance purposes:
  `KANA → (none)`, `N5 → N4 → N3 → N2 → N1 → (none)`. Kana sits outside the
  JLPT chain — it starts enabled alongside N5 but doesn't feed into it.
- On completion: that pool auto-disables, and if it has a "next" pool that
  isn't already enabled, the next pool auto-enables.
- The user can manually enable/disable any pool at any time from the Home
  screen (e.g. re-enable N5 or Kana to review old words while also studying
  N4). Manual toggles are not overridden except by the auto-advance rule
  above.
- Multiple pools can be enabled simultaneously. Notifications draw from the
  combined active-entry set of all enabled pools.
- If no enabled pool has any active (unmastered) entries, no notification
  fires (nothing to quiz).

## Answering

- **Free text only**: no LLM grading — every check is local, instant, and
  deterministic (see "Vocabulary data source" for why: offline-first, no
  backend, reproducible feedback).
- Match against any string in the entry's `meanings` list, after
  normalizing both sides (lowercase, trim whitespace).
- If a plain match feels too strict in practice, add a small edit-distance
  tolerance (typo forgiveness) — stdlib-level, no new dependency. Not
  needed up front; the `meanings` list (split from multi-gloss source data)
  already covers most valid synonym cases.
- Updates `correctStreak`, `totalCorrect`/`totalWrong`, then re-checks pool
  completion for that entry's level.

## Vocabulary data source

- **Vocab/kanji (N5–N1)**: converted from the tanos.co.uk JLPT list via
  [Bluskyo/JLPT_Vocabulary](https://github.com/Bluskyo/JLPT_Vocabulary)
  (JSON, already has word/reading/meaning/level). Reshaped by a one-off
  script into the `Entry` schema — `meaning` strings split on `;`/`,` into
  the `meanings` list.
  - Fallback/alternative if this dataset has gaps:
    [elzup/jlpt-word-list](https://github.com/elzup/jlpt-word-list).
  - No stated reuse license on these repos (derived from tanos.co.uk, which
    also has none posted) — fine for personal offline use; revisit if this
    app is ever published publicly.
- **Kana (hiragana + katakana)**: hand-authored JSON, not sourced from any
  dataset. It's a fixed ~100-character set — writing it directly is less
  work than finding/normalizing an external source for it.

## Screens

**Home screen** (default screen; also reached via "Back to Home" from quiz)
- Per-level stats: correct / wrong counts, mastered count out of total.
- Per-level enable/disable toggle.

**Quiz screen** (opened via notification tap, with entry id as extra)
- Word/kana/kanji display, free-text field, submit button.
- Submit → feedback (correct/incorrect + correct answer) → "Back to Home".

## Notifications

- Random interval within active hours (default window TBD, e.g. 8am–10pm;
  exposing this as a setting is a later nice-to-have, not required for v1).
- Requires `POST_NOTIFICATIONS` runtime permission (Android 13+) and either
  `WorkManager` periodic-ish scheduling or `AlarmManager` with
  `SCHEDULE_EXACT_ALARM` if tighter timing is wanted. WorkManager is the
  lazier/more robust default; exact alarms only if timing feels too loose in
  practice.

## Tech stack

- `targetSdk = 36` (Android 16, matches dev device), `minSdk = 33`
  (Android 13 — floor of the notification-permission model this app relies
  on; no reason to go lower for a single-device personal app).
- Kotlin + Jetpack Compose
- Room (local DB)
- WorkManager for scheduling
- Word lists (JLPT-tagged vocab + kana charts) bundled as JSON assets,
  seeded into Room on first launch — see "Vocabulary data source"
- No backend, no accounts, no sync

## Out of scope for v1

- Accounts / multi-device sync
- Configurable active-hours window (hardcoded default first)
- Reading-based quizzing (meanings only, per earlier decision)
- Multiple choice / distractor selection (free text only, per decision)
