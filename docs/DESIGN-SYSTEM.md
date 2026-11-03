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
| Focus | `AppCard`, `PrimaryButton` | immersive session view and idle view use their own space palette on purpose; align text colours and chips |
| Progress | `AppCard`, `BarChart`, `ProgressBar`, `SectionHeader` | Day/Week/Month tabs, achievements list, heatmap colours |
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
