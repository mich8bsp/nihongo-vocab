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
- Implemented in `data/AnswerService.kt` (`isCorrectAnswer` +
  `AnswerService.submitAnswer`), operating on `EntryDao`/`PoolStateDao`.

## Vocabulary data source

- **Vocab/kanji (N5–N1)**: [elzup/jlpt-word-list](https://github.com/elzup/jlpt-word-list)
  (`src/n5.csv`–`src/n1.csv`), MIT licensed (Jamie Sinclair / elzup,
  2020) — attribution kept in `app/src/main/assets/vocab/ATTRIBUTION.md`.
  - Correction from initial plan: Bluskyo/JLPT_Vocabulary (the originally
    picked primary source) turned out to only have kanji + reading, no
    English meanings at all — useless for this app's answer-checking, so
    it's not used. elzup's CSVs have `expression,reading,meaning,tags`
    columns, which is what's actually needed.
  - Reshaped by `scripts/generate_vocab_assets.py` into the `Entry`
    schema: `meaning` column split on `,` into the `meanings` list,
    same-text rows within a level merged (union of meanings), and any
    expression appearing in more than one level's file is kept only in
    the easiest level it appears in (small overlaps exist between level
    files in the source data).
  - `reading` and `tags` columns from the source are discarded — not part
    of the `Entry` schema (design only quizzes English meaning).
- **Kana (hiragana + katakana)**: hand-authored via
  `scripts/generate_kana_assets.py` (a Python table of char/romaji pairs,
  not an external dataset). Covers seion + dakuten + handakuten (142
  entries total); youon (contracted sounds like きゃ) are out of scope for
  v1 since they're combinations of already-covered characters.

## Screens

**Home screen** (default screen; also reached via "Back to Home" from quiz)
- Per-level stats: correct / wrong counts, mastered count out of total.
- Per-level enable/disable toggle.
- **Practice button**: picks a random active entry the same way a
  notification would (`pickRandomActiveEntry`, shared with
  `QuizNotificationWorker` so both pick identically) and opens Quiz for
  it directly — an on-demand way to practice without waiting for a
  notification, and a much faster way to test the quiz flow than fighting
  notification timing or OEM battery restrictions. Shows a message
  instead if nothing's available (no enabled pool, or everything in the
  enabled pools already mastered).
- Implemented in `ui/HomeScreen.kt`, backed by `EntryDao.getStatsByLevel()`
  and `PoolStateDao.getAll()`/`setEnabled()`. This is `MainActivity`'s
  actual launch screen now (after seeding completes).

**Quiz screen** (opened via notification tap, with entry id as extra)
- Word/kana/kanji display, free-text field, submit button.
- Submit → feedback (correct/incorrect + correct answer) → "Back to Home".
- Implemented in `ui/QuizScreen.kt`: a self-contained composable taking
  `entryId` + `AnswerService` + an `onBack` callback. No formal navigation
  graph — see "Navigation" below, this turned out not to need one.
  `MainActivity` shows it whenever it has a real entry id (from a
  notification tap), `onBack` clears that back to `HomeScreen`.
- The Column uses `Modifier.imePadding()` so the keyboard doesn't cover
  the Submit button when the answer field is focused — paired with
  `android:windowSoftInputMode="adjustResize"` on `MainActivity` in the
  manifest, the standard combination for reliable keyboard-avoidance
  behavior across OEMs (this app has already hit one Samsung-specific
  surprise, see Part 9 in TODO.md, so pairing both rather than relying on
  just one).
- System back button on Quiz is intercepted with `BackHandler(onBack =
  onBack)` so it returns to Home (same as the "Back to Home" button)
  instead of the default Activity behavior of finishing the app — there's
  no back stack (see "Navigation"), so an unhandled back press would just
  exit.

## Navigation

Home ⇄ Quiz is just a `quizEntryId: Long?` bit of state in `MainActivity`:
null shows `HomeScreen`, non-null shows `QuizScreen` for that id. Set from
`EXTRA_ENTRY_ID` on the launch intent (cold start from a notification tap)
or `onNewIntent` (already-running instance, `launchMode="singleTop"` so
repeat taps don't stack activities); cleared by Quiz's `onBack`. No
Compose Navigation / `NavHost` — with exactly 2 screens, one trivial
transition, and no back-stack requirements, a route graph would be pure
ceremony. Reconsider only if a real need for more screens or back-stack
behavior shows up.

## Notifications

- Random interval within active hours: 20–90 minutes, clamped into an
  8am–10pm window (rolls to next day's 8am if a pick would otherwise land
  after 10pm). Exposing this as a user setting is a later nice-to-have,
  not required for v1. Implemented in `notification/NotificationScheduling.kt`
  (`computeNextDelayMillis`) — pure function, randomness is an injectable
  parameter so it's fully unit-testable.
- `POST_NOTIFICATIONS` runtime permission requested on launch (Android
  13+); if denied, notifications just silently don't show — no further
  handling.
- `notification/QuizNotificationWorker` (`CoroutineWorker` via
  WorkManager, not `AlarmManager`/exact alarms — timing doesn't need to
  be tight for passive practice): picks a random active entry from the
  enabled pools, posts a notification (title "Quiz time", body = entry
  text) if one was found, then always reschedules itself via
  `enqueueUniqueWork` (`REPLACE` when called from within the worker,
  `KEEP` when called from `MainActivity` on app start so relaunching the
  app doesn't reset an in-flight countdown). No active entries in any
  enabled pool → the DAO query naturally returns null (empty SQL `IN ()`
  matches nothing) and the worker just skips posting, still reschedules.

## Tech stack

- `targetSdk = 36` (Android 16, matches dev device), `minSdk = 33`
  (Android 13 — floor of the notification-permission model this app relies
  on; no reason to go lower for a single-device personal app).
- Package/applicationId: `io.github.mich8bsp.nihongovocab` (personal
  project, no owned domain — `io.github.<username>` convention).
- Repo: https://github.com/mich8bsp/nihongo-vocab (public).
- Kotlin + Jetpack Compose
- Room (local DB)
- WorkManager for scheduling
- Word lists (JLPT-tagged vocab + kana charts) bundled as JSON assets,
  seeded into Room on first launch — see "Vocabulary data source".
  Seeding (`AssetSeeder`) inserts entries and `PoolState` atomically in
  one `db.withTransaction { }`, guarded by `entryDao.count() > 0` — this
  guard is only reliable if seeding truly is all-or-nothing (a process
  death mid-seed must never leave entries populated but pool_state
  empty, since that would silently disable re-seeding forever).
- No backend, no accounts, no sync

## App icon

Adaptive icon (`mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`):
a simple vermillion torii gate (`drawable/ic_launcher_foreground.xml`, plain
rectangles for kasagi/shimaki/pillars/nuki, `#C8332B`) on a cream background
(`drawable/ic_launcher_background.xml`, `#FDF6EC`). No legacy PNG mipmaps —
`minSdk 33` is well above adaptive icons' API 26 floor, so the vector-only
adaptive icon covers every supported device. Wired via `android:icon` /
`android:roundIcon` on `<application>` in the manifest.

## Out of scope for v1

- Accounts / multi-device sync
- Configurable active-hours window (hardcoded default first)
- Reading-based quizzing (meanings only, per earlier decision)
- Multiple choice / distractor selection (free text only, per decision)
