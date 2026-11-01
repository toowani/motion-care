# STEP 2 · 공식과 상수의 근거

`shared/params.json`의 값이 어디서 왔는지 정리합니다. 아래 서지는 초록과 저장소 기록으로 확인했고, 기획서에 인용하기 전 원문 PDF로 한 번 더 확인합니다 (11.1 확인).

## 핵심 근거 6편

| # | 문헌 | 우리 앱에서 쓰는 곳 | 핵심 내용 |
|---|---|---|---|
| 1 | Turner, M. & Griffin, M. J. (1999). Motion sickness in public road transport: the effect of driver, route and vehicle. *Ergonomics*, 42(12), 1646–1664. | 문제 정의, 파라미터 1·2, 운전 방식 시나리오 | 버스·코치 승객 3,256명, 56개 노선 조사. 앞뒤·옆 흔들림을 크게 만드는 운전자일수록 멀미가 많고, 옆 흔들림이 큰 국도 노선에서 메스꺼움이 많음. 0.5 Hz 미만의 옆 흔들림이 멀미와 가장 잘 맞음. 뒤쪽 좌석일수록 옆 흔들림과 멀미가 증가 |
| 2 | Donohew, B. E. & Griffin, M. J. (2004). Motion sickness: effect of the frequency of lateral oscillation. *Aviation, Space, and Environmental Medicine*, 75(8), 649–656. | 저주파 대역(수식 ①②), 옆 방향 가중 | 옆 방향 흔들림의 멀미 주파수 가중은 0.0315–0.25 Hz에서 평평하고 0.25 Hz 위로 옥타브당 12 dB씩 감소. 수직 방향 가중과 다름 |
| 3 | ISO 2631-1:1997. Mechanical vibration and shock — Evaluation of human exposure to whole-body vibration — Part 1. | 누적량 공식(수식 ③), 기준선 `tau0` | 멀미 노출량 MSDVz (단위 m/s^1.5). 구토 비율 ≈ Km × MSDVz, 성인 남녀 혼합 Km = 1/3. **수직 흔들림 기준**이라 차량 수평 흔들림에는 그대로 맞지 않는다는 비판이 있음 → 우리는 공식 형태만 빌리고 방향 가중은 1·2번으로 보완 |
| 4 | Kuiper, O. X., Bos, J. E., Diels, C. & Schmidt, E. A. (2020). Knowing what's coming: Anticipatory audio cues can mitigate motion sickness. *Applied Ergonomics*, 85, 103068. | 예고 음성(수식 ⑦), "운전자는 왜 멀미를 안 할까" | 움직임 1초 전에 방향·시점을 알려 주는 음성 신호를 준 조건에서 멀미 점수가 유의하게 낮음. 후속 연구: 예측 불가 움직임은 멀미 52% 증가, 예고로 17% 감소 (Bos 외 2020). 실험실 모션 장치 결과라 실제 차량 효과는 추가 확인 필요 |
| 5 | D'Amour, S., Bos, J. E. & Keshavarz, B. (2017). The efficacy of airflow and seat vibration on reducing visually induced motion sickness. *Experimental Brain Research*, 235(9), 2811–2820. | 환경 계수(수식 ④), 공조 닫힌 고리 | 82명 4개 집단. 얼굴로 오는 바람은 멀미를 유의하게 줄였고, 좌석 진동은 효과 없음. 시각 유발 멀미 연구라 차량 멀미에는 근거 수준을 낮춰 표현 |
| 6 | Henriques, I. F. 외 (2019). Survey of motion sickness susceptibility in children and adolescents aged 3 months to 18 years. *Journal of Neurology* (PubMed 31087161). | 가족 모드, 타깃(아이 있는 가족) | 4–13세에서 가장 흔하고 사춘기 이후 감소하는 역U자형. 6–9세에서 증상이 가장 심함. 2세 미만은 드묾 |

> 6번의 제1저자 표기는 원문으로 확인 필요. 나머지 서지는 초록 기준으로 확인했습니다.

## 파라미터별 근거 상태

| 항목 | 현재 값 | 근거 | 상태 |
|---|---|---|---|
| 누적량 공식 \(D = (\sum w a^2 \Delta t)^{1/2}\) | — | 3 | 근거 있음 (형태만 차용) |
| 저주파 대역 | 10 s 평균 제거, 1 Hz 샘플 | 1, 2 (옆 방향 0.25 Hz 이하가 핵심) | 근거 있음 |
| 누적 기준선 `tau0` | 30 | 3: Km 1/3 → MSDV 30에서 약 10% 구토 예측 (수직 기준) | 수평 흔들림 적용은 가정 |
| 방향 가중 `wX`, `wY` | 1.0, 1.0 | 1: 옆 흔들림이 멀미와 가장 강하게 연관 | `wY`를 1보다 크게 둘 근거 있음. 시뮬레이션 점검 후 조정 후보 |
| 지수 기준선 `thetaCaution`, `thetaHigh` | 0.9, 1.5 m/s² | 시뮬레이션 합리성 점검(G2)에 맞춘 출발값 | 문헌 직접 근거 없음 → 기획서에 "초기값, 출시 후 보정" 명시 |
| 환경 계수 `gammaT`, `gammaV` | 0.04 /°C, 0.15 | 5 (바람 효과의 방향), 크기는 가정 | 방향만 근거, 크기는 가정 |
| 예고 시점 | 말 2 s + 준비 3 s | 4는 1초 전 예고로도 효과 | 근거 있음 (우리는 더 여유 있게) |
| 개인 기준선 수축 `tauPriorN` | 5 | 적은 표본에서의 수축 추정 (방법론) | — |
| 뒷좌석 우선 | 뒷좌석 탑승 시 가족 모드 제안 | 1: 뒤쪽일수록 흔들림·멀미 증가 | 근거 있음 |

## 기획서에 쓸 문장 (초안)

- 운전자는 다음 움직임을 예측하기 때문에 멀미를 덜 한다. 실험에서도 움직임을 미리 알려 주는 음성만으로 멀미가 줄었다 (Kuiper 외, 2020).
- 버스 승객 3천여 명 조사에서 멀미는 운전 방식과 0.5 Hz 미만의 옆 흔들림에 좌우됐다 (Turner & Griffin, 1999). 우리 앱이 차량 데이터로 이 저주파 흔들림을 계산하는 이유다.
- 얼굴로 오는 바람은 멀미를 줄인다 (D'Amour 외, 2017). 차가 직접 공조를 바꿀 수 있는 이유가 여기에 있다.
- 아이의 멀미는 4–13세에 가장 흔하다 (2019, J Neurol). 가족 모드가 아이 기준으로 움직이는 이유다.

## 출처 링크

- Turner & Griffin 1999: https://eprints.soton.ac.uk/406261
- Donohew & Griffin 2004: https://core.ac.uk/works/412108
- ISO 2631-1 해설과 한계: https://arxiv.org/pdf/2501.17664
- Kuiper 외 2020: https://researchonline.rca.ac.uk/4350/
- D'Amour 외 2017: https://cir.nii.ac.jp/crid/1364233270594795648
- 소아 멀미 조사 2019: https://pubmed.ncbi.nlm.nih.gov/31087161/
