# Implementation TODO

Ordered so each part is runnable/testable before moving to the next.
See DESIGN.md for the decisions behind these.

## 1. Project setup
- [x] Android Gradle project skeleton (Kotlin + Jetpack Compose), minSdk 33 /
      targetSdk 36, hand-written (no Android Studio available in this
      environment). Repo: https://github.com/mich8bsp/nihongo-vocab
- [x] Dependencies wired in `app/build.gradle.kts`: Room 2.8.4 (+ KSP),
      WorkManager 2.11.2, Navigation Compose 2.8.5.
- [ ] **Open in Android Studio and let it generate the Gradle wrapper** —
      this environment has no local JDK/Gradle, so `gradlew`/wrapper jar
      were never generated and the project has not been build-verified.
      First sync in Android Studio is the real correctness check; expect
      possible minor version bumps it suggests (AGP 9.1.1, Kotlin 2.2.20,
      Compose BOM 2026.04.01 were picked from web search, not compiled
      locally).

## 2. Data layer
- [ ] Room entities: `Entry`, `PoolState` (fields per DESIGN.md).
- [ ] DAOs: query active entries by enabled pools, update streak/counters,
      check pool-complete, toggle pool enabled state.
- [ ] Pull vocab/kanji JSON from Bluskyo/JLPT_Vocabulary (fallback:
      elzup/jlpt-word-list), reshape into `Entry` schema via one-off script
      (split `meaning` on `;`/`,` into `meanings` list). Hand-author kana
      (hiragana+katakana) JSON directly — no external source needed.
      Bundle both as assets. See DESIGN.md "Vocabulary data source".
- [ ] One-time seed: on first launch, populate Room from the bundled JSON if
      empty. Seed `PoolState` with KANA and N5 enabled, N4–N1 disabled.

## 3. Core answer logic
- [ ] Function: check free-text answer against `meanings` (case-insensitive).
- [ ] Function: apply an answer result — update `correctStreak`,
      `totalCorrect`/`totalWrong`, then check if the entry's pool just
      became complete → auto-disable it / auto-enable its "next" pool per
      the KANA→(none), N5→N4→N3→N2→N1→(none) chain.
- [ ] Self-check: small test exercising streak reset on wrong answer, streak
      mastery at 3, and pool auto-advance (including kana having no next).

## 4. Quiz screen (UI)
- [ ] Screen taking an entry id, loading the entry.
- [ ] Free-text field + submit button.
- [ ] Feedback state: correct/incorrect + correct answer shown.
- [ ] "Back to Home" navigates to Home screen.

## 5. Home screen (UI)
- [ ] Per-level stats display (correct/wrong/mastered-of-total), read from
      Room.
- [ ] Per-level enable/disable toggle switches, wired to `PoolState`.
- [ ] This is also the app's launch screen (default start destination).

## 6. Notification scheduling
- [ ] Request `POST_NOTIFICATIONS` permission on first launch.
- [ ] WorkManager job: pick a random active entry from enabled pools, post a
      notification with its text, reschedule itself after a random interval
      within the active-hours window (hardcoded default, e.g. 8am–10pm).
- [ ] Handle "no active entries in any enabled pool" — skip firing instead
      of crashing/looping.
- [ ] Notification tap → deep link into Quiz screen with the entry id.

## 7. Navigation wiring
- [ ] Compose Navigation graph: Home ⇄ Quiz.
- [ ] Cold start from notification tap goes straight to Quiz; cold start
      otherwise goes to Home.

## 8. Polish pass
- [ ] Empty/edge states: all pools disabled, all entries everywhere
      mastered, permission denied.
- [ ] Manual pool toggle doesn't fight with an in-flight auto-advance
      (e.g. re-enabling a "just completed" pool works as expected).

## 9. On-device test
- [ ] Install on a real device, verify notification fires, tap flow works,
      streak/mastery/auto-advance behave as designed, stats update.
