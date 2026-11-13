# later/ — parked UI

<img src="../../../../../../../../../docs/img/doc_small_4.svg" width="100%" alt="Parked UI hidden from the app in rounds 6 and 7, still compiled: TurnCue.kt, CueBubble.kt, RestStopOffer.kt, FamilyViews.kt with what each parks and who used it; engine side (android/core, sim/engine.py) unchanged">

## Still in place

<img src="../../../../../../../../../docs/img/doc_small_5.svg" width="100%" alt="Logic still in place outside this folder: TripService family mode behind FAMILY_UI = false, NaviGuide.addRestStop, Settings and ProfileStore, MoodTheme.cueLine, TripUi onFamily callbacks">

## Re-enabling

<img src="../../../../../../../../../docs/img/doc_small_6.svg" width="100%" alt="Re-enabling steps for turn cues (TurnCueSound, LightCue, CueBubble, cueStylePicker), family mode (FAMILY_UI = true, familyChip, familyCard, FamilyProfilesCard) and the rest-stop offer (RestStopOffer, reset, offer, postDelayed)">

Rest-stop constructor call:

```kotlin
RestStopOffer(navi, ::askYesNo, { f -> pending?.question?.let(f) == true || queued.any { f(it.question) } }, { live?.lat to live?.lon }, ::action)
```
