# STEP 1 · 신호 확인 (G1)

<img src="img/doc_small_1.svg" width="100%" alt="G1 신호 확인표 (Pleos 에뮬레이터, 11.1): 방향지시등(CAR_LIGHTS)·실내 온도 19.1°C·내기/외기 읽기·내기/외기와 온도 바꾸기(22°C 성공)는 통과, 속도(콘솔에 직접 설정 없음)와 진행 방향(GPX 재생)은 미확인">

Logcat 필터: `tag:MotionCare`

## G1 판단

<img src="img/doc_small_2.svg" width="100%" alt="G1 대체안: 방향지시등이 안 되면 내비 다음 회전만으로, 공조 쓰기가 막히면 음성 제안과 기획서 권한 신청 명시, GPX 재생이 위치로 안 들어오면 앱 안의 재생 모드로만 시연">

## 결과 (11.1)

<img src="img/doc_small_3.svg" width="100%" alt="11.1 결과: G1 통과(방향지시등 이벤트 수신, 공조 읽기·쓰기 동작, 예고와 공조 닫힌 고리를 실차 신호로 시연 가능), 속도·위치는 추가 확인(시연은 재생 모드로 충분), Virtual Console 접속, Seat Occupancy·Window Position·Steering Angle 신호도 있음">

```
adb forward tcp:8088 tcp:8088
```

Virtual Console: http://127.0.0.1:8088/
