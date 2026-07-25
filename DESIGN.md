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
5. User taps "Next" to keep quizzing (picks another random active entry
   the same way, staying on the Quiz screen) or presses back to return to
   **Home screen** (no separate "Back to Home" button - system back
   already does this, see "Navigation").
6. Home screen is also what's shown when the app is opened normally (not via
   notification).

## Data model

**Entry**
- `id`
- `text` — the kanji/kana/word as shown in the notification
- `meanings: List<String>` — acceptable English answers (dictionary entries
  often have multiple valid glosses)
- `romaji: String` — Hepburn romaji of the entry's reading, shown alongside
  the meanings after answering (empty for `KANA` entries, where the
  meaning itself already is the romaji - see "Answering")
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
  normalizing both sides (lowercase, trim whitespace). Also accepts the
  same meaning with any `(...)` context clarification dropped, e.g.
  `mother (formal)` → `mother`, `(my) older brother (humble)` → `older
  brother` - the source glosses use parentheses for nuance/grammar hints
  the user shouldn't be required to type.
- If a plain match feels too strict in practice, add a small edit-distance
  tolerance (typo forgiveness) — stdlib-level, no new dependency. Not
  needed up front; the `meanings` list (split from multi-gloss source data)
  already covers most valid synonym cases.
- Updates `correctStreak`, `totalCorrect`/`totalWrong`, then re-checks pool
  completion for that entry's level.
- Implemented in `data/AnswerService.kt` (`isCorrectAnswer` +
  `AnswerService.submitAnswer`), operating on `EntryDao`/`PoolStateDao`.
  `submitAnswer` and `giveUp` (see "Screens" → Quiz screen) both funnel
  into a private `recordResult(entry, correct)` so the streak/counters/
  pool-completion logic lives in exactly one place.
- The post-answer feedback also shows the entry's `romaji` in parentheses
  after the meanings, e.g. `counter for small animals (~hiki)` — except
  for `KANA` entries, where the answer being checked *is* the romaji
  already (showing it again would be redundant).
- **Romaji leniency**: when looking at kanji, the instinct is sometimes to
  type the reading instead of translating it. If Submit's answer doesn't
  match a meaning but does match the entry's `romaji` (`isRomajiAnswer`,
  same normalization as `isCorrectAnswer`), the attempt is *not* scored at
  all - no streak reset, no `totalWrong` bump. Instead the Quiz screen
  shows a hint ("That's the romaji reading - try the English meaning")
  and leaves the text field open for a real attempt, which then goes
  through the normal correct/incorrect scoring. The hint clears as soon
  as the user edits the field again. Always false for `KANA` entries
  (`romaji` is blank there, see above).

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
    schema, same-text rows within a level merged (union of meanings), and
    any expression appearing in more than one level's file is kept only
    in the easiest level it appears in (small overlaps exist between
    level files in the source data).
  - The `meaning` column isn't a plain comma-separated list: elzup uses
    `;` between distinct senses and `,` between synonyms within a sense
    (e.g. `coat; court (e.g., tennis)` is the senses `coat` and
    `court (e.g., tennis)`), and a plain split-on-`,` corrupts this - it
    either never splits a `;`-only meaning (leaving an untypeable
    compound answer) or splits *inside* an `(e.g., ...)` aside, producing
    broken fragments with unbalanced parens. Found by auditing all
    generated entries (565 of 7836, ~7%, were affected).
    `split_meanings()` fixes this properly: mask `(...)` content first
    (so its `;`/`,` can't be mistaken for a separator), split on `;` then
    `,`, restore the masked content per part. Leaves a `_self_check()`
    covering the known tricky cases (asides, a `;` *inside* parens),
    run at the top of `main()`. ~10 entries still have a stray unmatched
    `(` because the *source* CSV itself has a typo (missing `)`) -
    accepted as a known ceiling, not worth special-casing for 10 rows.
  - Before committing to this fix, considered switching to a JMDict-based
    source (`AnchorI/jlpt-kanji-dictionary`) for genuinely clean gloss
    arrays - rejected: it has no per-word JLPT level tag (only kanji do),
    so elzup's level list would still be needed anyway, plus it's ~57MB,
    needs kanji+reading cross-referencing with homograph-ambiguity risk,
    and a second license to attribute, all to solve a problem the parser
    fix above already solves directly.
  - `tags` column from the source is discarded. `reading` is kept, but not
    stored verbatim — `scripts/generate_vocab_assets.py` converts it to
    Hepburn romaji at generation time via `pykakasi` (not a repo
    dependency, only needed to regenerate the assets) and stores that as
    `Entry.romaji`. Precomputing in Python means the app itself needs no
    kana→romaji conversion logic at runtime.
- **Kana (hiragana + katakana)**: hand-authored via
  `scripts/generate_kana_assets.py` (a Python table of char/romaji pairs,
  not an external dataset). Covers seion + dakuten + handakuten (142
  entries total); youon (contracted sounds like きゃ) are out of scope for
  v1 since they're combinations of already-covered characters.

## Screens

**Home screen** (default screen; also reached via system back from Quiz)
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
- Word/kana/kanji display, free-text field, Submit button, and a "Give
  Up" button next to it for when the user doesn't know the answer and
  doesn't want to type gibberish just to move on — recorded identically
  to a wrong answer (`AnswerService.giveUp`: streak reset, `totalWrong`
  incremented, same `AnswerResult` shape), just skipping the string
  comparison. Unlike Submit it's always enabled (no text required).
- Submit checks locally first whether the typed answer is a romaji guess
  (see "Answering" → Romaji leniency): if so it shows the hint and stays
  on the entry field instead of scoring anything, otherwise it calls
  `AnswerService.submitAnswer` as normal.
- Submit/Give Up → feedback (correct/incorrect + correct answer) → "Next" button,
  which picks another random active entry (`AnswerService.pickNext()`,
  same selection a notification/the Home Practice button would make) and
  swaps the Quiz screen straight to it — no detour through Home. Falls
  back to a "nothing to practice" message (same wording as Home's
  Practice button) if nothing's left to quiz. There's no separate "back
  to Home" button on the result screen - the system back button already
  does that (see below), so a dedicated button would just duplicate it.
- Implemented in `ui/QuizScreen.kt`: a self-contained composable taking
  `entryId` + `AnswerService` + `onBack` + `onNext: (Long) -> Unit`
  callbacks. No formal navigation graph — see "Navigation" below, this
  turned out not to need one. `MainActivity` shows it whenever it has a
  real entry id (from a notification tap or `onNext`); `onBack` clears
  that back to `HomeScreen`, `onNext` just swaps in the new entry id
  (identical wiring to Home's `onPractice`).
- The Column uses `Modifier.imePadding()` so the keyboard doesn't cover
  the Submit button when the answer field is focused — paired with
  `android:windowSoftInputMode="adjustResize"` on `MainActivity` in the
  manifest, the standard combination for reliable keyboard-avoidance
  behavior across OEMs (this app has already hit one Samsung-specific
  surprise, see Part 9 in TODO.md, so pairing both rather than relying on
  just one).
- System back button on Quiz is intercepted with `BackHandler(onBack =
  onBack)` so it returns to Home instead of the default Activity behavior
  of finishing the app — there's no back stack (see "Navigation"), so an
  unhandled back press would just exit.

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
- Room (local DB), schema version 2 (v1 → v2 added `Entry.romaji` via
  `MIGRATION_1_2`, an `ALTER TABLE ... ADD COLUMN ... DEFAULT ''` - existing
  installs keep all progress, just with an empty `romaji` on
  already-seeded entries until re-seeded; `exportSchema` stays off, still
  not worth a schema-history folder for a single-device personal app)
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
