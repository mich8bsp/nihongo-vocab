# Implementation TODO

Status: v1 core loop shipped and in active use; now in ongoing
feature-request mode. See `DESIGN.md` for the decisions behind all of
this. Full verification detail for each item lives in its commit message,
not here — this file tracks *what's done and what's left*, not the story
of how.

## Remaining

- [ ] Final on-device pass on a real phone specifically for Doze/battery/
      notification-shade behavior an emulator can't reproduce. Largely
      de-risked already — several real-device bug reports below were
      fixed and re-verified — but never formally closed out.

## Shipped

1. Project setup — Kotlin+Compose, Room+KSP, gradle wrapper.
2. Data layer — Room entities/DAOs, vocab seeding from elzup/jlpt-word-list
   + hand-authored kana (`AssetSeeder`).
3. Core answer logic — `AnswerService` streak/mastery/pool-completion.
4. Quiz screen (UI).
5. Home screen (UI) — stats, per-level toggle, Practice button.
6. Notification scheduling — originally WorkManager, replaced with
   AlarmManager (see DESIGN.md "Notifications").
7. Navigation — resolved without a `NavHost`.
8. Polish pass — edge cases (all pools disabled, zero active entries,
   permission denied, manual toggle vs. auto-advance) verified live.
9. Fixed: keyboard covering the Submit button (`imePadding` + `adjustResize`).
10. Fixed: system back on Quiz quit the app instead of returning Home.
11. Added: torii-gate adaptive app icon.
12. Added: romaji shown in correct-answer feedback (schema v2 migration).
13. Replaced redundant "Back to Home" result-screen button with "Next".
14. Added: "Give Up" button.
15. Added: romaji-leniency hint on Submit (typed the reading, not the meaning).
16. Added answer leniency: parenthetical clarifications, leading articles,
    mid-phrase alt words, digit/word numbers.
17. Fixed elzup source data's `;`/`,` meaning-splitting bug (~7% of entries).
18. Fixed: notifications not firing in background (WorkManager → AlarmManager,
    Doze deferral).
19. Added: non-clickable "Reveal" notification type.
20. Added: multiple-choice quiz mode toggle.
21. Added: two-stage quiz (romaji reading gates the meaning check).
22. Added: stage 1 Give Up button + Quiz screen autofocus.
23. Added: Settings screen (multiple-choice toggle moved here) + gear icon
    on Home.
24. Changed: 4:1 reveal:quiz notification ratio (was 1:1).
25. Added: notifications on/off toggle in Settings.
26. Added: kana reading hint toggle + Quiz screen reveal button
    (`data/RomajiToKana.kt`).
27. Fixed: item 17's meaning-split fix never reached installs that had
    already seeded the old broken data (`AssetSeeder` only seeded an empty
    DB). `AssetSeeder` now also refreshes `meanings`/`romaji` on existing
    entries (matched by `text`) against the bundled JSON on every launch,
    preserving `id`/`correctStreak`/totals.
28. Fixed a wrong expectation in `RomajiToKanaTest` (asserted a chōon mark
    `ー` the function was never designed to produce - found while fixing
    item 27, unrelated to it).
29. Fixed: kana-only non-KANA-level entries (e.g. hand-authored kana words
    tagged N5) got a pointless stage 1 with nothing to read (`Entry.hasKanji()`).
    Also dropped the "Stage 1/2" text labels and moved the kana-hint button
    inline next to Submit.
30. Added a new `Level.CUSTOM` pool ("My Vocabulary") listed alongside
    KANA/N5-N1 on Home with the same toggle/stats/Practice treatment
    (incl. notifications); its row has an Edit button to a new My
    Vocabulary screen with an editable word/romaji/meaning table
    (add/edit/delete rows). Also backfills a `pool_state` row for any
    `Level` (e.g. this new `CUSTOM`) missing one on an already-seeded
    install - same class of fix as item 27, but for `PoolState`.
31. Fixed: My Vocabulary fields could be hidden behind the IME while
    editing (`imePadding()` on the `LazyColumn`, `adjustResize` was
    already set).
