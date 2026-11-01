# Level Up — offline routine & focus RPG (native Android, Kotlin)

Native Kotlin + Jetpack Compose (Material 3) port of the web prototype, following
`PRODUCT-SPEC.md`, `TECHNICAL-SPEC.md` and `PLAN.md`. No backend, no account, no AI, **no INTERNET permission**.

> **Honest status:** this code was written without access to an Android SDK/Gradle in the authoring
> environment, so it has **not been compiled or run yet**. Expect to fix a few compile errors on the first
> build. The pure-Kotlin domain logic has unit tests, but they have not been executed either.

## Build
Requirements: Android Studio Ladybug (2024.2) or newer, JDK 17, Android SDK 35.

1. Open this folder in Android Studio and let Gradle sync (it downloads Gradle 8.9 from `gradle-wrapper.properties`;
   if you want a wrapper script, run `gradle wrapper` once).
2. Run the `app` configuration on an emulator/device (Android 8.0+, minSdk 26).
3. Debug APK: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
4. Unit tests: `./gradlew testDebugUnitTest`  ·  Device tests: `./gradlew connectedDebugAndroidTest`

Versions are pinned in `gradle/libs.versions.toml` (AGP 8.7.0, Kotlin 2.0.21, KSP 2.0.21-1.0.25, Compose BOM 2024.10.00, Room 2.6.1).

## What is implemented
| Phase | Status |
|---|---|
| 0 Foundation, theme, navigation | done (uncompiled) |
| 1 Onboarding (name, sleep window → quiet hours, skip), DataStore, reduced-motion | done |
| 2 Room + routine editor (add/edit/delete/enable/reorder/duplicate, weekday masks, overnight, conflict warnings, dated instances that never rewrite completed history) | done |
| 3 Notifications (channel, just-in-time permission with explanation, offsets, snooze, tap → Today highlight, reschedule on boot/time-zone/date/app update/edit, quiet hours) | done |
| 4 Points/XP/levels/streaks, append-only ledger with idempotency keys + transactions, reversals | done (daily caps apply to focus rewards only) |
| 5 Focus timer (persisted, monotonic clock, reboot-safe, cancel = no reward, capped reward) + Cosmic Observatory canvas driven by saved totals | done (no sound/vibration yet) |
| 6 Distraction protection prototype | done (uncompiled): focus-only, UsageStatsManager polling in a foreground service, opt-in app list, never-block essentials, per-launch dedupe + cooldown, emergency pause, interruption history, master off switch. No AccessibilityService, no overlay. |
| 7 Progress (7/30-day charts, 5-week heatmap, category time), achievements, coin shop (themes + observatory items), JSON export | done (no CSV, no monthly view) |
| 8 Premium foundation | done (uncompiled): `EntitlementProvider` abstraction, feature catalogue, Premium screen. **Billing is not implemented**; every user is on the free plan and nothing is locked or for sale. |
| 9 QA / privacy | unit tests, repository-flow and component tests written, CI compiles instrumented tests and runs lint; **manual device tests not run yet** (`docs/MANUAL-TEST-PLAN.md`); privacy summary in `PRIVACY.md` |

## Known limitations / deviations
- Distraction protection (Phase 6): detects selected apps reaching the foreground only while a focus session is RUNNING and
  Usage Access is granted; shows a notification (no blocking overlay). Penalty/cooldown are global settings, not per app.
  It cannot see content inside apps, cannot stop an app from opening, and OEM battery policies may delay or kill the checker.
  The foreground service uses type `specialUse`, which must be declared in Play Console before publishing.
  Database is now version 2 (migration 1→2 adds `blocked_apps` and `interruption_events`).
- Reminders use inexact `setAndAllowWhileIdle` alarms (no exact-alarm permission); they can be minutes late under Doze.
- Time zone follows the device (the setting exists in storage but has no UI).
- Routine is stored as tasks with weekday masks instead of a separate `Routine` table; sleep/break blocks are ordinary tasks.
- Days on which the app was never opened have no task instances, so they do not count toward streaks.
- Difficulty levels from the web prototype and the AI coach were intentionally dropped.
- A focus session is not auto-completed while the app is closed; it finishes when the app is reopened.
- Never claims to detect adult content or reels inside other apps.

## Scoring rules
Task: ≤30 min = 5, ≤60 = 10, ≤120 = 20, longer = 25 (points = XP). All high-priority done: +20. Every task done: +30
(+coins). Focus: 1 XP/min, 1 point per 5 min, 1 coin per 10 min, daily cap 120 min (configurable), sessions under 5 min earn nothing.
Level: XP to advance from level L = 100 + 25·L. Daily points are floored at 0; the ledger keeps the true signed history.

## Testing
- `./gradlew testDebugUnitTest` – scoring, levels, streaks, time/DST/midnight, focus maths, distraction policy, premium rules.
- `./gradlew connectedDebugAndroidTest` (emulator/device) – Room ledger constraints, repository flows (toggle idempotency,
  history preserved after edits/deletes, focus reward once, cancel earns nothing) and component UI tests.
- Manual checklist: `docs/MANUAL-TEST-PLAN.md`. **No manual results have been recorded yet.**
- Logging audit: the app contains no `Log`/`println` calls, so no personal data is written to logcat.
- Not covered by automated tests: full-screen Compose flows (onboarding, create/edit task, permission re-enable), the guard service,
  notification delivery and reboot recovery.
