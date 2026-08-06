# Nihongo Vocab — Design Document

Android app for passive Japanese vocabulary practice via notifications.
Offline, single-user, no backend.

## Core loop

1. A random active (unmastered) entry from the enabled pools fires either a
   Quiz notification (tap opens the quiz) or a Reveal notification
   (word+romaji+meaning, dismiss-only, not clickable) — 1:4 quiz:reveal
   ratio.
2. Entries containing kanji: stage 1 (type the reading in romaji) gates
   stage 2 (the meaning, free text or multiple choice). KANA entries, and
   any other entry written entirely in kana (no separate reading to quiz),
   skip straight to stage 2.
3. Feedback (correct/incorrect + correct answer) → "Next" (another random
   entry, stays on Quiz) or system back → Home.
4. Home is also the normal launch screen (not via notification).

## Data model

**Entry**: `id`, `text`, `meanings: List<String>`, `romaji` (Hepburn, blank
for KANA), `level` (KANA/N5-N1/CUSTOM), `correctStreak` (0-3, resets on
wrong, mastered at 3 and drops out of the notification pool),
`totalCorrect`/`totalWrong` (lifetime, never reset).

**PoolState**: `level`, `enabled`. Default: KANA + N5 enabled. `CUSTOM`
gets a row like any other level - toggled and practiced (including via
notifications) the same way; only its content comes from the My
Vocabulary screen instead of bundled assets.

## Pool enable/disable rules

- A pool is complete when every entry has `correctStreak >= 3`.
- Auto-advance chain: `KANA→(none)`, `N5→N4→N3→N2→N1→(none)`. On
  completion, that pool auto-disables and its "next" auto-enables (if not
  already).
- User can manually enable/disable any pool anytime, independent of the
  chain; multiple pools can be enabled at once.
- Notifications draw from the combined active-entry set of all enabled
  pools; if none, no notification fires.

## Answering

- Two quiz modes (Settings toggle, `data/QuizPreferences.kt`): free text
  (default) or multiple choice (entry's own first meaning + 2 distractors
  from `EntryDao.getRandomOtherEntries`, same level, excluding any
  distractor meaning that collides with the entry's own). No LLM
  grading — every check is local, instant, deterministic.
- Reverse quiz (Settings toggle, independent of the above): prompt shows
  the English meaning, answer is the Japanese side — free text (checked
  via `isCorrectJapaneseAnswer`, accepts either the exact word/kana or its
  romaji reading, since typing kanji needs a JP IME) or multiple choice
  (`AnswerService.buildQuizOptions(entry, reverse = true)`, options are
  Japanese text instead of meanings). No two-stage reading gate or kana
  hint in this direction — nothing kanji to read yet. A KANA entry has no
  romaji (its meaning already is the romaji), so free-text reverse
  quizzing on KANA needs a JP IME; multiple choice works regardless.
- A My Vocabulary entry can have `text` blank if the user only filled in
  romaji + meaning (no kanji/kana typed in). `Entry.displayText()` (text,
  falling back to romaji) covers this everywhere text would otherwise
  render empty — Quiz screen, notifications, reverse-quiz feedback
  (`Entry.textWithRomaji()`).
- Two-stage quiz (entries with kanji only, via `Entry.hasKanji()`): stage 1
  (romaji reading, checked via `isRomajiAnswer`) gates stage 2 (the
  meaning). Stage 1 attempts aren't
  scored (no streak/counter change) — it's a gate, not the question. Only
  stage 2's result is ever recorded, via `AnswerService.submitAnswer`/
  `giveUp`. Both stages render on the same screen at once.
- Free-text matching (`isCorrectAnswer`, case/whitespace-insensitive)
  against any of `entry.meanings`, with leniency: drop a `(...)`
  clarification (trailing, or mid-phrase with the alt word substituted
  in), drop a leading "a"/"an"/"the", and treat spelled-out numbers/digits
  as interchangeable.
- Romaji leniency: if a submitted answer matches `entry.romaji` instead of
  a meaning, it's not scored at all — shows a hint ("try the English
  meaning") instead, since typing the reading is a common instinct when
  looking at kanji.
- Logic lives in `data/AnswerService.kt`; `submitAnswer`/`giveUp` share a
  private `recordResult` for streak/counter/pool-completion so that logic
  exists in one place.

## Romaji → kana conversion

`data/RomajiToKana.kt` (`romajiToKana`) derives the Quiz screen's kana
reading hint from `entry.romaji` — there's no per-kanji furigana data, so
a tap-a-kanji-to-reveal-it feature isn't possible without a new data
source; this needs none.

Greedy longest-match mora tokenizer, table seeded from `kana.json`'s
syllables plus yoon (contracted sound) and loanword-only combos (fa/fi/
fe/fo, di, che, ...). Long vowels need no special-casing since this
dataset's romaji already spells them mora-by-mora (e.g. "gakkou", not a
macron'd "gakkō"). Two extra rules: a doubled consonant is sokuon (っ),
and gemination before chi/cha/chu/cho is spelled "tch" rather than
doubling; an `n'` apostrophe disambiguates ん from the next mora. Known
gap: modern Hepburn collapses ぢ/づ into じ/ず, so those rare words get
the audibly-identical hint instead of the exact kana — acceptable since
this is a pronunciation aid, not a spelling reproduction. Validated by
round-tripping every `romaji` value in the bundled vocab (~7800 entries)
with zero unmapped characters left over.

## Vocabulary data source

- **N5–N1**: [elzup/jlpt-word-list](https://github.com/elzup/jlpt-word-list)
  (MIT, attribution in `app/src/main/assets/vocab/ATTRIBUTION.md`),
  reshaped by `scripts/generate_vocab_assets.py`. `reading` is converted
  to Hepburn romaji via `pykakasi` at generation time (not a runtime
  dependency).
  - Fixed gotcha: elzup's `meaning` column mixes `;` (distinct senses)
    and `,` (synonyms within a sense), with `(...)` asides that can
    contain either — a naive comma-split corrupted ~7% of entries.
    `split_meanings()` masks paren content before splitting. ~10 entries
    still broken from a source-CSV typo (accepted, not worth
    special-casing).
- **Kana**: hand-authored via `scripts/generate_kana_assets.py` (142
  entries: seion + dakuten + handakuten; youon out of scope — they're
  combinations of already-covered characters).
- `data/AssetSeeder.kt` seeds Room from these assets only on an empty DB.
  On later launches it instead refreshes `meanings`/`romaji` on existing
  entries (matched by `text`) against the bundled JSON, preserving `id`/
  `correctStreak`/totals — so a future data-generation fix (like the one
  above) reaches already-seeded installs too. Doesn't add newly-added
  vocab to an existing install; that still needs a fresh seed.

## Screens

**Home** (default; also reached via back from Quiz/Settings/My Vocabulary):
per-level stats + enable toggle for every `Level` (KANA, N5-N1, and
`CUSTOM` as "My Vocabulary") in one list, a settings gear icon, and a
single Practice button (`pickRandomActiveEntry`, shared with the
notification path, draws from whichever pools are enabled - JLPT and/or
custom). The `CUSTOM` row additionally has an Edit button opening My
Vocabulary. Settings (quiz mode, notifications, kana hint) are global and
apply to `CUSTOM` identically to JLPT levels - no special-casing needed.

**My Vocabulary** (from Home's Edit button, on the `CUSTOM` row): a list
of the user's own `CUSTOM`-level entries sorted alphabetically by romaji,
with "Add word" pinned above it.
Each row is collapsed by default showing only its romaji (tap to
expand/collapse); expanded, it shows word/romaji/meaning as full-width
labeled `TextField`s plus a Delete button. Every field writes to Room on
each change (no save button, no per-row draft state - simplest thing that
works for a personal table this small); a newly-added row starts expanded
so it's immediately editable. Reuses the existing `Entry`/`EntryDao`
machinery entirely - no new quiz or answer-checking logic, since
`Entry.hasKanji()`/`AnswerService` already work for any level.

**Settings** (from Home's gear icon): multiple-choice toggle, notifications
toggle (default on — off cancels the armed alarm via
`QuizAlarmReceiver.setEnabled`), kana-hint toggle (default off). Back
(system or in-screen arrow) returns to Home.

**Quiz** (from a notification tap or Home's Practice button): entry
display, stage 1 (if it has kanji) + stage 2 as described in "Answering",
then feedback + Next. No stage labels — the two-stage flow is conveyed by
layout (stage 2 dims until stage 1 passes) rather than text. The kana-hint
reveal is a "Hint" button next to stage 1's Submit (only shown when the
Settings toggle is on and there's a kanji reading to hint); revealed kana
renders below the entry text. `Modifier.imePadding()` + `adjustResize`
keeps the keyboard clear of Submit. System back returns to Home.

## Navigation

Three bits of state in `MainActivity`: `quizEntryId: Long?` (Quiz screen,
takes priority), `showSettings: Boolean`, `showMyVocabulary: Boolean`. No
Compose Navigation/`NavHost` — 4 screens, no back-stack complexity beyond
"return to Home", a route graph would be ceremony.

## Notifications

- Master toggle in Settings (default on); off cancels the armed alarm
  outright, not just skips the post.
- Random interval 20–90 min, clamped into an 8am–10pm active-hours window
  (`notification/NotificationScheduling.kt`, pure + unit-tested).
- `POST_NOTIFICATIONS` requested on launch; silently no-ops if denied.
- 1:4 quiz:reveal ratio per fire (`Random.nextInt(5) == 0` → quiz).
- `notification/QuizAlarmReceiver`: `AlarmManager.setExactAndAllowWhileIdle`/
  `setAndAllowWhileIdle`, **not** WorkManager — WorkManager's JobScheduler
  backend gets deferred indefinitely by Doze once the app's been unopened
  a while (confirmed broken on a real device: no notifications until the
  app reopened, then the overdue one fired immediately). Needs
  `SCHEDULE_EXACT_ALARM` (Android 12+; falls back to the inexact variant
  if not granted — `MainActivity` prompts for it on launch). Next-trigger
  time persisted in `SharedPreferences` so `ensureScheduled` (app start +
  `BOOT_COMPLETED`, since alarms don't survive reboot) re-arms the same
  pending time rather than resetting the countdown.

## Tech stack

- `targetSdk 36` (Android 16), `minSdk 33` (floor of the notification-
  permission model this app relies on).
- Package: `io.github.mich8bsp.nihongovocab`. Repo:
  https://github.com/mich8bsp/nihongo-vocab (public).
- Kotlin + Jetpack Compose, Room (schema v2 — `MIGRATION_1_2` added
  `Entry.romaji`), AlarmManager for scheduling. No backend, accounts, or
  sync.

## App icon

Adaptive icon (vector-only, no legacy PNGs needed above API 26):
vermillion torii gate (`#C8332B`) on a cream background (`#FDF6EC`).

## Out of scope for v1

- Accounts / multi-device sync
- Configurable active-hours window (hardcoded default)
