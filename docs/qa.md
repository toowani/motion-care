# 에뮬레이터 확인 기록

## 확인 완료 (11.13 · 저주파 소리)

<img src="img/doc_qa_1.svg" width="100%" alt="저주파 소리 LowTone: 100 Hz 순음 1분 재생을 dumpsys audio로 확인(시작, 1분 뒤 해제, 완화 버튼 재시작), 빌드 통과, 남은 것은 실차 소리 크기 측정">

## 확인 완료 (11.8 · 링 색 = 표정)

<img src="img/doc_qa_2.svg" width="100%" alt="링 길이는 멀미 가능성, 링 색은 표정을 따르도록 고침. 노란 표정 + 노란 링, 빨간 표정 + 빨간 링으로 확인, 다시 찍은 그림 5개">

## 확인 완료 (11.1)

<img src="img/doc_qa_3.svg" width="100%" alt="11.1 확인 표: 재생 모드 공조 설정, 방향지시등 · 실내 온도 신호 수신, Pleos TTS/STT와 NaviHelper 연결">

## 확인 완료 (11.2)

<img src="img/doc_qa_4.svg" width="100%" alt="11.2 확인 6항목(가족 모드, 휴게소, 정차 후 질문, 뒷좌석 탑승, 속도 · 위치, NaviHelper 다음 회전)과 확인하며 고친 것">

## 확인 완료 (11.2 오후)

<img src="img/doc_qa_5.svg" width="100%" alt="무드등 예고와 영역 한계, 편안한 길 요청(경로 변경은 미확인), 52초 시연 영상 v1">

## 확인 완료 (11.2 저녁 · 새 화면)

<img src="img/doc_qa_6.svg" width="100%" alt="새 화면 확인 7항목(첫 실행, 실시간 신호, 테마별 음성, 가족 모드 막대, 정차 후 질문, 멀미 났어, 시연 패널 끔)과 리뷰 워크플로로 고친 8건">

## 확인 완료 (11.2 · 백그라운드 서비스)

<img src="img/doc_qa_7.svg" width="100%" alt="백그라운드 서비스 확인 8항목(포그라운드 서비스, 내비 전환, 알림 카드, Pleos 헤드업 알림, MiniApp 등)과 리뷰로 고친 5건">

## 확인 완료 (11.2 밤 · 팀 회의 반영)

<img src="img/doc_qa_8.svg" width="100%" alt="팀 회의 의견 7건과 바꾼 것, 에뮬레이터 확인 결과, 확인하며 고친 핸들 각도 권한 · 부호 · 오래된 값">

## 확인 완료 (11.2 밤 · 실제 도로 지도와 새 캐릭터)

<img src="img/doc_qa_9.svg" width="100%" alt="실제 도로 지도와 새 캐릭터 5개 변경, 확인하며 고친 9건, 이번에 고치지 않은 남은 문제 4건">

## 확인 완료 (11.2 밤 · 한눈에 보는 화면)

<img src="img/doc_qa_10.svg" width="100%" alt="한눈에 보는 화면 5개 변경과 확인, 스크린샷 재촬영으로 고친 것, 리뷰로 고친 것, 남은 아이콘 캐시 문제">

## 확인 완료 (11.2 밤 · 앱 아이콘, 화면 나누기)

<img src="img/doc_qa_11.svg" width="100%" alt="앱 아이콘을 자동차 아이콘으로 되돌림, CRAVE 로고 위치, 실제 Pleos 내비와 화면 나누기 GIF, 화면 나누기와 아이콘 캐시 메모">

## 확인 완료 (11.3 · 스스로 찾은 개선)

### 지도 버벅임

<img src="img/doc_qa_12.svg" width="100%" alt="지도 버벅임 4건: 바뀌는 프레임 초당 약 12에서 39-42로, 가장 긴 멈춤 0.30초에서 0.07초로">

### 화면과 동작

<img src="img/doc_qa_13.svg" width="100%" alt="화면과 동작 개선 12건의 문제, 고친 것, 확인">

### 뒷좌석 탑승 값 뒤집힘 · 시연 재생 옵션

<img src="img/doc_qa_14.svg" width="100%" alt="뒷좌석 값이 뒤집힌 원인(Occupied 2, Empty 1)과 확인, 시연 재생 옵션과 주의점">

```bash
--es replay <시나리오> --ei replay_from <초> --ef replay_speed <배속>   # --user 10과 함께
am force-stop --user 10 ai.motioncare.app                                  # 앱이 이미 열려 있으면 먼저
```

### GIF 촬영 방법

<img src="img/doc_qa_15.svg" width="100%" alt="GIF 촬영 방법: 2배속 한 번에 녹화, 30 fps 보정 후 15/20 fps, 반복 지점에만 0.4초 겹치기, 프레임 간격과 파일 크기">

### 시뮬레이터와 엔진 (같은 회차)

<img src="img/doc_qa_16.svg" width="100%" alt="회전 예고 재준비 조건, 굽은 길 커브 예고 25곳, 지수 처음 60초, 테스트 벡터 2,162개 오차 2x10^-6 이하">

숫자 표: [validation.md](validation.md)

## 확인 완료 (11.3 · README 움직이는 그림 크기)

<img src="img/doc_qa_17.svg" width="100%" alt="README 움직이는 그림을 본문 폭 가득, 애니메이션 WebP 6개 840 px 20 fps 1.4-7.4 MB, 회전 예고 그림 2560 px 재녹화">

## 확인 완료 (11.3 · 팀장 회의 반영)

<img src="img/doc_qa_18.svg" width="100%" alt="팀장 회의 결정 1-8: 회전 예고 숨김, 가족 모드 숨김, 탭 두 개, 멀미 완화 버튼, 캐릭터 위젯, 색 하나로, Pleos 스타일 아이콘, 지도 줌 고정">

### 숨긴 화면 코드 보관

<img src="img/doc_qa_19.svg" width="100%" alt="later 폴더의 숨긴 화면 코드 파일 4개와, 확인하며 남은 것 5건(창문 움직임, 작업 표시줄 아이콘, 대비, 분석 기록 문구, 학습 기록)">

### README 그림

<img src="img/doc_qa_20.svg" width="100%" alt="README 그림 정리: 새로 찍은 것, 다시 만든 그래프, README에서 빠진 옛 그림 목록">

```bash
python3 sim/readme_figures.py
```

## 확인 완료 (11.4 · 팀 회의 2 반영)

<img src="img/doc_qa_21.svg" width="100%" alt="팀 회의 2 결정 A-G(이름, 팀 표기, 캐릭터 링, 휴게소 확장 이동, README, 지나온 길 지움), 고친 것, 확인 못 한 것, README 그림 재촬영">

## 남은 확인

<img src="img/doc_qa_22.svg" width="100%" alt="남은 확인 6건: 창문 실제 개방, 옛 화면 그림, 실차 경로선(Pleos 문의), 헤드업 알림(Pleos 문의), 도심 회전 뒤 지도 멈춤">

QA용 명령 (`route_to`는 디버그 빌드에서만 동작):

```bash
adb shell am start --user 10 -n ai.motioncare.app/.MainActivity --es route_to "37.3947,127.1112,판교역"
python3 -I sim/feed_geo.py ~/Library/Android/sdk/platform-tools/adb sim/scenarios/B_winding_rough.csv 90
```

## 알려진 문제: 에뮬레이터가 꺼짐 (11.2)

<img src="img/doc_qa_23.svg" width="100%" alt="에뮬레이터가 꺼지는 원인: 띄운 셸이 끝나면 자식 프로세스인 에뮬레이터도 종료 신호를 받음. nohup과 disown으로 띄우거나 Cold Boot Now">

```bash
nohup ~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2_1 -no-snapshot-load -no-boot-anim -gpu host > /tmp/emu.log 2>&1 < /dev/null & disown
```
