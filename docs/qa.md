# 에뮬레이터 확인 기록

## 확인 완료 (11.1)

| 항목 | 결과 |
|---|---|
| 재생 모드 · 굽은 길 거칠게 | 지수 상승 → "멀미 위험" → 음성 안내 → 공조 외기·22°C 설정 성공 (차량 공조 바 22.0° 표시) |
| 실차 신호 · 방향지시등 | Virtual Console "Blink left" → "곧 왼쪽으로 돌아요" |
| 실차 신호 · 실내 온도, 내기/외기 | 수신 (19.1°C, 외기) |
| Pleos TTS / STT | 클라이언트 키로 서버 연결됨 (권한 `pleos.car.permission.TTS_SERVICE`, `STT_SERVICE` 필요) |
| NaviHelper | 연결됨 |

## 아직 확인하지 못한 것 (친구 컴퓨터에서 해 주면 좋은 것)

1. **가족 모드**: "가족 모드" 버튼 → "B 굽은 길 · 거칠게" 또는 "D 가족 귀성" → "민준에게는 벌써 흔들림이 많아요" 안내가 나오는지
2. **정차 후 질문**: 재생 중 "정지" → "오늘 민준 괜찮았어요?" 대화상자 → 답하면 이벤트 기록에 "기준선 갱신"이 뜨는지
3. **휴게소**: 굽은 길 거칠게에서 높음이 3분 지속되면 "휴게소에 들를까요?" → 마이크로 "응" (에뮬레이터 Extended Controls › Microphone에서 호스트 마이크 허용) → "경유지 추가 요청"
4. **뒷좌석 탑승**: 실차 신호 모드에서 Virtual Console › Seat Occupancy › Rear left = Occupied → "가족 모드를 켤까요?"
5. **속도·위치**: `python3 sim/to_gpx.py B_winding_rough` → Extended Controls › Location › Routes에서 `sim/out/B_winding_rough.gpx` 재생 → 실차 신호 모드 화면의 km/h와 그래프가 움직이는지
6. **NaviHelper 다음 회전**: 에뮬레이터 내비에서 목적지 설정 후 실차 신호 모드 → 회전 전 예고

결과는 이 파일에 표로 추가하고 `qa/emulator` 브랜치로 PR을 열어 주세요.

## 알려진 문제: 에뮬레이터가 몇 분 뒤 꺼짐

- 로그에 `Failed to find ColorBuffer`가 보이지만 이는 매 부팅 때 나오는 경고이고, 실제로는 `Wait for emulator ... to shutdown gracefully` → 스냅샷 저장 → 종료 순서로 **정상 종료 요청**을 받고 꺼집니다.
- 의심 원인: 에뮬레이터 창을 닫음, Android Studio의 Running Devices가 기기를 넘겨받음, 맥 잠자기. GPU 모드(auto/host/swiftshader)와 데이터 초기화로는 해결되지 않았습니다.
- 재시작: Android Studio › Device Manager › `Pleos_Connect_v2_1` › Cold Boot Now. 터미널로 켰다면 그 창은 닫지 마세요.
