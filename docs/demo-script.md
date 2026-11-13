# 시연 영상 콘티 v3 (72초)

<img src="img/doc_demoscript_1.svg" width="100%" alt="변경 이력과 제출 필름: 11.13 저주파 소리 추가(100 Hz 1분, 음성 문구 변경, 11.8 필름은 저주파 전 영상, 외장 스피커나 합성으로 녹음), v3 11.3 · 11.4 · 11.8 화면 변경, 옛 v1 · v2 쓰지 않음, 필름 25초(11.8, 제출용, 차 안에서 겪는 순서, 에뮬레이터 녹화 합성 + AI 생성), 짧은 실제 화면 demo.webp(B_winding_rough 2배속, 몽글이 테마)">

> 필름: `../ai_video/explainer/멀미케어_필름_v3.mp4` (음성판 `멀미케어_필름_v3_음성.mp4`), README 맨 위 `docs/img/explainer.webp`. 옛 `../멀미케어_시연_v1.mp4` · `v2.mp4`는 쓰지 않음.

## 72초 콘티와 준비

<img src="img/doc_demoscript_2.svg" width="100%" alt="72초 에뮬레이터 전체 녹화(선택): 목표는 폰으로는 못 하는 장면 3개(차가 저주파 소리와 공기 바꿈, 멀미 완화 버튼 하나, 내비 옆 한 화면). 준비: Wi-Fi AndroidWifi, Virtual Console 26°C 내기 공조 켬, 디버그 빌드 설치, 몽글이 테마, 호스트에서 소리 녹음, 디버그 재생 옵션 --user 10">

에뮬레이터는 셸이 끝나도 꺼지지 않게 `nohup`으로 켭니다.

```bash
nohup ~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2_1 -no-snapshot-load -no-boot-anim -gpu host > /tmp/emu.log 2>&1 < /dev/null & disown
```

```bash
adb forward tcp:8088 tcp:8088
```

- Virtual Console: http://127.0.0.1:8088/
- 앱 설치: `cd android && ./gradlew :app:installDebug`
- 테마: 재생 명령에 `--es theme clay`를 붙이거나 설정에서 고름

```bash
adb shell am force-stop --user 10 ai.motioncare.app
```

```bash
adb shell am start --user 10 -n ai.motioncare.app/.MainActivity --ez skip_welcome true --es replay B_winding_rough --ei replay_from 60 --ef replay_speed 2
```

<img src="img/doc_demoscript_3.svg" width="100%" alt="장면별 재생 옵션: 지도 · 링(B_winding_rough, 0, 3배속), 공기 바꿈(0, 2배속), 멀미 완화(60, 2배속). 멀미 완화 버튼 좌표 2248 150, 20초 안에 다시 누르면 환기 중이에요만, 저주파는 1분 동안 재생 중 재생 안 함, 화면 녹화 screenrecord 최대 3분">

- 멀미 완화 버튼 좌표 (2560x1440): `adb shell input tap 2248 150`
- 화면 녹화: `adb shell screenrecord --size 1920x1080 --bit-rate 8000000 /sdcard/demo.mp4` (최대 3분, Ctrl+C로 끝내고 `adb pull /sdcard/demo.mp4`)

## 장면

<img src="img/doc_demoscript_4.svg" width="100%" alt="72초 장면표: 0–6초 질문, 6–18초 지도 탭 링 초록에서 노랑, 18–30초 브레이크 부드럽게 알림과 공조 26°C 내기에서 22°C 외기(저주파 100 Hz, 나고야대 연구), 30–44초 멀미 완화 한 번, 44–54초 분석 탭, 54–64초 Pleos 화면 나누기, 64–72초 구조도 sdk_map.svg. 창문은 에뮬레이터에서 움직이지 않음, 손잡이를 끄는 동안 앱이 흐려짐">

- 화면 나누기: `adb shell input swipe 2545 717 1700 717 1500`, 되돌리기 `adb shell input swipe 1705 717 2545 717 1500`

## 숏폼(선택, 30초)

<img src="img/doc_demoscript_5.svg" width="100%" alt="숏폼 30초: 필름 25초를 그대로 쓰거나 줄여 뒷좌석 동승자 멀미 순간, 멀미 완화 버튼, 낮은 소리 · 창문 · 바람, #HMG차량용앱개발공모전. 차량 내외부 이미지 노출 최소화. 공식 이름 현대자동차그룹 차량용 앱 개발 공모전, 10.28 설명회 기준, 접수 전 공지 다시 확인">

- 필수 해시태그 `#HMG차량용앱개발공모전` (10.28 설명회 정리 기준, 접수 전 공지 다시 확인)
