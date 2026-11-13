# 시뮬레이션 검증 결과 (기획서 "검증" 절의 근거)

<img src="img/doc_validation_1.svg" width="100%" alt="실차 없이 두 가지로 검증(합리성 점검, 실제 주행 비교). 시나리오 4개는 모두 실제 도로(강남 도심, 북악스카이웨이, 경부고속도로 판교–수원신갈, 동탄–남한산성로 가족 나들이)이고 OSRM 경로를 1 m 간격으로 나눠 만듦">

모든 숫자는 아래 명령으로 다시 만들 수 있습니다 (경로는 `sim/fetch_routes.py`가 받아 둔 `sim/routes/`).

```
python3 sim/generator.py
python3 sim/prototype.py
python3 sim/public_check.py
python3 sim/freq_response.py
```

## 1. 합리성 점검 G2 (10개 모두 통과)

<img src="img/doc_validation_2.svg" width="100%" alt="합리성 점검 G2 표: 시나리오 6개(도심 · 굽은 길 부드럽게/거칠게, 고속도로, 가족 나들이)의 길이, 평균 · 최대 지수, 누적량 D, 주의 · 높음 초, 예고 횟수">

<img src="img/doc_validation_3.svg" width="100%" alt="1절 결과: 고속도로 < 도심 < 굽은 길 순서, 거칠게 몰면 누적량 도심 1.57배 · 굽은 길 1.58배, 첫 알림 시각, 엔진에만 있는 휴게소 · 예고 · 공조 닫힌 고리 · 가족 모드 점검">

### 필터를 바꾸며 달라진 동작 (11.11)

<img src="img/doc_validation_4.svg" width="100%" alt="필터 변경 전후 표: 평균 · 최대 지수가 커지고 도심 부드럽게에서 주의 알림이 324초에 한 번 생김, 도심 거칠게는 높음 41초">

## 2. 실제 주행 데이터와 비교 (현실성 점검)

<img src="img/doc_validation_5.svg" width="100%" alt="comma2k19 실제 고속도로 45초와 시뮬 고속도로 비교 표와 보정 경과: 앞뒤 가속도 0.22배에서 1.4배로, 필터 변경 뒤 2.13배로 기준 초과">

<img src="img/doc_validation_6.svg" width="100%" alt="생성기 보정(11.12): 목표 가속도 1 m/s², 50 m 이동 평균으로 0.97배, 한계는 직선 45초라 옆 가속도 비교 불가">

## 3. 엔진 주파수 응답 (멀미 대역 필터)

<img src="img/freq_response.svg" width="100%" alt="엔진 주파수 응답: 예전(회색) · 지금(파랑) · 문헌 옆 흔들림 가중(점선)">

`python3 sim/freq_response.py`로 다시 만들 수 있습니다. 파라미터는 `shared/params.json`의 `band*` 값입니다.

<img src="img/doc_validation_7.svg" width="100%" alt="주파수별 응답 표: 0.05–0.25 Hz에서 지금 엔진은 문헌 평탄 구간과 +0.5 / −1.0 dB 안, 0.5 Hz에서 −11.8 dB (문헌 −12 dB)">

<img src="img/doc_validation_8.svg" width="100%" alt="예전 원인은 지수 이동 평균(α = 0.5), 지금은 0.0315 Hz 고역 통과 + 0.27 Hz 저역 통과 2차 필터, 결과 0.05–0.25 Hz 문헌 일치 · 지수 9–27% 증가">

## 4. 기획서에 쓸 정직한 한계

<img src="img/doc_validation_9.svg" width="100%" alt="정직한 한계: 정답 멀미 데이터는 시뮬레이션으로 알 수 없고, 실제 주행 비교는 45초 고속도로 한 구간뿐">
