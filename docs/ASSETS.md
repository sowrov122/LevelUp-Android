# Illustration assets

28 images supplied for the redesign, optimised to WebP (about 1.7 MB in total, down from about 35 MB of PNG) in
`app/src/main/res/drawable-nodpi/`. Code refers to them only through `ui/theme/Art.kt`.

| Resource | Size | Planned use |
|---|---|---|
| `splash_bg`, `logo_rocket` | 852×1846 opaque, 512 | splash and onboarding |
| `hero_observatory`, `avatar_astronaut` | 1400×613 opaque, 512 | Home banner, profile |
| `station_stage_1` … `station_stage_6` | 768, transparent | Focus session: station grows in six steps (`Art.stationStageIndex`) |
| `shield_lock`, `alert_triangle` | 512 | App blocker, penalty pop-up |
| `item_*` (11 files) | 512 | Reward shop cards (`Art.shopItem(id)`) and the station scene |
| `coin`, `trophy`, `badge_medal`, `levelup_crest`, `crown` | 512 | coins, achievements, level-up, premium |

The supplied file `item_new_planet.png.png` was renamed to `item_new_planet`. Colour themes have no artwork (swatches are drawn).
The assets are imported but not yet shown on any screen; each phase wires in its own pictures.
