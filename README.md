# 멀미 케어

**멀미 지수로 높이는 탑승 쾌적함**

> <img src="docs/img/crave_logo.svg" width="28" align="absmiddle"> 현대자동차그룹 차량용 앱 개발 공모전 (Pleos Connect) · 팀 CRAVE

<img src="docs/img/demo.webp" width="100%" alt="북악스카이웨이 실제 도로 경로를 시뮬레이션으로 재생 (에뮬레이터, 3배속): 지도 탭, 캐릭터 둘레가 멀미 가능성 색으로 바뀜, 멀미 완화 버튼 → 창문 열고 바람 넣었어요">

**에뮬레이터 시연** · 실제 도로 경로 재생 · 3배속

<a href="docs/demo-script.md"><img src="docs/img/link_video.svg" height="36" alt="시연 영상 v3 · 촬영 예정 (콘티 보기)"></a> <a href="https://github.com/toowani/motion-care/releases"><img src="docs/img/link_apk.svg" height="36" alt="APK 받기 · GitHub Releases"></a> <a href="#8-부록-한계--실행-방법--문서"><img src="docs/img/link_repo.svg" height="36" alt="실행 방법 · 에뮬레이터 4단계"></a>

<img src="docs/img/at_a_glance.svg" width="100%" alt="자동: 흔들림 지수 I가 오르면 차가 외기 전환·온도 낮춤, 바뀐 실내가 다시 계산으로 / 버튼: 멀미 완화를 누르면 창문·바람·음성, 그 순간의 D → 나만의 기준선 τ → 멀미 가능성 P">

[심사 기준](#심사-기준) · [1 멀미 조건](#1-우리가-판단한-멀미-조건) · [2 근거](#2-근거) · [3 멀미 지수](#3-멀미-지수-계산) · [4 구현 방법](#4-구현-방법) · [5 실제 구현](#5-실제-구현) · [6 가치](#6-가치) · [7 확장](#7-확장-가능성) · [8 부록](#8-부록-한계--실행-방법--문서)

---

## 심사 기준

<a href="#왜-폰이-아니라-차여야-하나"><img src="docs/img/crit_vehicle.svg" width="100%" alt="차량특화성: 차 신호로 계산하고, 차가 직접 공조·창문을 바꿈 · 핸들 각도로 1–2초 먼저, 외기·22°C 닫힌 고리, 버튼 하나로 뒷창문 20% · §4 §5 §6"></a>

<a href="#1-우리가-판단한-멀미-조건"><img src="docs/img/crit_creativity.svg" width="100%" alt="창의성: 동승자는 다음 움직임을 모름 → 계산·공기·창문·학습을 한 고리로 · 4–13세에서 가장 흔함, 같은 조합의 양산 서비스 없음(조사 범위), 버튼 = 학습 데이터 · §1 §2 §3 §6"></a>

<a href="#4-구현-방법"><img src="docs/img/crit_tech.svg" width="100%" alt="기술 활용성: Vehicle(HVAC·Window·SteeringWheel·TurnSignal)·NaviHelper·TTS · 에뮬레이터 실제 SDK 11항목 점검·9 확인, 파이썬 = Kotlin 2,162개 샘플 일치 · §4 §5"></a>

<a href="#6-가치"><img src="docs/img/crit_business.svg" width="100%" alt="사업성: 아이가 있는 가족 운전자 · 출시 1년 MAU 약 1,800명 · Retention: 누를수록 내 기준 정확, 무료 + IP 테마·음성 팩, 택시 24만·통학 4만 대 · §6 §7"></a>

<a href="#멀미-케어"><img src="docs/img/crit_bonus.svg" width="100%" alt="가산점 개발 산출물: 에뮬레이터 실제 실행 시연 + 저장소 + 서명된 APK · 시연 애니메이션·영상 v3 예정, APK: GitHub Releases, CI 점검·테스트·빌드 · 맨 위 §8"></a>

---

## 1. 우리가 판단한 멀미 조건

<a href="#심사-기준"><img src="docs/img/badge_creativity.svg" height="36" alt="심사 기준: 창의성"></a>

<img src="docs/img/conditions.svg" width="100%" alt="멀미 조건 4가지: ① 예고 없는 움직임(핸들 각도·방향지시등·다음 회전 → 1–2초 먼저 계산) ② 느린 흔들림 누적(속도·진행 방향 → 흔들림 지수 I·누적량 D) ③ 덥고 답답한 실내(실내 온도·내기순환 → 외기 전환·온도 낮춤) ④ 사람마다 다른 기준(멀미 완화 버튼 누른 순간 → 개인 기준선 τ 학습)">

<img src="docs/img/who_gets_sick.svg" width="100%" alt="운전자는 다음 움직임을 알고 뒷좌석 동승자는 모름 · 멀미가 가장 흔한 나이 4–13세 (Huppert 외, 2019)">

## 2. 근거

<a href="#심사-기준"><img src="docs/img/badge_creativity.svg" height="36" alt="심사 기준: 창의성"></a>

<img src="docs/img/evidence.svg" width="100%" alt="조건별 근거: 1초 전 음성 예고(Kuiper 외, 2020), 0.5 Hz 미만 옆 흔들림과 누적량 형태(Turner & Griffin, 1999 · ISO 2631-1), 바람(D'Amour 외, 2017), 4–13세·뒤쪽 좌석(Huppert 외, 2019 · Turner & Griffin, 1999)">

DOI와 한계: [research/literature.md](research/literature.md)

## 3. 멀미 지수 계산

<a href="#심사-기준"><img src="docs/img/badge_creativity.svg" height="36" alt="심사 기준: 창의성"></a> <a href="#심사-기준"><img src="docs/img/badge_tech.svg" height="36" alt="심사 기준: 기술 활용성"></a>

<img src="docs/img/index_flow.svg" width="100%" alt="멀미 지수 계산 흐름: 속도·진행 방향(핸들 각도로 앞당김) → 가속도 → 느린 흔들림 → 지수 I·누적 D → 판단(환경 계수 m, 개인 기준선 τ) → 흔들림 단계·멀미 가능성 P · 지수 > 기준선 × m → 외기 전환·22°C, 멀미 완화 버튼 → 창문·바람·음성">

<img src="docs/img/two_values.svg" width="100%" alt="두 가지 값: 흔들림 지수 I(최근 60초, 잔잔·흔들림·흔들림 많음, 경계 0.9·m·1.5·m)와 멀미 가능성 P(낮음·보통·높음, 경계 25%·50%), 하나의 색 척도">

<img src="docs/img/f_dose.png" width="100%" alt="D = ( Σ_t ( w_X a_x^2 + w_Y a_y^2 ) Δt )^{1/2} — 누적 흔들림 D 식: 앞뒤·옆 가속도 제곱에 가중치를 곱해 1초마다 더한 값의 제곱근, 길이가 다른 주행은 D/√t로 비교">

<img src="docs/img/f_cabin.png" width="100%" alt="m = 1 − γ_T max(0, T − 22) − γ_V min(1, t_recirc / 3600) — 환경 계수 m 식: 실내 온도가 22°C를 넘고 내기순환이 길수록 m이 줄어 같은 흔들림에도 더 일찍 위험">

<img src="docs/img/f_prob.png" width="100%" alt="P = 1 / (1 + e^{−(D − τ m)/s}) — 멀미 가능성 P 식: 누적 흔들림 D가 개인 기준선 τ·m을 넘을수록 S자로 커지고 25%·50%에서 단계가 바뀜">

<details>
<summary>전체 수식 ①–⑥ (가속도 · 핸들 각도 · 흔들림 지수 · 알림 기준선 · 개인 기준선 · 예고 시점)</summary>

<img src="docs/img/f_accel.png" width="100%" alt="a_x = Δv/Δt, a_y = v ω — 가속도 식: 앞뒤 가속도는 1초 동안의 속도 변화, 옆 가속도는 속도 × 회전 속도">

<img src="docs/img/f_yaw.png" width="100%" alt="ω = v tan(δ_sw / r) / L — 핸들 각도로 회전 속도를 미리 예측하는 식: GPS 방향보다 1–2초 빠름, 조향비 14.5·축간거리 2.9 m">

<img src="docs/img/steering_lead.svg" width="100%" alt="개념도: 핸들 각도 δ_sw가 먼저 움직이고, 핸들 각도로 예측한 회전 속도 ω가 바로 따라가며, GPS 방향은 1–2초 늦음 · k = 0.2로 GPS 쪽으로 당김">

<img src="docs/img/band_filter.svg" width="100%" alt="0.1 Hz 미만(꾸준한 가속·긴 커브)은 최근 10초 평균을 빼서 제거, 0.1–0.5 Hz만 남겨 누적 흔들림 D(주행 전체)·흔들림 지수 I(최근 60초)로, 0.5 Hz 초과는 1초 샘플이라 담기지 않음">

<img src="docs/img/f_index.png" width="100%" alt="I = sqrt( (1/60) Σ_{최근 60초} ( w_X a_x^2 + w_Y a_y^2 ) ) — 흔들림 지수 I 식: 최근 60초 제곱평균제곱근, 0.1–0.5 Hz 흔들림만 남기고 D는 같은 값을 출발부터 누적">

<img src="docs/img/f_thresholds.png" width="100%" alt="θ_caution = 0.9 m, θ_high = 1.5 m, m ≥ 0.6 — 알림 기준선 식: 주의 0.9·m, 높음 1.5·m; 덥고 답답해 m이 줄면 같은 흔들림 지수도 더 높은 단계로">

<img src="docs/img/f_tau.png" width="100%" alt="τ = (n D̄ + n_0 τ_0) / (n + n_0) — 개인 기준선 τ 식: 멀미 완화 버튼을 누른 순간 누적량의 평균과 출발값 30을 기록 수에 따라 섞음, 기록 5개에서 반반">

<img src="docs/img/f_cue.png" width="100%" alt="d_cue = v (t_say + t_prepare) — 예고 거리 식: 속도 × (말하는 시간 2초 + 대비할 시간 3초), 엔진에만 있고 앱 화면·소리에서는 꺼 둠">

모든 상수: [`shared/params.json`](shared/params.json)

</details>

## 4. 구현 방법

<a href="#심사-기준"><img src="docs/img/badge_tech.svg" height="36" alt="심사 기준: 기술 활용성"></a> <a href="#심사-기준"><img src="docs/img/badge_vehicle.svg" height="36" alt="심사 기준: 차량특화성"></a>

<img src="docs/img/sdk_map.svg" width="100%" alt="읽기(1초마다): Vehicle SteeringWheel(CAR_DRIVE)·TurnSignal·HVAC, 위치(GNSS)·CarPropertyManager, NaviHelper(TBT, DrivingInfo) → 계산: SDK와 분리된 순수 Kotlin 엔진(I·D·m·τ·P) → 행동: Vehicle HVAC(외기·22°C), Vehicle Window(뒷창문 20%, CONTROL_CAR_WINDOWS), TextToSpeech(HYBRID), SpeechToText(확장) · 닫힌 고리: 바뀐 실내 온도·환기가 다시 계산으로">

<img src="docs/img/architecture.svg" width="100%" alt="앱 구조: Pleos 차량 신호 읽기 → 주행 지킴이(앱을 닫아도 뒤에서 계속 계산) ↔ 계산 엔진(시뮬레이터와 같은 계산) → 공기·창문·음성, 화면은 볼 때만, 내비를 보는 동안은 알림 카드">

### 실차 없이 3단계 검증

<img src="docs/img/validation_ladder.svg" width="100%" alt="실차 없이 3단계 검증: ① 실제 도로 시나리오 시뮬레이션 점검 10/10 통과(도로 4종 · 주행 6개) ② 실제 주행 데이터 비교(comma2k19) 가속도 1.41배(기준 0.5–2배, 고속도로 45초) ③ Pleos 에뮬레이터(실제 SDK) 11항목 점검 · 9 확인(부분 1 · 거절 1, v2.1 · API 34)">

### 도로 4종 · 주행 6개 (OpenStreetMap)

<img src="docs/img/scenario_cards.svg" width="100%" alt="실제 도로 4종: 도심 3.7 km(회전 4·신호 정지 6, 부드럽게·거칠게), 굽은 길 5.2 km(커브 52곳, 부드럽게·거칠게), 고속도로 17.0 km(12.8분), 나들이길 37.4 km(34.5분) → 1초 간격 가상 센서 값">

---

## 5. 실제 구현

<a href="#심사-기준"><img src="docs/img/badge_tech.svg" height="36" alt="심사 기준: 기술 활용성"></a> <a href="#심사-기준"><img src="docs/img/badge_vehicle.svg" height="36" alt="심사 기준: 차량특화성"></a> <a href="#심사-기준"><img src="docs/img/badge_bonus.svg" height="36" alt="심사 기준: 가산점 개발 산출물"></a>

<img src="docs/img/real_or_sim.svg" width="100%" alt="Pleos Connect Emulator v2.1(Android Automotive API 34)에서 실제 실행 · 진짜 Pleos SDK: 공조·창문·음성, 방향지시등·핸들 각도·실내 온도 / 시뮬레이션(실제 도로 경로 재생): 속도·진행 방향·경로·회전 안내 카드·도착 시간 · 차 신호 조작은 Pleos 제공 Virtual Console">

### ① 아이가 고르는 아이콘 테마

<img src="docs/img/gif_themes.webp" width="100%" alt="아이콘 테마 4종이 숨쉬기·눈 깜빡임으로 움직이는 모습, 단계 색은 초록·노랑·빨강">

<img src="docs/img/theme_names.svg" width="100%" alt="아이콘 테마 4종: 깔끔한 기본(신호등형 표정), 말랑 3D 친구(몽글이), 애니메이션 친구(부릉이), 가상 아이돌 콜라보(루미, 예시 · IP 계약 없음) · 첫 실행 때 고름 · 주행 중 변경 잠금, 그림·말투만 다름, 계산·안내는 같음">

### ② 지도 탭 (기본 화면)

<img src="docs/img/ui_r6_map.jpg" width="100%" alt="지도 탭: 회전 안내 카드, 앞으로 갈 파란 경로, 오른쪽 위 멀미 가능성 링을 두른 캐릭터, 위쪽 주황 멀미 완화 버튼">

<img src="docs/img/map_elements.svg" width="100%" alt="지도 탭 요소: 진행 방향 위 지도, 앞으로 갈 길만 그리는 파란 경로, 회전 안내 카드·도착 바, 캐릭터 둘레 링(멀미 가능성, 12시부터 시계 방향, 25%·50% 경계)">

### ③ 멀미 완화 버튼: 한 번 누르면 바로

<img src="docs/img/ui_r6_relief.jpg" width="100%" alt="멀미 완화 버튼을 누른 뒤: 지도 위 '창문 열고 바람 넣었어요', 캐릭터 링은 빨강">

<img src="docs/img/relief_sequence.svg" width="100%" alt="멀미 완화 버튼(지도·분석·한 화면 모드 어디서나): 1 뒷창문 20%(Vehicle Window) 2 바깥 공기+22°C(Vehicle HVAC) 3 '창문 열고 바람 넣을게요. 먼 곳을 보세요.'(TextToSpeech) 4 누른 순간 D → 내 기준선 τ · 안 될 때: 창문 불가 → 화면+음성 안내, 공조 꺼짐 → 그대로, 20초 안 다시 누름 → '바람 넣는 중이에요'만">

### ④ 흔들림이 커지면 차가 공기를 바꿈

<img src="docs/img/closed_loop.svg" width="100%" alt="닫힌 고리: 굽은 길(26°C, 내기순환 출발)에서 21초에 외기 전환·22°C, 환경 계수 m 0.84 → 0.99, 실내 온도 26 → 22°C / 앱 없으면 m 0.825에 머묾">

### ⑤ 분석 탭

<img src="docs/img/ui_r6_analysis_callouts.jpg" width="100%" alt="분석 탭: 1 멀미 가능성 게이지 89% 높음, 2 이번 주행 타일 4개(주행 시간·공기 바꿈·완화 버튼·최고 흔들림), 3 그래프 전환(흔들림 지수 ↔ 멀미 가능성), 4 그래프와 주의선·높음선, 5 멀미 줄이는 습관 타일, 6 멀미 완화 버튼, 7 시연 패널">

### ⑥ 내비와 함께

<img src="docs/img/gif_compact.webp" width="100%" alt="Pleos 화면 나누기 손잡이를 끌어 실제 Pleos 내비 옆 한 화면 모드로: 표정 카드, 멀미 가능성 % 알약, 멀미 완화 버튼">

<img src="docs/img/nav_front.svg" width="100%" alt="Pleos 내비가 앞 화면이어도 주행 지킴이가 뒤에서 계산·공조·음성 안내를 계속 · 지도 위 알림 카드는 Pleos 홈이 시스템 앱 알림만 띄워 Pleos 협의">

**한 화면 모드** · 앱 폭 900 dp 미만

### ⑦ 설정 한 곳에서

<img src="docs/img/ui_r6_settings_callouts.jpg" width="100%" alt="설정: 1 아이콘 테마 4종(주행 중 변경 잠금), 2 소리 안내, 3 공조 자동 조절(바깥 공기·22°C), 4 시연 패널, 5 만든 팀 CRAVE">

### 검증 결과

<img src="docs/img/scenario_bars.svg" width="100%" alt="도로 4종 · 주행 6개의 평균 흔들림 지수(막대)와 최댓값(세로선), 알림 켜진 시간: 고속도로 0.43(0 / 12.8분), 도심 부드럽게 0.62(0 / 8.8분), 도심 거칠게 0.89(5.2 / 8.2분), 나들이길 0.70(8.4 / 34.5분), 굽은 길 부드럽게 1.39(7.3 / 7.9분), 굽은 길 거칠게 2.04(5.8 / 6.2분)">

<img src="docs/img/timeline_winding.svg" width="100%" alt="굽은 길 거칠게: 흔들림 지수, 주의·높음 기준선, 21초 음성 안내·공조, 46초 공조">

<img src="docs/img/real_vs_sim.svg" width="100%" alt="실제 고속도로(comma2k19)와 시뮬레이션 고속도로 비교, 같은 45초: 앞뒤 가속도 1.41배, 평균 지수 0.35 vs 0.27">

### 에뮬레이터에서 확인한 것

<img src="docs/img/emulator_checks.svg" width="100%" alt="Pleos Connect Emulator v2.1에서 11항목 점검: 확인 9(완화 버튼 학습, 공조 닫힌 고리, 공조 꺼짐, 핸들 각도, Pleos 음성, 내비 다음 회전, 내비 앞 화면, 한 화면 모드, 지도 탭), 부분 1(창문 명령), 거절 1(내비에 앱 영역 알리기 → Pleos 협의)">

고친 버그: [docs/qa.md](docs/qa.md) · 숫자: [docs/validation.md](docs/validation.md)

---

## 6. 가치

<a href="#심사-기준"><img src="docs/img/badge_vehicle.svg" height="36" alt="심사 기준: 차량특화성"></a> <a href="#심사-기준"><img src="docs/img/badge_business.svg" height="36" alt="심사 기준: 사업성"></a> <a href="#심사-기준"><img src="docs/img/badge_creativity.svg" height="36" alt="심사 기준: 창의성"></a>

### 왜 폰이 아니라 차여야 하나

<img src="docs/img/phone_vs_car.svg" width="100%" alt="폰 앱과 멀미 케어 비교: 흔들림 먼저 계산, 실내 온도·환기, 창문 열기, 매 주행 실행">

### 다른 서비스와 비교

<img src="docs/img/market_compare.svg" width="100%" alt="기능 비교(차 신호, 미리 계산, 공기·바람, 장비 없음, 사람별 기준, 상태): Apple Motion Cues·Google Motion Assist·Citroën Seetroën·현대차 스무스 모드(양산), 현대모비스(시범), JLR·VW·Bentley(연구), holoride(파산), Mercedes·BMW(특허), 멀미 케어(다섯 가지 모두, 시제품)">

[기획서 4절](docs/proposal.md) · [research/market.md](research/market.md)

### 타깃 · MAU · Retention

<img src="docs/img/mau_funnel.svg" width="100%" alt="타깃과 예상 MAU(막대는 로그 눈금): 탑재 차량 151,110대 × 자녀 가구 20.3% = 30,675 × 설치율 10%(가정) = 3,068 × 월 사용 60%(가정) = 1,841 ≈ 1,800명 · 지금 깔린 차 기준 약 560명 · 2030년 약 2천만 대">

<img src="docs/img/retention_revenue.svg" width="100%" alt="Retention 장치(멀미 완화 버튼 → 누른 순간 D 기록 → 내 기준선 τ 정확해짐, 한 번 켜 두면 뒤에서 계속, MAU 식의 월 사용 60% 가정)와 수익 모델(기본 무료, 캐릭터·가상 아이돌 IP 테마와 음성 팩 판매)">

[기획서 8절](docs/proposal.md)

### 확장 대상

<img src="docs/img/b2b_targets.svg" width="100%" alt="확장 대상: 택시·호출 약 24만 대, 통학 차량 약 4만 대, 자율주행 시범운행지구 42곳, Pleos MiniApp·계기판(Pleos 협의)">

[협의 문의 초안](docs/pleos-inquiry.md)

---

## 7. 확장 가능성

<a href="#심사-기준"><img src="docs/img/badge_business.svg" height="36" alt="심사 기준: 사업성"></a> <a href="#심사-기준"><img src="docs/img/badge_creativity.svg" height="36" alt="심사 기준: 창의성"></a>

<img src="docs/img/extension_map.svg" width="100%" alt="같은 엔진(I·D·m·τ·P)에서 확장 4가지: ① 예측 음성 안내(엔진 계산 중, 앱에서 꺼 둠, 근거 Kuiper 외 2020, 동승 실험 뒤 켬) ② 휴게소 추천(제안 시점 3분 45초, 확장 LLM+POI 검색 API) ③ 가족 모드(아이마다 기준선) ④ 승차감 정량 지표(차종 비교·택시 평가·업계 표준, 제안)">

숨긴 화면 코드: [`android/app/src/main/kotlin/ai/motioncare/app/later/`](android/app/src/main/kotlin/ai/motioncare/app/later/README.md)

### ① 예측 음성 안내 (회전 예고)

<img src="docs/img/timeline_city.svg" width="100%" alt="엔진 안의 회전 예고: 도심 회전 4곳마다 예고 시점, 신호 대기 뒤 한 번 더, 56초 음성 안내 (앱에서는 꺼 둠)">

### ② 휴게소 추천 (LLM 연결로 확장)

<img src="docs/img/rest_stop_flow.svg" width="100%" alt="흔들림 많음이 길게 이어지면 → 엔진이 제안 시점 계산(굽은 길 거칠게 3분 45초) → 확장: LLM에 남은 경로·멀미 가능성 P·동승자 → 들를 곳 한 줄 추천 → NaviHelper 경유지 · 질문 코드는 보관, POI 검색 API 필요">

### ③ 개인 맞춤 · 아이별 (가족 모드)

<img src="docs/img/family.svg" width="100%" alt="같은 나들이길에서 두 아이의 기준선(민준 τ 24.9, 서연 τ 35.9)과 도달 시점(29.3분, 32.6분)">

**대응 순서를 보여 주는 그림** · 검증 아님

### ④ 승차감 정량 지표

<img src="docs/img/ride_quality_uses.svg" width="100%" alt="승차감 정량 지표: I와 D를 차종 비교, 택시·기사 평가, ISO 2631-1 형태의 업계 표준 지표로 활용">

---

## 8. 부록: 한계 · 실행 방법 · 문서

<a href="#심사-기준"><img src="docs/img/badge_bonus.svg" height="36" alt="심사 기준: 가산점 개발 산출물"></a>

### 심사용 빠른 실행

<img src="docs/img/quick_run.svg" width="100%" alt="심사용 빠른 실행 4단계: ① 에뮬레이터 켜기(Pleos Connect v2.1, Wi-Fi › AndroidWifi) ② APK 설치(GitHub Releases, adb install) ③ 앱 실행(Pleos 사용자 10, --user 10) ④ 시연: 굽은 길 · 거칠게 → 멀미 완화">

```bash
nohup ~/Library/Android/sdk/emulator/emulator \
  -avd Pleos_Connect_v2_1 -no-snapshot-load -no-boot-anim -gpu host \
  > /tmp/emu.log 2>&1 < /dev/null & disown
```

```bash
adb install -r CRAVE_MotionCare_release.apk
```

```bash
adb shell am start --user 10 -n ai.motioncare.app/.MainActivity
```

APK: [GitHub Releases](https://github.com/toowani/motion-care/releases)

### 한계

<img src="docs/img/limits.svg" width="100%" alt="한계와 다음 단계: 정답 데이터 없음(보정: 버튼 기록 + 동승 조사), 실차 시험 불가, 실제 주행 비교 범위, 창문(확인 필요), 내비가 앞일 때 알림 카드(Pleos 협의), 지도 탭(시스템 앱 전용 API), 핸들 각도 상수, 자동 실행(Pleos 협의)">

동승 조사 안내: [docs/ride-study.md](docs/ride-study.md)

### 앱 아이콘

<p align="center"><img src="docs/img/ui_16_icon.jpg" width="680" alt="Pleos 앱 목록: 내비게이션 · 멀미 케어 · 전화 · 차량 설정이 같은 진회색 사각 + 흰색 글리프 (웃는 자동차 + 잎사귀)"></p>

**Pleos 시스템 아이콘 스타일** · 웃는 자동차 + 잎사귀 · [팀 CRAVE 로고](docs/img/crave_logo.svg)

### 폴더 구조

<img src="docs/img/repo_structure.svg" width="100%" alt="폴더 구조: android/core(판단 엔진), android/app(TripService·Notifier·MainActivity·ui·theme·later), sim, shared, docs, research, data/rides(로컬 전용), apk(release APK 빌드 위치, git 제외 · GitHub Releases로 배포), .github/workflows/ci.yml">

### 시뮬레이션과 그래프

```bash
python3 sim/generator.py
python3 sim/prototype.py
python3 sim/readme_figures.py
```

### 엔진 테스트와 앱 빌드

<img src="docs/img/engine_parity.svg" width="100%" alt="shared/params.json의 같은 상수를 sim/engine.py(파이썬)와 android/core(Kotlin)가 읽음, 굽은 길·도심·앱 없는 나들이길 2,162개 샘플 일치(오차 2×10⁻⁶ 이하) · 바꿀 때 순서: engine.py 수정 → prototype.py 점검·test_vectors.csv 갱신 → android/core">

```bash
cd android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :core:test :app:lintDebug :app:assembleDebug
```

### 에뮬레이터

<img src="docs/img/controls_layout.svg" width="100%" alt="화면 조작: 위쪽 멀미 완화·설정, 머리 줄 탭 지도(기본)·분석(고정), 아래쪽 시연 패널(굽은 길 거칠게·부드럽게, 도심 거칠게, 나들이길 4배속, 실차 신호, 정지) · 실차 신호는 앱을 켜면 자동 시작">

<details>
<summary>Virtual Console (Pleos 제공 차 신호 도구)</summary>

<p align="center"><img src="docs/img/virtual_console.jpg" width="600" alt="Pleos가 제공하는 가상 차량 도구 Virtual Console (우리가 만든 것 아님): 페달, 기어, 공조, 좌석, 방향지시등 조작"></p>

```bash
adb forward tcp:8088 tcp:8088   # → http://127.0.0.1:8088/
```

</details>

디버그 빌드 옵션 (Pleos 사용자 10, 앱을 먼저 종료)

```bash
adb shell am force-stop --user 10 ai.motioncare.app
adb shell am start --user 10 -n ai.motioncare.app/.MainActivity \
  --es theme toon --ez skip_welcome true \
  --es replay B_winding_rough --ei replay_from 120 --ef replay_speed 2
# 실제 내비 경로: --es route_to "37.3947,127.1112,판교역"
```

### 키와 서명

`android/local.properties` (커밋 금지)

```properties
pleos.clientId=...          # Pleos Playground › My Project
pleos.clientSecret=...
release.storeFile=keystore/motioncare-release.jks
release.storePassword=...
release.keyAlias=motioncare
release.keyPassword=...
apk.teamName=팀명            # → apk/팀명_MotionCare_release.apk
```

```bash
cd android && ./gradlew :app:assembleRelease   # 서명된 APK → apk/ (클라이언트 키 포함, git 제외)
```

### 개발 환경

<img src="docs/img/dev_env.svg" width="100%" alt="개발 환경: Android Studio 2026.2, AGP 9.4.1 · Gradle 9.8.2, Kotlin 2.4 · compileSdk 34, Pleos Vehicle 2.3.2, NaviHelper 3.0.5, TextToSpeech/SpeechToText 3.3.1">

### 문서

[기획서](docs/proposal.md) · [검증](docs/validation.md) · [확인 기록](docs/qa.md) · [제출 체크리스트](docs/submission.md) · [Pleos 문의 초안](docs/pleos-inquiry.md) · [시연 콘티](docs/demo-script.md) · [동승 조사](docs/ride-study.md) · [문헌 근거](research/literature.md) · [경쟁 조사](research/market.md)

### 남은 일

<img src="docs/img/remaining_work.svg" width="100%" alt="남은 일: 실제 동승 데이터 5–10회, 창문 실제 동작 확인, 시연 영상 v3 촬영 · README에 링크, release APK 다시 빌드 · GitHub Releases에 첨부, 기획서 접수 양식 옮기기">
