# 시연 영상 콘티 (75초, 에뮬레이터 화면 녹화)

목표: "폰으로는 못 하는 장면" 3개를 보여 준다. ① 움직이기 전에 예고 ② 실내 공기를 차가 바꿈 ③ 가족 중 가장 약한 아이 기준으로 미리 대응.

## 준비

```bash
~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2_1 -no-snapshot-load
adb forward tcp:8088 tcp:8088          # Virtual Console: http://127.0.0.1:8088/
adb shell screenrecord --size 1920x1080 --bit-rate 8000000 /sdcard/demo.mp4
adb pull /sdcard/demo.mp4               # 녹화 종료는 Ctrl+C (최대 3분)
```

- 앱 설치: `cd android && ./gradlew :app:installDebug`
- Virtual Console에서 Target Temperature 26°C, Air Recirculation = Recirculate 로 시작 (덥고 탁한 실내)
- 화면 녹화와 별도로 노트북 화면 녹화로 Virtual Console 창을 함께 담으면 "차가 바뀌는 것"이 잘 보임

## 장면

| 시간 | 화면 | 내레이션/자막 |
|---|---|---|
| 0–8초 | 검은 화면에 질문 | "운전자는 왜 멀미를 안 할까?" → "다음 움직임을 알기 때문입니다." |
| 8–20초 | 앱 "실차 신호" → Virtual Console Turn Signal "Blink left" | 차가 먼저 말함: "곧 왼쪽으로 돌아요." 자막: 방향지시등 → 동승자에게 예고 |
| 20–40초 | "B 굽은 길 · 거칠게" 재생, 그래프 상승 | 자막: 차의 가감속·코너링으로 흔들림 누적량 계산 |
| 40–52초 | "멀미 위험" → 음성 안내 → 공조 바 22.0°, 외기 | "바깥 공기로 바꾸고 온도를 조금 낮출게요." 자막: 실내 공기를 차가 바꾸면 기준선 회복 (점선이 올라감) |
| 52–65초 | "가족 모드" → "D 가족 귀성" | "민준에게는 벌써 흔들림이 많아요. 바람 넣고 천천히 갈게요." 자막: 가장 약한 아이 기준으로 먼저 |
| 65–75초 | 정지 → "오늘 민준 괜찮았어요?" → 구조도 한 장 | 자막: 탈수록 우리 가족에게 맞춰집니다 · Pleos Vehicle · NaviHelper · TTS · STT |

## 숏폼(선택, 30초)

"뒷좌석 아이가 멀미하는 순간" 실사 한 컷 → 앱이 회전 전에 말하고 바람을 바꾸는 장면 → "#HMG차량용앱개발공모전". 차량 내외부 이미지 노출은 최소화.
