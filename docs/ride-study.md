# 실제 탑승 멀미 데이터 모으기 (주말 동승 키트)

<img src="img/doc_ridestudy_1.svg" width="100%" alt="가족·친구 차에 동승 → 폰 GPS 기록과 하차 직후 설문 → 앱 엔진이 예측한 흔들림과 실제 멀미가 맞는지 확인. 엔진은 1초마다 흔들림 지수 I(최근 60초 RMS)와 누적량 D를 계산. 연구윤리위원회(IRB) 승인을 받지 않은 비공식 예비 조사이므로 결과는 가능성 확인 수준">

- 기록 변환·분석: `python3 sim/import_ride.py`, `python3 sim/ride_report.py`
- 데이터 폴더: `data/rides/` (위치 기록은 git에 올리지 않음, 7절 참고)

## 1. 무엇을 확인하나

<img src="img/doc_ridestudy_2.svg" width="100%" alt="누적량 D = √(Σ(wx·ax² + wy·ay²)Δt). 질문 4개: 1 순위가 맞는가(탑승 8회 이상, Spearman ρ ≥ 0.6이면 맞는 방향, ρ < 0.3이면 다시 설계), 2 알림이 증상보다 먼저 오는가, 3 증상 시작 때 D가 초기 기준선 τ0 = 30 근처인가, 4 저주파 100 Hz 소리 1분의 효과(탐색, Gu 외 2025, 같은 사람·같은 길만 비교)">

## 2. 준비물

<img src="img/doc_ridestudy_3.svg" width="100%" alt="GPS 기록 앱: Sensor Logger(iPhone·Android, 압축 CSV, Location만 1 Hz)와 GPSLogger(Android, GPX 1.1 또는 CSV, 1초 간격·거리 필터 0 m). Sensor Logger 무료판은 Excel·KML이 유료라 CSV 사용. import_ride.py는 time·latitude·longitude·speed·bearing 열 이름을 자동 인식하고 ISO 8601 또는 1970년 기준 숫자 시간을 읽으며 속도 열에 km가 있으면 km/h. 위치 차이로만 속도를 구하면 D가 굽은 길 4%, 저속 도심 35% 크게 나옴">

<img src="img/doc_ridestudy_4.svg" width="100%" alt="주말 전 시험 기록: 집 근처 5분 걷기 또는 버스 기록, 파일 내보내기, import_ride.py 실행 확인(출력에 speed/bearing from log speed/course가 나오면 앱 속도 사용). 준비물: 충전된 폰·보조배터리·거치대나 컵홀더, 설문지(4절)와 시계, 멀미 봉투·물, 운전자에게 언제든 세워 달라고 하면 세워 주기를 미리 부탁">

```bash
python3 sim/import_ride.py 파일
```

## 3. 탑승 절차

<img src="img/doc_ridestudy_5.svg" width="100%" alt="출발 전 기준 상태 3가지(MISC 0 또는 1, 2시간 안에 멀미약·과식·공복 없음, 이전 탑승과 30분 이상 쉼·이전 MISC 3 이상이면 그날 중단). 같은 좌석(뒷좌석 권장), 폰 고정, 같은 행동 유지, 출발 직전 기록 시작·정차 후 종료, 증상 시작 시각 메모. MISC 6 이상이면 탑승을 끝내고 차를 세움: 안전이 먼저">

## 4. 하차 직후 설문 (1분)

<img src="img/doc_ridestudy_6.svg" width="100%" alt="하차 직후 4문항: 1 전체 멀미 점수 0–10(0.5 단위 가능), 2 MISC 한국어판 0–10(0 아무 문제 없음, 1 약간 불편, 2–5 어지러움 등이 아주 약함·조금·중간·심함, 6–8 메스꺼움 조금·중간·심함, 9 헛구역질, 10 토함), 3 증상 시작 시각(출발 후 분), 4 주로 한 일(창밖 보기·폰·책·잠). MISC는 Bos 등(2005)의 척도를 옮긴 비공식 번역">

### 참여 동의 문구

<img src="img/doc_ridestudy_7.svg" width="100%" alt="탑승 전에 읽어 주고 말로 동의를 받는 문구와 미성년자용 보호자 동의 서식(자녀 별명, 보호자 서명, 날짜). 이름 대신 별명, 위치 기록은 분석 후 삭제, 결과는 익명 숫자와 그래프로만 사용, 언제든 중단 가능. 자녀 본인에게도 쉽게 설명하고 싫다고 하면 기록하지 않음">

## 5. 탑승 구성 (총 5–10회)

<img src="img/doc_ridestudy_8.svg" width="100%" alt="탑승 구성: 1 highway smooth 15–30분(기준 탑승), 2 city smooth 15–20분, 3 city normal 15–20분, 4 winding smooth 10–20분, 5 winding normal 10–20분(멀미 가장 많을 것으로 예상), 6–10 조합 반복 또는 다른 탑승자(8회 이상이면 상관계수 판단 가능). 거칠게 운전 요구 금지, 하루 2–3회·굽은 길은 마지막, 운전자 휴대폰 조작 금지. 저주파 소리: 같은 사람·같은 길 2회 중 한 번만 출발 직전 100 Hz 순음 1분(약 60–66 dBA), notes에 tone=on/off 기록">

## 6. 파일 정리와 분석

<img src="img/doc_ridestudy_9.svg" width="100%" alt="survey.csv 열: ride_id, date, nickname, age_group, seat, road, style, activity, baseline_misc(0–1), score(0–10), misc, onset_min, start_s·end_s, notes">

1. 내보낸 파일 이름을 탑승 번호로 바꿔 `data/rides/`에 둡니다: `R01.gpx`, `R02.csv` …
2. 설문을 `data/rides/survey.csv`로 적습니다. 형식 예: [ride-survey-example.csv](ride-survey-example.csv) (값은 가짜)
3. 탑승 하나씩 확인:

```bash
python3 sim/import_ride.py data/rides/R01.gpx
```

4. 전체 보고서:

```bash
python3 sim/ride_report.py
```

<img src="img/doc_ridestudy_10.svg" width="100%" alt="import_ride.py 출력: 점 개수, 버린 점(250 km/h 초과), 신호 끊긴 구간(10초 이상, 직선으로 채움, D 작게 나옴), 길이, 평균 속도, 평균·최대 지수 I, 누적량 D, 주의·높음 알림 시간, 첫 알림 시각. --csv는 1 Hz CSV 저장. ride_report.py는 report.csv와 D–점수 산점도 report.svg(Pearson·Spearman)를 만들고 10회 미만이면 참고용. 실내 온도·내기순환은 기본값 22 °C·외기, 메모가 있으면 --temp 26 --recirc">

- 온도·내기순환 메모가 있으면: `python3 sim/import_ride.py --temp 26 --recirc 파일`

## 7. 개인정보

<img src="img/doc_ridestudy_11.svg" width="100%" alt="개인정보 4가지: 이름 대신 별명만 쓰고 연결을 적지 않음, GPS 파일에 집·학교 위치가 있으므로 report.csv를 만든 뒤 위치 파일 삭제(report.csv·report.svg에는 좌표 없음), .gitignore로 git 제외, 공모전 자료에는 산점도와 요약 숫자만 쓰고 지도·경로 그림은 넣지 않음">

`.gitignore`에 추가 (폴더 안 모든 파일을 무시하고 빈 폴더 표시 파일만 남김):

```
data/rides/*
!data/rides/.gitkeep
```

추가한 뒤 확인:

```bash
git check-ignore -v data/rides/R01.gpx
```

## 출처

- Sensor Logger 앱 정보(무료, 압축 CSV·JSON 기본 내보내기): [App Store](https://apps.apple.com/us/app/sensor-logger/id1531582925), [Android](https://appwisp.com/app/android/com.kelvin.sensorapp)
- GPSLogger(GPX·KML·CSV·NMEA 기록, 1–3초 간격 권장): [F-Droid](https://f-droid.org/packages/com.mendhak.gpslogger/)
- MISC: Bos, J. E., MacKinnon, S. N., & Patterson, A. (2005). Motion sickness symptoms in a ship motion simulator: effects of inside, outside, and no view. *Aviation, Space, and Environmental Medicine*, 76(12), 1111–1118.
