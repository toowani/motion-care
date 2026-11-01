# 에뮬레이터 확인 기록

## 확인 완료 (11.1)

| 항목 | 결과 |
|---|---|
| 재생 모드 · 굽은 길 거칠게 | 지수 상승 → "멀미 위험" → 음성 안내 → 공조 외기·22°C 설정 성공 (차량 공조 바 22.0° 표시) |
| 실차 신호 · 방향지시등 | Virtual Console "Blink left" → "곧 왼쪽으로 돌아요" |
| 실차 신호 · 실내 온도, 내기/외기 | 수신 (19.1°C, 외기) |
| Pleos TTS / STT | 클라이언트 키로 서버 연결됨 (권한 `pleos.car.permission.TTS_SERVICE`, `STT_SERVICE` 필요) |
| NaviHelper | 연결됨 |

## 확인 완료 (11.2)

| 항목 | 결과 | 확인하며 고친 것 |
|---|---|---|
| 가족 모드 | "가족 모드" → "B 굽은 길 · 거칠게" → t=78초 "민준 기준선 도달 (확률 0.51)" + 음성 안내 | 공조 외기 안내가 주의→높음 상승 때마다 반복되던 것을 5분에 한 번으로 |
| 휴게소 | 높음 3분 지속(t=198초) → "휴게소에 들를까요?" → 응 → 내비 앱에 경유지 "서울만남의광장휴게소" 요청 도착 | 권한 `pleos.car.permission.NAVI_ROUTE_SEARCH` 누락 추가. 음성 인식 실패 시 화면 응/아니 버튼으로 대체 |
| 정차 후 질문 | "정지" → "오늘 민준 괜찮았어요?" → 살짝 / 서연 괜찮음 → "기준선 갱신: 민준 26.0, 서연 37.5" | 가족 모드 버튼 글자가 길어지면 "정지"가 화면 밖으로 밀리던 것을 가로 스크롤로 |
| 뒷좌석 탑승 | Virtual Console Seat Occupancy › Rear left = Occupied → "뒷좌석에 탑승자가 있어요. 가족 모드를 켤까요?" → 응 → 가족 모드 켜짐 | 리스너 하나를 두 좌석에 등록하면 SDK가 마지막 좌석만 알려 줌 → 좌석마다 리스너 |
| 속도·위치 | 시나리오 좌표를 `adb emu geo fix`로 1초마다 입력 → 화면 42 km/h, 지수 상승, "뒷좌석이 흔들리고 있어요" 안내 | 에뮬레이터 위치는 속도·방향을 0으로 보내므로 연속 위치에서 계산. 250 km/h를 넘는 위치 점프는 무시 |
| NaviHelper 다음 회전 | `requestRoute`로 판교역 경로 요청 → "내비 다음 회전: 왼쪽 80 m" (TBT 수신) | 에뮬레이터 네트워크가 꺼져 있으면 경로 탐색 실패: 설정 › 연결 › Wi-Fi › AndroidWifi 연결 필요 |

## 남은 확인

- **회전 예고가 주행 중 실제로 울리는지 (NaviHelper 경로)**: 다음 회전 수신까지는 확인. 차가 움직여야 예고 거리(속도 × 5초) 안에 들어오므로 Virtual Console에서 기어 D + ACCEL로 속도를 올린 뒤 확인할 차례에 에뮬레이터가 꺼짐. 방향지시등 예고는 11.1에 확인됨.
- 가족 모드에서 실차 신호 중 "편안한 길" 경로 옵션 요청 (`changeRouteOption(COMFORTABLE)`): 코드만 추가, 에뮬레이터 미확인.

QA용 명령:

```bash
adb shell am start -n ai.motioncare.app/.MainActivity --es route_to "37.3947,127.1112,판교역"
python3 -I sim/feed_geo.py ~/Library/Android/sdk/platform-tools/adb sim/scenarios/B_winding_rough.csv 90
```

(`route_to`는 디버그 빌드에서만 동작합니다.)

## 알려진 문제: 에뮬레이터가 꺼짐 (원인 확인, 11.2)

- 로그에 `Failed to find ColorBuffer`가 보이지만 이는 매 부팅 때 나오는 경고입니다. 실제로는 `Wait for emulator ... to shutdown gracefully` → 스냅샷 저장 → 종료 순서로 **바깥에서 종료 요청**을 받고 꺼집니다. 차 안(logcat)에는 종료 기록이 없습니다.
- 원인: 에뮬레이터를 띄운 셸(터미널 탭, 도구의 백그라운드 작업)이 끝나면 그 자식 프로세스인 에뮬레이터도 종료 신호를 받습니다. Virtual Console을 닫아도 똑같이 꺼졌고, `nohup ... &`로 띄우자 계속 켜져 있었습니다.
- 해결: 아래처럼 `nohup`으로 띄우거나, Android Studio › Device Manager › `Pleos_Connect_v2_1` › Cold Boot Now로 켭니다.

```bash
nohup ~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2_1 -no-snapshot-load > /tmp/emu.log 2>&1 &
```
