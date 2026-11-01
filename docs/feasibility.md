# STEP 1 · 신호 확인 (G1)

에뮬레이터에서 앱의 "실차 신호 (Pleos)" 버튼을 누르고 Logcat(`tag:MotionCare`)과 화면을 보며 채웁니다.

| 신호 | API | 권한 | 확인 방법 | 결과 |
|---|---|---|---|---|
| 방향지시등 | `Vehicle.getTurnSignal().registerTurnSignalState` | `pleos.car.permission.CAR_LIGHTS` | Virtual Console에서 좌/우 켜기 → 예고 음성이 나오는지 | ✅ 11.1 · Turn Signal State "Blink left" → "곧 왼쪽으로 돌아요" |
| 실내 온도 | `HVAC.registerHvacTemperatureCurrent(ROW_1_LEFT)` | `CAR_CLIMATE` | 화면의 "실내 °C"가 바뀌는지 | ✅ 11.1 · 19.1°C 수신 |
| 내기/외기 읽기 | `HVAC.registerHvacRecirculationState` | `CAR_CLIMATE` | 화면의 "내기/외기" 표시 | ✅ 11.1 · 지원 영역은 `checkHvacRecirculationCapabilities`로 받아야 함 (운전석 ROW_1_LEFT는 미지원) |
| 내기/외기·온도 바꾸기 | `HVAC.setHvacRecirculation`, `setHvacTemperature` | `CONTROL_CAR_CLIMATE` | 이벤트 기록에 "공조: …"가 뜨는지, 실패 메시지가 뜨는지 | ✅ 11.1 · 외기 전환·22°C 설정 성공, 차량 공조 바에 22.0° 표시 |
| 속도 | `CarPropertyManager` `PERF_VEHICLE_SPEED` | `android.car.permission.CAR_SPEED` | Virtual Console에서 속도 변경 | ☐ 미확인 · 콘솔에 속도 직접 설정 없음(ACCEL 페달·크루즈 목표 속도만) |
| 진행 방향 | `LocationManager` GPS (에뮬레이터 GPX 재생) | `ACCESS_FINE_LOCATION` | Extended Controls › Location에서 경로 재생 | ☐ |

## G1 판단

- 방향지시등이 안 되면 → 예고는 경로 안내(NaviHelper)의 다음 회전만으로
- 공조 쓰기가 막히면 → 음성 제안("창문을 조금 열어 볼까요?")으로 대체, 기획서에 권한 신청 명시
- GPX 재생이 위치로 안 들어오면 → 앱 안의 재생 모드로만 시연

## 결과 (11.1)

- G1 통과: 방향지시등 이벤트 수신, 공조 읽기·쓰기 모두 동작 → 예고와 공조 닫힌 고리를 실차 신호로 시연 가능
- 속도·위치는 추가 확인 필요. 시연은 재생 모드로 충분
- Virtual Console 접속: `adb forward tcp:8088 tcp:8088` 후 http://127.0.0.1:8088/ (Emulation Controls 검색창에 항목 이름 입력)
- 콘솔에 Seat Occupancy(뒷좌석 탑승), Window Position, Steering Angle도 있음 → 가족 모드 자동 제안, 환기 파라미터에 활용 가능
