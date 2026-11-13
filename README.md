# 🤢 멀미 케어

### 멀미 케어는 차량 주행 중 예고없는 움직임, 흔들림 누적, 실내 환경 때문에 사람마다 다르게 나타나는 멀미를 저주파, 환기, 음성 안내를 통해 해결하며, 그 순간을 기록해 멀미 기준을 학습하는 개인화된 Pleos 차량용 앱입니다.

**알려 주는 데서 멈추지 않습니다.**

<img src="docs/img/explainer.webp" width="100%" alt="멀미 케어 필름 (25초, 가족이 겪는 순서): 굽은 산길 → 뒷좌석 아이 '속이 울렁거려…' → 센터 화면의 몽글이와 '브레이크 부드럽게' 알림, 차가 운전자에게 '뒷좌석이 흔들려요. 브레이크를 부드럽게 밟아 주세요.' → 운전자가 브레이크를 부드럽게 → 송풍구에서 바깥 공기 '바깥 공기로 바꾸고 온도를 조금 낮출게요.' → '그래도 어지러워…' → 멀미 완화 버튼 한 번 → '창문 열고 환기할게요. 먼 곳을 보세요.' → 창문으로 바람, 아이 '이제 좀 살 것 같아' → 멀미 케어">

<sub>앱 화면·음성은 실제 녹화 · 차 안 장면은 Kling 2.5 Turbo AI 생성 · 영상은 저주파 소리를 넣기 전에 만들었습니다</sub>

<img src="docs/img/at_a_glance.svg" width="100%" alt="차 신호로 먼저, 힘들면 버튼 하나 · 멀미하는 동승자 누구나. 왼쪽 ① 차가 먼저(켜 두기만 하면): 멀미 위험 계산(핸들·속도·실내 온도) → 저주파 소리 1분(출발할 때·흔들릴 때) + 바깥 공기·온도 낮춤(외기 순환·22°C) + 운전자 음성 안내(“브레이크 부드럽게”) → 실내가 나아지면 다시 계산. 오른쪽 ② 버튼 하나(그래도 힘들면): 멀미 완화 버튼 → 자동 조치(저주파·환기·음성 안내) → 그 순간을 기록 → 내 멀미 기준 학습 → 링이 내 기준으로">

<img src="docs/img/demo.webp" width="100%" alt="처음에 캐릭터(몽글이)를 크게 보여 준 뒤 오른쪽 위로 이동, 캐릭터를 크게 키워 표시 · 북악스카이웨이 실제 도로 경로를 시뮬레이션으로 재생 (Pleos 에뮬레이터, 2배속, 말랑 3D 친구 몽글이): 캐릭터 표정과 둘레 링이 노랑 → 빨강, 멀미 완화 버튼 → 창문 열고 환기했어요">

<a href="docs/demo-script.md"><img src="docs/img/link_video.svg" height="40" alt="시연 콘티"></a> <a href="https://github.com/toowani/motion-care/releases/latest/download/CRAVE_MotionCare_release.apk"><img src="docs/img/link_apk.svg" height="40" alt="APK 받기 · GitHub Releases"></a> <a href="#심사용-빠른-실행"><img src="docs/img/link_repo.svg" height="40" alt="실행 방법 · 에뮬레이터 4단계"></a>

<img src="docs/img/crave_logo.svg" width="22" align="absmiddle"> **현대자동차그룹 차량용 앱 개발 공모전 (Pleos Connect) · 팀 CRAVE**

[1 문제](#1-문제-멀미는-왜-누구에게) · [2 해결](#2-해결-저주파--환기--음성-안내) · [3 멀미 지수](#3-멀미-지수-차-신호로-흔들림을-계산) · [4 핵심 기술](#4-핵심-기술-차-신호로-때를-잡는-저주파-소리) · [5 구현](#5-구현-방법) · [6 실제 화면](#6-실제-구현) · [7 가치](#7-가치) · [8 확장](#8-확장-가능성) · [9 부록](#9-부록-한계--실행-방법--문서)

---

## 1. 문제: 멀미는 왜, 누구에게

<img src="docs/img/conditions.svg" width="100%" alt="멀미 조건 4가지: ① 예고 없는 움직임(핸들 각도 → 흔들림 계산을 1–2초 앞당김, 방향지시등·다음 회전은 회전 예고용 확장) ② 느린 흔들림 누적(약 0.03–0.25 Hz, 속도·진행 방향 → I·D 계산 → 저주파 소리 1분) ③ 덥고 답답한 실내(실내 온도·내기순환 → 외기 전환·온도 낮춤) ④ 사람마다 다른 기준(멀미 완화 버튼 누른 순간 → 개인 기준선 τ 학습)">

## 2. 해결: 저주파 · 환기 · 음성 안내

### 그래도 힘들면: 멀미 완화 버튼 하나

<img src="docs/img/relief_sequence.svg" width="100%" alt="멀미 완화 버튼(지도·분석·한 화면 모드 어디서나): 1 저주파 100 Hz 1분(AudioTrack, 차 스피커) 2 뒷창문 20%(Vehicle Window) 3 바깥 공기+22°C(Vehicle HVAC) 4 '낮은 소리 틀고 환기할게요. 먼 곳을 보세요.'(TextToSpeech) 5 누른 순간 D → 내 기준선 τ(주행마다 첫 누름만, D ≥ 5일 때, 최근 30개, 차 한 대에 기준 하나) · 창문·공조를 못 쓸 때 대신 하는 일: 창문 불가(거절·뒷창문 없음·4초 무응답) → 화면+음성 안내, 공조 꺼짐 → 그대로, 20초 안 다시 누름 → '환기 중이에요'만">

### 그 순간을 기록해 멀미 기준을 학습

<img src="docs/img/f_tau.png" width="100%" alt="τ = (n D̄ + n_0 τ_0) / (n + n_0) — 개인 기준선 τ 식: 멀미 완화 버튼을 누른 순간 누적량의 평균과 출발값 30을 기록 수에 따라 섞음, 기록 5개에서 반반 · 주행마다 첫 누름만, D ≥ 5일 때, 최근 30개, 차 한 대에 기준 하나">

### 근거

<img src="docs/img/evidence.svg" width="100%" alt="근거: 핵심 100 Hz 소리 1분 → 흔들림 뒤 자세 흔들림·멀미 설문 점수 감소(Gu 외, 2025, 나고야대; 흔들리기 직전 양쪽 귀, 80–85 dBZ, 그네·주행 시뮬레이터·실제 차, 주행 중 재생 효과는 미검증), 1초 전 음성 예고(Kuiper 외, 2020, 확장 ① 예측 음성의 근거, 앱에서는 꺼 둠), 0.5 Hz 미만 옆 흔들림(Turner & Griffin, 1999), 누적량 형태(ISO 2631-1, 수직 기준을 수평에 응용), 바람(D'Amour 외, 2017), 4–13세에서 가장 흔함 → 효과가 가장 클 것으로 봄(Huppert 외, 2019), 뒤쪽 좌석(Turner & Griffin, 1999)">

<img src="docs/img/slide_low_freq.jpg" width="100%" alt="멀미 흔들림은 0.5 Hz 이하: 저주파 = 2초 이상 주기의 출렁임 · ISO 2631-1 멀미 대역 0.1–0.5 Hz, 승차감·건강 0.5–80 Hz, 옆 흔들림 가중 곡선 0.0315–0.25 Hz 평탄 후 0.8 Hz까지 감소, 멀미 케어 0.03–0.25 Hz 그대로, 0.5 Hz까지 점점 줄임 · 버스 승객 3,256명(110시간) 0.5 Hz 미만 옆 흔들림과 메스꺼움이 함께 증가 · 흔들림 실험 120명 약 0.25 Hz까지 평탄 후 감소 · 차종 5종 비교 차종 간 차이 없고 운전 방식·노선에 따라 차이 (ISO 2631-1:1997 · Turner & Griffin 1999 · Donohew & Griffin 2004)">

DOI와 한계: [research/literature.md](research/literature.md)

## 3. 멀미 지수: 차 신호로 흔들림을 계산

<img src="docs/img/slide_sampling_1hz.jpg" width="100%" alt="그래서 1 Hz 차 신호로 계산: 1초 간격 샘플로 0.5 Hz(2초 주기)까지 볼 수 있음(나이퀴스트) = 멀미 대역 · 고주파 관성 센서 없이 추가 장비 0, 차 안에서 계산, 순수 Kotlin 엔진은 SDK와 분리돼 다른 차량으로 이식 가능 · 입력(속도·방향·핸들 각도·실내 온도·내기순환) → 저주파 엔진(가속도 → 저주파 → 지수·누적량 → 멀미 가능성) → 출력(공조·창문·음성 안내)">

<img src="docs/img/slide_four_values.jpg" width="100%" alt="네 값 → 멀미 가능성 P: 지금의 흔들림 I(60초 저주파 RMS), 쌓인 흔들림 D(출발부터 누적, ISO 2631-1 수평 응용), 실내 환경 m(더위·내기순환이면 기준 낮춤), 사람별 기준 τ(완화 버튼 순간의 D로 학습) → P = 1/(1+e^(−(D−τm)/s)), 멀미 가능성 25%·50% 단계(분석 게이지), 링은 차오르는 양">

<img src="docs/img/index_flow.svg" width="100%" alt="멀미 지수 계산 흐름: 속도·진행 방향(핸들 각도로 앞당김) → 가속도 → 느린 흔들림 → 지수 I·누적 D → 판단(환경 계수 m, 개인 기준선 τ) → 흔들림 단계·멀미 가능성 P · 지수 I > 주의 기준 0.9·m (10초 유지) → 음성 안내·외기 전환·22°C, 멀미 완화 버튼 → 창문·바람·음성">

<img src="docs/img/two_values.svg" width="100%" alt="두 가지 값: 흔들림 지수 I(최근 60초, 잔잔·흔들림·흔들림 많음, 경계 0.9·m·1.5·m, 10초 넘게 이어지면 전환, 캐릭터 표정·링 색)와 멀미 가능성 P(낮음·보통·높음, 경계 25%·50%, 링이 차오르는 양·분석 게이지) · 같은 세 가지 색(초록 = 낮음, 노랑 = 보통, 빨강 = 높음), 기준은 둘: 표정·링 색은 I 단계, 게이지 색은 P 단계">

<img src="docs/img/f_dose.png" width="100%" alt="D = ( Σ_t ( w_X a_x^2 + w_Y a_y^2 ) Δt )^{1/2} — 누적 흔들림 D 식: 앞뒤·옆 가속도 제곱에 가중치를 곱해 1초마다 더한 값의 제곱근, 길이가 다른 주행은 D/√t로 비교">

<sub>가중치 $w_X$ · $w_Y$는 주행 중 바뀌지 않는 상수이고 지금은 둘 다 $1$입니다 (앞뒤 · 옆 흔들림을 같게 셈). 동승 데이터가 쌓이면 보정할 자리입니다.</sub>

<img src="docs/img/f_cabin.png" width="100%" alt="m = 1 − γ_T max(0, T − 22) − γ_V min(1, t_recirc / 3600) — 환경 계수 m 식: 실내 온도가 22°C를 넘고 내기순환이 길수록 m이 줄어 같은 흔들림에도 더 일찍 위험">

<img src="docs/img/f_prob.png" width="100%" alt="P = 1 / (1 + e^{−(D − τ m)/s}) — 멀미 가능성 P 식: 누적 흔들림 D가 개인 기준선 τ·m을 넘을수록 S자로 커지고 25%·50%에서 분석 게이지 단계가 바뀜 (알림 시점은 바꾸지 않음)">

<sub>$P$는 확률이 아니라 보정 전 점수입니다.</sub>

<details>
<summary>전체 수식 (가속도 · 핸들 각도 · 흔들림 지수 · 알림 기준선 · 예고 시점)</summary>

<img src="docs/img/f_accel.png" width="100%" alt="a_x = Δv/Δt, a_y = v ω — 가속도 식: 앞뒤 가속도는 1초 동안의 속도 변화, 옆 가속도는 속도 × 회전 속도">

<img src="docs/img/f_yaw.png" width="100%" alt="ω = v tan(δ_sw / r) / L — 핸들 각도로 회전 속도를 미리 예측하는 식: GPS 방향보다 1–2초 빠름, 조향비 14.5·축간거리 2.9 m">

<img src="docs/img/steering_lead.svg" width="100%" alt="개념도: 핸들 각도 δ_sw가 먼저 움직이고, 핸들 각도로 예측한 회전 속도 ω가 바로 따라가며, GPS 방향은 1–2초 늦음 · k = 0.2로 GPS 쪽으로 당김">

<img src="docs/img/band_filter.svg" width="100%" alt="0.03 Hz 미만(꾸준한 가속·긴 커브)은 고역 통과 필터(0.0315 Hz)로 제거, 0.03–0.25 Hz는 그대로 누적 흔들림 D(주행 전체)·흔들림 지수 I(최근 60초)로, 0.25–0.5 Hz는 점점 줄여(0.5 Hz에서 약 1/4, 옆 흔들림 멀미 가중 곡선과 같은 모양), 0.5 Hz 초과는 1초 샘플이라 담기지 않음">

<img src="docs/img/freq_response.svg" width="100%" alt="엔진 주파수 응답(옆 흔들림 사인파): 지금 엔진은 0.0315 Hz에서 -3 dB, 0.05–0.25 Hz 평탄(+0.5 / -1.0 dB), 0.5 Hz에서 -11.8 dB로 문헌 옆 흔들림 멀미 가중(Donohew &amp; Griffin 2004, 0.0315–0.25 Hz 평탄 후 감소)과 겹침 · 예전 엔진은 0.1–0.25 Hz에서 3–9 dB 낮았음">

<img src="docs/img/f_index.png" width="100%" alt="I = sqrt( (1/60) Σ_{최근 60초} ( w_X a_x^2 + w_Y a_y^2 ) ) — 흔들림 지수 I 식: 최근 60초 제곱평균제곱근, 0.03–0.25 Hz 중심(멀미 가중)으로 흔들림을 남기고 D는 같은 값을 출발부터 누적">

<img src="docs/img/f_thresholds.png" width="100%" alt="θ_caution = 0.9 m, θ_high = 1.5 m, m ≥ 0.6 — 알림 기준선 식: 주의 0.9·m, 높음 1.5·m, 10초 넘게 이어지면 단계 전환; 덥고 답답해 m이 줄면 같은 흔들림 지수도 더 높은 단계로">

<img src="docs/img/f_cue.png" width="100%" alt="d_cue = v (t_say + t_prepare) — 예고 거리 식: 속도 × (말하는 시간 2초 + 대비할 시간 3초), 엔진에만 있고 앱 화면·소리에서는 꺼 둠">

모든 상수: [`shared/params.json`](shared/params.json) · 저주파 소리 상수: [`LowTone.kt`](android/app/src/main/kotlin/ai/motioncare/app/LowTone.kt)

</details>

## 4. 핵심 기술: 차 신호로 때를 잡는 저주파 소리

<img src="docs/img/low_tone.svg" width="100%" alt="핵심 기술: 차 신호로 때를 잡는 저주파 소리. 왼쪽 나고야대 연구(Gu 외, 2025, Environ Health Prev Med 30:22): 100 Hz 순음 · 1분 · 양쪽 귀 · 80–85 dBZ를 흔들리기 직전에 들려주면 그네·주행 시뮬레이터·실제 차에서 자세 흔들림 감소, 시뮬레이터에서 심박 변이 개선, 실제 차에서 멀미 설문(MSAQ) 점수 감소, 생쥐에서 효과 120분 이상. 오른쪽 멀미 케어가 소리를 트는 때: 출발할 때(속도 3 km/h를 처음 넘는 순간), 흔들림이 쌓일 때(흔들림 지수 I가 주의 기준을 10초 넘게 넘으면, 외기·음성 안내와 함께, 5분 간격), 멀미 완화 버튼(창문 환기·음성 안내와 함께). 한계: 주행 중 재생 효과는 연구가 확인하지 않음, 귀 위치 소리 크기는 차종별 보정 필요, 특허는 미국·유럽·일본·중국 등록, 한국 출원 없음">

<img src="docs/img/paper_fig6_vehicle.png" width="100%" alt="Gu 외(2025) Fig. 6, 실제 차 실험: (A) 머리받침에 단 스피커 2개를 양쪽 귀에서 10 cm 거리에 두고 100 Hz 소리를 들려줌 (B) 소리 없음 1분 또는 100 Hz 1분 → 차 3분 주행 → 자세 흔들림 측정과 멀미 설문(MSAQ), 같은 사람이 두 조건을 120분 넘게 쉬고 모두 받음 (C) 자세 흔들림 증가가 100 Hz 쪽에서 작음 (D) MSAQ 총점·위장·중추·졸림 관련 점수가 100 Hz 쪽에서 낮음, 말초는 차이 없음, n=10">

<sub>논문 그림: 실제 차 실험 (Gu 외, 2025, Fig. 6, n=10). (A) 머리받침 스피커로 양쪽 귀에 100 Hz · (B) 소리 1분 뒤 3분 주행, 같은 사람이 소리 있음 · 없음을 모두 받음 · (C) 자세 흔들림 증가가 줄어듦 · (D) 멀미 설문(MSAQ) 총점이 낮아짐. 출처: Environ Health Prev Med 2025;30:22, [doi:10.1265/ehpm.24-00247](https://doi.org/10.1265/ehpm.24-00247), [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) · 원문 그림을 잘라 씀, 내용은 바꾸지 않음.</sub>

<sub>한계: 연구는 흔들리기 **전** 재생만 확인했고, 흔들리는 **중** 재생 효과는 저자도 추가 연구가 필요하다고 썼습니다. 그래서 흔들림이 쌓일 때 · 버튼을 누를 때의 재생은 동승 조사로 확인할 항목입니다. </sub>

<sub>특허 (2026-11-13 확인): 나고야대는 '50–140 Hz · 70–85 dB 소리로 이석을 자극하는 장치'를 미국(US12023222B2)·유럽(EP3804672B1)·일본(JP7191400B2)·중국(CN112243368B)에 등록했고, DENSO가 이 특허의 라이선스 옵션 비용을 냈습니다(논문 이해관계 공개). Google Patents 패밀리 기준 한국 출원은 없고 PCT 국내 진입 기한(2020년 말)도 지나, 국내 사용은 이 특허에 걸리지 않는 것으로 봅니다(KIPRIS 직접 조회와 변리사 확인은 전). 해외 판매 차량에 넣으려면 라이선스가 필요합니다. 주파수가 시간에 따라 바뀌는 소리를 다룬 후속 출원(US20260007850A1·EP4555983A1, 심사 중)은 고정 100 Hz인 멀미 케어와 청구항 1이 다릅니다. sound spice®는 상표로 표기되며, 앱은 이 이름을 쓰지 않습니다.</sub>

## 5. 구현 방법

<img src="docs/img/sdk_map.svg" width="100%" alt="읽기(1초마다): Vehicle SteeringWheel(CAR_DRIVE)·TurnSignal·HVAC, 위치(GNSS)·CarPropertyManager, NaviHelper(TBT, DrivingInfo) → 계산: SDK와 분리된 순수 Kotlin 엔진(I·D·m·τ·P) → 행동: AudioTrack·차 스피커(저주파 100 Hz 1분, USAGE_MEDIA), Vehicle HVAC(외기·22°C), Vehicle Window(뒷창문 20%, CONTROL_CAR_WINDOWS), TextToSpeech(HYBRID), SpeechToText(확장) · 닫힌 고리: 바뀐 실내 온도·환기가 다시 계산으로">

<img src="docs/img/architecture.svg" width="100%" alt="앱 구조: Pleos 차량 신호 읽기 → 주행 지킴이(앱을 닫아도 뒤에서 계속 계산) ↔ 계산 엔진(시뮬레이터와 같은 계산) → 공기·창문·음성, 화면은 볼 때만, 알림 카드는 알림 센터에만(지도 위 표시는 Pleos 협의)">

### 실차 없이 3단계 점검

<img src="docs/img/validation_ladder.svg" width="100%" alt="3단계 점검 (계산 동작 · 시뮬레이션 현실성 · SDK 연결) · 멀미 예측 정확도 검증은 아님: ① 계산 동작: 실제 도로 시나리오 시뮬레이션 점검 10/10 통과(도로 4곳 · 주행 6회) ② 시뮬레이션 현실성: 실제 주행 데이터 비교(comma2k19) 가속도 0.97배(기준 0.5–2배, 고속도로 45초) ③ SDK 연결: Pleos 에뮬레이터(실제 SDK) 9가지 기능 확인, 창문은 명령 수락까지 (v2.1 · API 34)">

<sub>검증은 시뮬레이션 · 에뮬레이터까지입니다. 실제 동승자가 탄 주행 데이터는 아직 없어, 멀미 가능성의 정확도는 확인 전입니다 · 다음 단계: [동승 조사](docs/ride-study.md)</sub>

### 도로 4곳 · 주행 6회 (OpenStreetMap)

<img src="docs/img/scenario_cards.svg" width="100%" alt="실제 도로 4곳: 도심 3.7 km(회전 4·신호 정지 6, 부드럽게·거칠게), 굽은 길 5.2 km(커브 52곳, 부드럽게·거칠게), 고속도로 17.0 km(13.0분), 나들이길 37.4 km(34.7분) → 1초 간격 가상 센서 값(속도·진행 방향·실내 온도) · 핸들 각도는 시나리오에 없음(에뮬레이터에서만 확인)">

---

## 6. 실제 구현

<img src="docs/img/real_or_sim.svg" width="100%" alt="Pleos Connect Emulator v2.1(Android Automotive API 34)에서 실제 실행 · 실 차량 연동 Pleos SDK: 공조·창문·음성 명령, 방향지시등·핸들 각도 / 시뮬레이션(실제 도로 경로 재생): 속도·진행 방향·경로·회전 안내 카드·도착 시간·실내 온도(모의 실내) · 차 신호 조작은 Pleos 제공 Virtual Console">

### ① 고르는 아이콘 테마

<img src="docs/img/gif_themes.webp" width="100%" alt="아이콘 테마 4종이 숨쉬기·눈 깜빡임으로 움직이는 모습, 단계 색은 초록·노랑·빨강">

<img src="docs/img/theme_names.svg" width="100%" alt="아이콘 테마 4종: 깔끔한 기본(신호등형 표정), 말랑 3D 친구(몽글이), 애니메이션 친구(부릉이), 가상 아이돌 콜라보(루미, 예시 · IP 계약 없음) · 첫 실행 때 고름 · 주행 중 변경 잠금, 그림·말투만 다름, 계산·안내는 같음">

### ② 지도 탭 (기본 화면)

<img src="docs/img/ui_r6_map.jpg" width="100%" alt="지도 탭: 회전 안내 카드, 앞으로 갈 파란 경로, 오른쪽 위 멀미 가능성 링을 두른 캐릭터(몽글이, 노랑), 위쪽 주황 멀미 완화 버튼">

<img src="docs/img/map_elements.svg" width="100%" alt="지도 탭 요소: 진행 방향 위 지도, 앞으로 갈 길만 그리는 파란 경로, 회전 안내 카드·도착 바, 캐릭터 둘레 링(길이 = 멀미 가능성, 12시부터 시계 방향, 색 = 캐릭터 표정 단계, 흔들림 지수 I 기준)">

### ③ 멀미 완화 버튼: 한 번 누르면 바로

<img src="docs/img/ui_r6_relief.jpg" width="100%" alt="멀미 완화 버튼을 누른 뒤: 지도 위 '창문 열고 환기했어요', 몽글이 표정과 링은 빨강">

<img src="docs/img/relief_sequence.svg" width="100%" alt="멀미 완화 버튼(지도·분석·한 화면 모드 어디서나): 1 저주파 100 Hz 1분(AudioTrack, 차 스피커) 2 뒷창문 20%(Vehicle Window) 3 바깥 공기+22°C(Vehicle HVAC) 4 '낮은 소리 틀고 환기할게요. 먼 곳을 보세요.'(TextToSpeech) 5 누른 순간 D → 내 기준선 τ(주행마다 첫 누름만, D ≥ 5일 때, 최근 30개, 차 한 대에 기준 하나) · 창문·공조를 못 쓸 때 대신 하는 일: 창문 불가(거절·뒷창문 없음·4초 무응답) → 화면+음성 안내, 공조 꺼짐 → 그대로, 20초 안 다시 누름 → '환기 중이에요'만">

<sub>저주파 소리 · 창문 · 공조 · 음성이 함께 움직입니다. 순서와 못 쓸 때의 대응은 [2절](#그래도-힘들면-멀미-완화-버튼-하나).</sub>

### ④ 흔들림이 커지면 차가 소리와 공기를 바꿈

<img src="docs/img/closed_loop.svg" width="100%" alt="닫힌 고리 동작 확인(조치 → 실내 값 → 다시 계산, 멀미 감소 효과가 아님 · 시뮬레이션 실내 모델): 굽은 길(26°C, 내기순환 출발)에서 19초에 외기 전환·22°C, 환경 계수 m 0.84 → 0.99, 실내 온도 26 → 22°C / 앱 없으면 m 0.825에 머묾">

### ⑤ 분석 탭

<img src="docs/img/ui_r6_analysis_callouts.jpg" width="100%" alt="분석 탭: 1 멀미 가능성 게이지, 2 이번 주행 타일, 3 그래프 전환(흔들림 지수 ↔ 멀미 가능성), 4 그래프와 주의선·높음선, 5 멀미 완화 동작 제안(창밖 보기·화면 짧게·바람 쐬기·가볍게 먹기), 6 멀미 완화 버튼, 7 시연 패널(시연용, 실제 제품에는 없음)">

### ⑥ 내비와 함께

<img src="docs/img/gif_compact.webp" width="100%" alt="Pleos 화면 나누기 손잡이를 끌어 실제 Pleos 내비 옆 한 화면 모드로: 표정 카드, 멀미 가능성 % 알약, 멀미 완화 버튼">

<img src="docs/img/nav_front.svg" width="100%" alt="Pleos 내비가 앞 화면이어도 주행 지킴이가 뒤에서 계산·공조·음성 안내를 계속 · 지도 위 알림 카드는 Pleos 홈이 시스템 앱 알림만 띄워 Pleos 협의">

**한 화면 모드** · 앱 폭 900 dp 미만

### ⑦ 설정 한 곳에서

<img src="docs/img/ui_r6_settings_callouts.jpg" width="100%" alt="설정: 1 아이콘 테마 4종(주행 중 변경 잠금), 2 소리 안내, 3 공조 자동 조절(바깥 공기·22°C), 4 시연 패널(시연용, 실제 제품에는 없음)">

<sub>설정에는 "저주파 소리" 켜기 · 끄기가 "소리 안내"와 "공조 자동 조절" 사이에 더 있습니다 (위 화면은 추가 전 녹화).</sub>

### 검증 결과

<img src="docs/img/scenario_bars.svg" width="100%" alt="도로 4곳 · 주행 6회의 평균 흔들림 지수(막대)와 최댓값(세로선), 알림 켜진 시간: 고속도로 0.36(0 / 13분), 도심 부드럽게 0.68(1.6 / 8.8분), 도심 거칠게 1.07(6.7 / 8.2분), 나들이길 0.76(9.7 / 34.7분), 굽은 길 부드럽게 1.65(7.5 / 7.9분), 굽은 길 거칠게 2.58(5.9 / 6.2분) · 굽은 길은 길 모양(커브 52곳)이 커서 부드럽게 달려도 높음 · 주의 0.9 · 높음 1.5는 시뮬레이션에 맞춘 출발값(문헌값 아님)">

<sub>굽은 길은 부드럽게 달려도 지수가 높습니다. 운전 방식보다 길 모양이 크게 작용하기 때문입니다. 주의 $0.9$ · 높음 $1.5$는 문헌값이 아니라 시뮬레이션에 맞춘 출발값이고, 동승 조사로 보정합니다.</sub>

<img src="docs/img/timeline_winding.svg" width="100%" alt="굽은 길 거칠게: 흔들림 지수, 주의·높음 기준선, 19초 브레이크 안내·공조(주의 단계 진입), 31초 공조 재요청은 엔진 출력만(앱은 5분 안 반복 생략)">

<img src="docs/img/real_vs_sim.svg" width="100%" alt="실제 고속도로(comma2k19)와 시뮬레이션 고속도로 비교, 같은 45초: 앞뒤 가속도 0.97배, 평균 지수 0.42 vs 0.35">

### 에뮬레이터에서 확인한 것

<img src="docs/img/emulator_checks.svg" width="100%" alt="Pleos Connect Emulator v2.1 10가지 점검 중 9가지 확인: 완화 버튼 학습, 공조 닫힌 고리, 공조 꺼짐, 핸들 각도, Pleos 음성, 내비 다음 회전, 내비 앞 화면, 한 화면 모드, 지도 탭 · 창문은 명령 수락까지(뒷창 위치 2), 실제 움직임은 확인 필요">

저주파 소리는 같은 에뮬레이터에서 따로 확인했습니다 (2026-11-13): 굽은 길 재생에서 출발 순간 `AudioTrack`(USAGE_MEDIA, 100 Hz, 8 kHz 모노)이 시작되고 1분 뒤 해제됨, 멀미 완화 버튼을 누르면 다시 시작됨. 실제 소리 크기(dBA)는 실차에서 잴 항목입니다.

고친 버그: [docs/qa.md](docs/qa.md) · 숫자: [docs/validation.md](docs/validation.md)

---

## 7. 가치

### 타깃

<img src="docs/img/target.svg" width="100%" alt="타깃: 멀미하는 동승자를 태우는 운전자 · 승객 46%가 차멀미를 겪음(5개국 4,479명, Schmidt 외, 2020) · 동승자는 다음 움직임을 모름(운전자보다 동승자가 더 멀미함) · 효과는 아이에게 가장 클 것(4–13세가 가장 약함, Huppert 외, 2019) · 같은 엔진으로 택시·통학 차량·자율주행 승객 · 멀미 완화 버튼은 본인이나 동승자가 누름 → 누른 순간이 그 차의 멀미 기준으로 쌓임(지금은 차 한 대에 기준 하나)">

### 왜 폰이 아니라 차여야 하나

<img src="docs/img/phone_vs_car.svg" width="100%" alt="폰 앱과 멀미 케어 비교: 저주파 소리(폰은 직접 찾아 틀어야 함, 멀미 케어는 차 신호로 때를 잡아 차 스피커로 1분), 흔들림 먼저 계산(핸들 각도로 1–2초), 실내 온도·환기, 창문 열기, 매 주행 실행(앱을 한 번 열면 화면을 닫아도 계속 · 시동 자동 시작은 Pleos 협의 후)">

### 다른 서비스와 비교

<img src="docs/img/market_compare.svg" width="100%" alt="기능 비교(저주파 소리 = 핵심 기술, 차 신호, 미리 계산, 공기·바람 = 조치, 장비 없음, 사람별 기준 = 개인 학습, 상태): Apple Motion Cues·Google Motion Assist·Citroën Seetroën·현대차 스무스 모드(양산), 현대모비스(시범), JLR·VW·Bentley(연구), holoride(파산), Mercedes·BMW(특허), DENSO·나고야대(100 Hz 소리 연구, 저주파 소리만 있음), 멀미 Zero(현대차·기아 2026 아이디어 페스티벌 최우수상, 임직원 팀, 2026-10-19 공개, OTA 차량 기능: 차 신호·미리 보여 주기·장비 없음, 공기·바람과 사람별 기준은 공개 자료 기준 보도에 없음), 멀미 케어(여섯 가지 모두, 시제품 · 미리 계산은 핸들 각도로 1–2초이고 화면 예고는 확장, 사람별 기준은 멀미 가능성 표시에 반영) · 겹침: 차 신호·움직임 미리 보여 주기·하드웨어 추가 없음 · 다름: 공조·창문을 실제로 조작하는 앱, 버튼 기록 기반 기준, Pleos 앱 마켓 배포">

<sub>저주파 소리 열은 이번에 넣었습니다. 같은 100 Hz 소리를 차에 넣는 일은 연구를 후원한 DENSO와 나고야대가 준비 중이라는 보도가 있습니다 (sound spice®, 공식 발표는 확인 못 함). 우리 차이는 차 신호로 재생 때를 잡고, 환기 · 음성 안내 · 개인 기준 학습과 한 앱으로 묶어 Pleos 앱 마켓으로 배포한다는 점입니다.</sub>

[기획서 4절](docs/proposal.md) · [research/market.md](research/market.md)

### MAU · Retention · 수익

<img src="docs/img/mau_funnel.svg" width="100%" alt="출시 1년 예상 MAU: Pleos 탑재 차종 2025년 판매(그랜저 + 아반떼, 당시 Pleos 미탑재라 근사치) 151,110대 × 멀미하는 동승자를 태우는 차 46%(승객 차멀미 경험률로 대신함, 가정) = 69,511 × 설치율 10%(가정) = 6,951 × 월 사용 60%(가정) = 4,171 ≈ 4,200명 · 범위: 설치 5% × 월 사용 40%(보수) 약 1,400명 ~ 20% × 70%(낙관) 약 9,700명 · Pleos Connect 2030 적용 목표 차량 2천만 대">

<img src="docs/img/retention_revenue.svg" width="100%" alt="다시 쓰게 하는 고리: 멀미 완화 버튼 누름 → 누른 순간 흔들림 기록 → 내 기준선 갱신, 주행마다 한 번 열면 화면을 닫아도 계속, 시동 자동 시작은 Pleos 협의 후(월 사용 60% 가정의 근거) · 수익 모델: 캐릭터·가상 아이돌 IP 테마와 음성 팩 판매, 기본 기능 무료, 루미는 오리지널 예시 · IP 계약 없음">

[기획서 8절](docs/proposal.md)

### 확장 대상

<img src="docs/img/b2b_targets.svg" width="100%" alt="확장 대상: 택시·호출 약 25만 대, 통학 차량 약 4만 대, 자율주행 시범운행지구 42곳, Pleos MiniApp·계기판(Pleos 협의 후)">

[협의 문의 초안](docs/pleos-inquiry.md)

---

## 8. 확장 가능성

<img src="docs/img/slide_vision.jpg" width="100%" alt="승차감은 숫자로, 피드백은 주행으로: 천천히 가 주세요라고 들어 줄 운전자 없음 → 차가 재고·듣고·고쳐야 · 승차감 지수(부드럽게 1.65, 거칠게 2.58, 우리 몫은 누적량 D·개인 기준선) · 피드백 → 즉시 개선 순환(멀미 피드백 → 기준 학습 → 제어 변경 → 다시 측정)">

<img src="docs/img/extension_map.svg" width="100%" alt="같은 엔진(I·D·m·τ·P)에서 확장 4가지: ① 예측 음성 안내(엔진 계산 중, 앱에서 꺼 둠, 근거 Kuiper 외 2020, 동승 실험 뒤 켬) ② 휴게소 추천(제안 시점 3분 30초, 확장 LLM+POI 검색 API) ③ 가족 모드(아이마다 기준선, 엔진 코드 있음 · 앱에서 꺼 둠) ④ 승차감 정량 지표(차종 비교·택시 평가·업계 표준, 제안) · 겹치는 두 카드의 우리 몫: 누적량 D 기반 멀미 가능성 · 개인 기준선 τ · 앱 배포">

숨긴 화면 코드: [`android/app/src/main/kotlin/ai/motioncare/app/later/`](android/app/src/main/kotlin/ai/motioncare/app/later/README.md)

### ① 예측 음성 안내 (회전 예고)

<img src="docs/img/timeline_city.svg" width="100%" alt="엔진 안의 회전 예고: 도심 회전 4곳마다 예고 시점(엔진에서만 계산, 앱에서는 꺼 둠), 신호 대기 뒤 한 번 더 · 25초 · 418초는 회전 예고가 아니라 앱이 실제로 말하는 브레이크 안내">

### ② 휴게소 추천 (LLM 연결로 확장)

<img src="docs/img/rest_stop_flow.svg" width="100%" alt="흔들림 많음이 길게 이어지면 → 엔진이 제안 시점 계산(굽은 길 거칠게 3분 30초) → 확장: LLM에 남은 경로·멀미 가능성 P·동승자 → 들를 곳 한 줄 추천 → NaviHelper 경유지 · 질문 코드는 보관, POI 검색 API 필요">

### ③ 개인 맞춤 · 아이별 (가족 모드)

<img src="docs/img/family.svg" width="100%" alt="같은 나들이길에서 두 아이의 기준선(민준 τ 24.9, 서연 τ 35.9)과 도달 시점(29.3분, 31.4분)">

**대응 순서를 보여 주는 그림** · 검증 아님

### ④ 승차감 정량 지표

<img src="docs/img/ride_quality_uses.svg" width="100%" alt="승차감 정량 지표: I와 D를 차종 비교, 택시·기사 평가(같은 노선끼리 비교), ISO 2631-1 형태의 업계 표준 지표로 활용">

---

## 9. 부록: 한계 · 실행 방법 · 문서

### 심사용 빠른 실행

<img src="docs/img/quick_run.svg" width="100%" alt="심사용 빠른 실행 4단계: ① 에뮬레이터 켜기(Pleos Connect v2.1, Wi-Fi › AndroidWifi) ② APK 설치(Releases에서 받기, adb install) ③ 앱 실행(Pleos 사용자 10, --user 10) ④ 시연: 굽은 길 · 거칠게 → 멀미 완화">

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

APK는 [Releases](https://github.com/toowani/motion-care/releases/latest/download/CRAVE_MotionCare_release.apk)에서 바로 받을 수 있습니다 (공모전 시뮬레이터용 빌드). 직접 빌드하려면 아래 "키와 서명"의 `android/local.properties`를 채운 뒤 빌드합니다.

### 한계

<img src="docs/img/limits.svg" width="100%" alt="한계와 다음 단계: 정답 데이터 없음(보정: 버튼 기록 + 동승 조사), 실차 시험 불가, 실제 주행 비교 범위, 창문(명령 수락 확인, 실차 동작 확인 필요), 내비가 앞일 때 알림 카드(Pleos 협의), 지도 탭(시스템 앱 전용 API), 핸들 각도 상수, 자동 실행(Pleos 협의)">

동승 조사 안내: [docs/ride-study.md](docs/ride-study.md)

### 앱 아이콘

<p align="center"><img src="docs/img/ui_16_icon.jpg" width="680" alt="Pleos 앱 목록: 내비게이션 · 멀미 케어 · 전화 · 차량 설정이 같은 진회색 사각 + 흰색 글리프 (웃는 자동차 + 잎사귀)"></p>

### 폴더 구조

<img src="docs/img/repo_structure.svg" width="100%" alt="폴더 구조: android/core(판단 엔진), android/app(TripService·Notifier·MainActivity·ui·theme·later), sim, shared, docs, research, data/rides(로컬 전용), apk(release APK 빌드 위치, git 제외 · Releases에 올림), .github/workflows/ci.yml">

### 시뮬레이션과 그래프

```bash
python3 sim/generator.py
python3 sim/prototype.py
python3 sim/freq_response.py      # 엔진 주파수 응답 (--svg: docs/img/freq_response.svg)
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

<img src="docs/img/controls_layout.svg" width="100%" alt="화면 조작: 위쪽 멀미 완화·설정, 머리 줄 탭 지도(기본)·분석(고정), 아래쪽 시연 패널(굽은 길 거칠게·부드럽게, 도심 거칠게, 나들이길, 실차 신호, 정지) · 실차 신호는 앱을 켜면 자동 시작 · 시연 재생은 모두 4배속">

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

<sub>멀미를 실제로 막는지는 동승 조사로 확인이 필요합니다.</sub>
