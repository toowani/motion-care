# later/ — parked UI

Features the team hid from the app (round 6 decisions). The code compiles with the app but
nothing on screen uses it. The engine side is untouched: `android/core` and `sim/engine.py`
still plan turn cues and family thresholds, and the sim checks still cover them.

| File | What is parked | Was used by |
|---|---|---|
| `TurnCue.kt` | `TurnCueSound` (chime / theme voice / silent, never over the navigation's own turn speech), `LightCue` (ambient-light turn pulse, restores the driver's setting) | `TripService.act` for `CUE_LEFT` / `CUE_RIGHT` |
| `CueBubble.kt` | map turn bubble (arrow + "70m" → "곧") under the character | `MapPanel.showCue` / `followCue` |
| `FamilyViews.kt` | `FamilyBars` (per-child bars), `familyCard`, `familyChip` + `FamilyIcon` (가족 모드 button), `FamilyProfilesCard` (settings 가족 프로필), `cueStylePicker` (settings 회전 예고 방식) | `HomeView`, `SettingsPanel` |

Still in place outside this folder (logic, not UI):

- `TripService`: family mode (`toggleFamily`, `FamilyMonitor` alert, post-trip "오늘 OO 괜찮았어요?",
  rear-seat suggestion) behind `FAMILY_UI = false`; `reportSick` / `recordSick` (old "멀미 났어").
- `Settings.cueStyle`, `Settings.lightCueOn`; `ProfileStore.load/save` (children).
- `MoodTheme.cueLine` stays in the theme API (each theme's turn sentence); moving it would break
  the theme interface, so it is simply not called.
- `TripUi.onFamily` / `onFamilyProbabilities` are still sent; `MainActivity` ignores them.

## Re-enabling

- Turn cues: in `TripService.act`, replace the early return for cues with
  `TurnCueSound(settings, navi, ::say).play(line)` and `LightCue(vehicle, ::note)` (call `start()`
  in `onCreate`, `release()` in `onDestroy`); add a `CueBubble` under `MapPanel`'s character and
  call `show(left)` on a cue, `follow(s.nextTurnM)` per sample. Add `cueStylePicker` back to settings
  and a cue tile/marker if wanted.
- Family: set `FAMILY_UI = true`, put `familyChip` in `HomeView`'s top bar (toggle → `toggleFamily`,
  repaint with `paintFamilyChip`), show `familyCard(FamilyBars)` in 분석 and feed it from
  `onFamilyProbabilities`, and add `FamilyProfilesCard` to `SettingsPanel`.
