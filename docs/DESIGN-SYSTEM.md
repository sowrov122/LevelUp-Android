# LEVEL UP design system (Phase 1)

Premium dark productivity-game look. Calm surfaces, restrained borders, no gradients, no neon, no glassmorphism.

## Tokens (`ui/theme/Tokens.kt`)
| Token | Value | Use |
|---|---|---|
| Background | `#140E0A` | screen background |
| Header | `#21140F` | top bar, bottom navigation |
| Card | `#302117` | cards |
| Elevated | `#3A2A20` | dialogs, raised surfaces, disabled buttons |
| Border | `#4A372A` | card outline, progress tracks |
| Lavender | `#62567D` | primary buttons, selected chips, nav indicator |
| ProgressBlue | `#91B8F0` | progress bars/rings, links, checked controls (also the base for purchasable theme colours) |
| RewardGold | `#FFD000` | coins and rewards only |
| TextPrimary | `#F5EBDD` | main text |
| TextSecondary | `#B5A89B` | secondary text |
| Success | `#8FD4A8` | muted mint, completed states |
| Danger | `#E5756A` | errors and penalties, sparingly |

Spacing: 4 / 8 / 12 / 16 / 24 dp. Radii: chip 12, button 14, card 16, dialog 20. Minimum touch target 48 dp.
`DesignTokensTest` checks every text/background pairing against WCAG AA (4.5:1).

Material roles (`ui/theme/Theme.kt`): `primary` = ProgressBlue (or the equipped theme colour), `primaryContainer` and
`secondaryContainer` = Lavender, `secondary` = RewardGold, `tertiary` = Success, `surface` = Card, `surfaceVariant` = Elevated.

## Shared components (`ui/components/DesignSystem.kt`, `Common.kt`)
`AppCard`, `PrimaryButton`, `SecondaryButton`, `LinkButton`, `AppChip`, `IconBadge`, `ScreenHeader`, `SectionHeader`,
`AppTopBar`, `AppBottomBar` (+ `NavItem`), `ProgressBar`, `ProgressRing`, `BarChart`, `StatCard`, `EmptyState`, `ConfirmDialog`.

## Audit of existing screens
| Screen | Reused now | To update in later phases |
|---|---|---|
| App shell (`AppRoot`) | `AppTopBar`, `AppBottomBar`, solid background | none |
| Today | **Phase 2 done**: `AppCard`, `PrimaryButton`/`SecondaryButton`, `ProgressRing`, `ProgressBar`, `ArtImage`, `FlameIcon`; stateless `TodayContent` with tests | none |
| Routine | **Phase 3 done**: timeline schedule, date strip, summary card, category filter chips, status pills, `CategoryIcon`, Customize mode, editor restyled with `AppChip`; stateless `ScheduleView` with tests | none |
| Focus | **Phase 4 done**: night-sky backdrop, animated station scene, `IconTile`, `AppChip`, `AppCard`, `PrimaryButton`; stateless `FocusIdleContent` / `FocusRunningContent` with tests | none |
| Progress | **Phase 5 done**: segmented 7/30 days, period picker, summary tiles, `AxisBarChart`, `HeatmapGrid`, achievement cards with real progress; stateless `ProgressContent` and a pure `buildProgressState` with tests | none |
| Rewards | `AppCard`, tabs via theme chips, gold coin pill | item cards → shared component, buy buttons → `PrimaryButton`, coin pill → badge |
| App Blocker | `AppCard`, `PrimaryButton` | many raw `Button`/`OutlinedButton` → `PrimaryButton`/`SecondaryButton`; app rows |
| Settings | `AppCard` | list-style rows with `IconBadge` |
| Premium | `AppCard` | feature rows |
| Onboarding | theme colours only | uses plain `Button`/`OutlinedTextField`; full redesign |
| Dialogs | `ConfirmDialog` restyled; other `AlertDialog`s pick up Elevated container from the theme | editor and explanation dialogs: shared dialog component |

Purchasable themes were re-based on the new palette: the free default is "Twilight" (ProgressBlue); Ember, Moonlit and Forest are optional accents.

## Icons
Phase 3 added `androidx.compose.material:material-icons-extended` (same Compose family, version managed by the BOM) so category
and navigation icons (book, laptop, dumbbell, leaf, restaurant, checklist, timer, chart, trophy) match the reference. Release builds
shrink unused icons with R8.

## Routine screen decisions
- Daily view is always chronological by start time. Manual order (Customize → move up/down) is the order of the editable routine list;
  "Sort list by time" rewrites it chronologically.
- Only today's tasks can be ticked; past days are history and future days are plans.
- Time labels follow the phone's 12/24-hour setting.
- Filter chips group the six categories: Study = Academic Study, Work = AI Coding + Trading, Health = Fitness, Personal = Walking & Garden, Meal = Meals.

## Focus Mode (Phase 4)
- Station level comes from completed sessions only (`StationProgress`: Lv. 1-6 at 0, 1, 3, 6, 10, 15 sessions). Each level uses one of the six
  station pictures; during a running session the pictures cross-fade with the session's progress.
- The reward preview on the screen uses `ScoringEngine.calculateFocusReward`, the same function that grants the real reward, including the daily cap.
- Rewards are granted exactly once, from one of: finishing in the app, the planned end being reached while the app is open, the end-of-session
  alarm, or the next time the app starts (`finishDueFocusSession`). Paused sessions never become due; cancelled and sub-5-minute sessions earn nothing.
- Distraction Protection text only says what is true: it never claims apps are blocked, and says so when Usage Access is missing.
- Animations (bobbing station, orbiting satellite, twinkling stars) stop when "Reduce motion" is on.

## Progress (Phase 5)
- Every number is built by the pure function `buildProgressState` from stored data only: task history (`DaySummary`), the reward ledger
  (points per day, floored at 0 like Today), completed focus sessions and lifetime totals. Charts re-render whenever any of those change.
- Completion % = tasks completed / tasks scheduled that day. Daily points = ledger points of that day (rewards, bonuses, focus, minus penalties).
- Heat map levels: None, Low 1-25%, Medium 26-75%, High 76-100% of that day's tasks; a focus session lifts an empty day to Low. Future days stay empty.
- 7 days / 30 days and a period picker (this period and the three before it). Focus time and sessions follow the chosen window; streaks are lifetime.
- Achievements now show progress toward a target (tasks, focus hours, streak days, total XP, study hours, level) and grant XP and coins once when completed.
  The "Achievements" XP is part of the ledger, so total XP and the XP Collector milestone include it.
- `ProgressConsistencyTest` (device) rebuilds the screen from a real database and compares it with the ledger, instances and sessions.

## Focus Mode update
- Station levels are driven by total completed focus minutes: Lv.2 5 min, Lv.3 10 min, Lv.4 40 min, Lv.5 100 min, Lv.6 160 min (see `StationProgress`).
- Before a session the station is shown complete; while a session runs it is drawn for "minutes focused so far + this session", so a new
  session starts at the level already earned (for example level 3 after a completed 10-minute session) and keeps growing.
- Minimum focus session is 10 minutes (shorter sessions earn nothing); custom lengths run from 10 to 180 minutes.
