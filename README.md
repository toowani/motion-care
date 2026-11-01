# 멀미 케어 (가칭) · 동승자 멀미 예방 앱

Pleos 공모전 출품안 ④. 운전자만 아는 다음 움직임(방향지시등·경로)을 동승자에게 미리 알리고, 실내 공기(온도·환기)를 차가 바꿔 멀미를 막습니다. 실차 없이 시뮬레이션으로 검증합니다.

## 폴더

```
research/        STEP 2 문헌 정리 (literature.md, 작성 예정)
sim/
  generator.py   STEP 3 시나리오 생성기: 도로 곡률 → 속도 → 가속도 → CSV
  engine.py      판단 엔진 (기준 구현, Kotlin과 숫자가 같아야 함)
  prototype.py   STEP 4 전 시나리오 실행 + 합리성 점검(G2) + 그래프
  scenarios/     생성된 시나리오 CSV
  out/           summary.csv, 시나리오별 그래프(SVG)
shared/
  params.json        가중치·기준선·환경 계수 (파이썬과 앱이 같이 읽음)
  test_vectors.csv   파이썬이 계산한 정답 → Kotlin 단위 테스트
android/         Android 프로젝트 (Android Studio로 열기)
  core/          순수 Kotlin 엔진 + 테스트 (STEP 5)
  app/           앱: 재생 모드, Pleos 신호, 공조 제어, 음성, 화면 (STEP 6–7)
docs/            feasibility.md (STEP 1 신호 확인 체크리스트)
```

## 실행

시뮬레이션과 점검 (파이썬 3, 외부 라이브러리 없음)

```bash
python3 sim/generator.py
python3 sim/prototype.py
```

Kotlin 엔진 테스트와 APK 빌드 (Android Studio에 들어 있는 JDK 사용)

```bash
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :core:test :app:assembleDebug
```

`app:preBuild`가 `sim/scenarios/*.csv`와 `shared/params.json`을 앱 assets로 복사합니다. 시나리오나 파라미터를 바꾸면 `prototype.py`를 다시 돌려 `test_vectors.csv`를 갱신한 뒤 빌드하세요.

## 테스트 환경

- Android Studio 2026.2, Android SDK 34, AGP 9.4.1, Gradle 9.8.2, Kotlin 2.4
- Pleos Connect Emulator v2.1.0 (API 34, Automotive), Virtual Console
- Pleos Vehicle SDK 2.3.2 (`ai.pleos.playground:Vehicle:2.3.2`)

## 앱 사용

상단 버튼으로 시나리오를 고르면 4배속으로 재생됩니다. 왼쪽은 운전자용 색 점, 오른쪽은 시연용 그래프와 이벤트 기록입니다. "실차 신호 (Pleos)"는 에뮬레이터의 방향지시등·공조·속도·위치를 받습니다.

## 같이 작업하기

1. 저장소를 받고 Android Studio에서 `android/` 폴더를 엽니다. `local.properties`(SDK 경로)는 Android Studio가 자동으로 만듭니다.
2. 에뮬레이터: SDK Manager › SDK Update Sites에 Pleos 이미지 주소를 추가하고 Pleos Connect v2.1.0(API 34)을 설치합니다. 주소와 AVD 설정은 Pleos 개발 문서 "Pleos Connect Emulator 설치 및 실행"을 따릅니다.
3. Virtual Console: 에뮬레이터를 켠 뒤 `adb forward tcp:8088 tcp:8088`, 브라우저에서 http://127.0.0.1:8088/
4. 계산 로직을 바꿀 때는 `sim/engine.py`를 먼저 고치고 `python3 sim/prototype.py`로 점검(G2)과 `test_vectors.csv`를 갱신한 뒤, `android/core`의 Kotlin을 같은 숫자가 나오게 고칩니다 (`./gradlew :core:test`).
5. Pleos 음성 SDK의 클라이언트 ID·시크릿 같은 키는 커밋하지 말고 각자 `android/local.properties`에 둡니다.

## 키와 서명 (각자 `android/local.properties`, 커밋 금지)

```
pleos.clientId=...          # Pleos Playground › My Project
pleos.clientSecret=...
release.storeFile=keystore/motioncare-release.jks
release.storePassword=...
release.keyAlias=motioncare
release.keyPassword=...
apk.teamName=팀명            # → apk/팀명_MotionCare_release.apk
```

`./gradlew :app:assembleRelease`가 서명된 APK를 `apk/`로 복사합니다. 이 APK에는 클라이언트 키가 들어 있어 평소에는 git에서 제외하고, 제출할 때만 `git add -f apk/...`로 올립니다. 키스토어(`android/keystore/`)는 잃어버리면 같은 서명으로 다시 낼 수 없으니 따로 백업하세요.

## 구현된 것

- 재생 모드(시나리오 4배속), 실차 신호(방향지시등·공조·뒷좌석 탑승·NaviHelper 다음 회전)
- 예고 음성, 공조 닫힌 고리(외기·22°C, 내기순환은 차가 알려 주는 영역으로 설정)
- Pleos TTS/STT (HYBRID, 클라이언트 키로 서버 음성 연결, 실패 시 안드로이드 기본 TTS)
- 휴게소 경유지: 음성으로 묻고 "응"이면 NaviHelper `addWaypoint` (휴게소 좌표는 데모용, POI 검색 API 필요)
- 가족 모드: 가장 약한 탑승자 기준 경보, "멀미 났어" 기록, 정차 후 한 질문으로 기준선 학습 (기기 저장)
- 서명된 release APK

## 아직 남은 것

- 에뮬레이터 그래픽 크래시(ColorBuffer) 원인 확인 후 가족 모드·휴게소 흐름 실기 확인
- 속도(`PERF_VEHICLE_SPEED`)와 GPX 위치 재생 확인
- 기획서, 시연 영상, 팀명 확정 후 APK 이름
