# STEP 2 · 공식과 상수의 근거

`shared/params.json`의 값이 어디서 왔는지 정리합니다. 아래 서지는 2026-11-02에 PubMed·Crossref 서지와 초록, 공개된 원문 PDF(4번 두 편)로 다시 확인했습니다. ISO 2631-1 원문은 유료라 확인 못 했고, 2차 문헌으로만 확인했습니다.

## 핵심 근거 8편

| # | 문헌 | 우리 앱에서 쓰는 곳 | 핵심 내용 |
|---|---|---|---|
| 1 | Turner, M. & Griffin, M. J. (1999). Motion sickness in public road transport: the effect of driver, route and vehicle. *Ergonomics*, 42(12), 1646–1664. [doi:10.1080/001401399184730](https://doi.org/10.1080/001401399184730) | 문제 정의, 파라미터 1·2, 운전 방식 시나리오 | 영국 본토 버스·코치 56회 운행의 승객 3,256명 조사 (운전자 17명, 차종 5종). 앞뒤·옆 흔들림을 크게 만드는 운전자일수록 멀미가 많고, 교외(cross-country) 위주 노선에서 옆 흔들림이 커서 메스꺼움이 많음. 0.5 Hz 미만의 옆 흔들림 노출이 멀미와 가장 잘 맞음. 뒤쪽 좌석일수록 옆 흔들림과 멀미가 증가. 차종 간 차이는 없음 |
| 2 | Donohew, B. E. & Griffin, M. J. (2004). Motion sickness: effect of the frequency of lateral oscillation. *Aviation, Space, and Environmental Medicine*, 75(8), 649–656. DOI 없음 → [PubMed 15328780](https://pubmed.ncbi.nlm.nih.gov/15328780/) | 느린 흔들림 대역(수식 ①②), 옆 방향 가중 | 120명(20명씩 6개 주파수, 0.0315–0.20 Hz) 실험을 이전 실험과 합쳐 0.0315–0.8 Hz 가중을 제안. 옆 방향 가중은 0.0315–0.25 Hz에서 평평하고 0.25–0.8 Hz에서 옥타브당 12 dB씩 감소. 수직 방향 가중과 다름. 옆 흔들림과 함께 롤이 있는 환경에는 맞지 않을 수 있음 |
| 3 | ISO 2631-1:1997. Mechanical vibration and shock — Evaluation of human exposure to whole-body vibration — Part 1: General requirements. [iso.org/standard/7612](https://www.iso.org/standard/7612.html) (표준이라 DOI 없음) | 누적량 공식(수식 ③), 기준선 `tau0` | 멀미 노출량 MSDVz (단위 m/s^1.5). 구토 비율 ≈ Km × MSDVz, 성인 남녀 혼합 Km = 1/3. **수직 흔들림 기준**이라 차량 수평 흔들림에는 그대로 맞지 않는다는 비판이 있음 → 우리는 공식 형태만 빌리고 방향 가중은 1·2번으로 보완. 표준 원문은 확인 못 함 (유료). Km = 1/3과 수직 한정은 Diels 외(2022, SAE 예고본)·Bos(2022, *Vibration*) 인용으로 확인 |
| 4 | Kuiper, O. X., Bos, J. E., Diels, C. & Schmidt, E. A. (2020). Knowing what's coming: Anticipatory audio cues can mitigate motion sickness. *Applied Ergonomics*, 85, 103068. [doi:10.1016/j.apergo.2020.103068](https://doi.org/10.1016/j.apergo.2020.103068) | 예고 음성(수식 ⑦), "운전자는 왜 멀미를 안 할까" | 20명, 레일 위 썰매에서 앞뒤 9 m 이동 15분 × 2조건. 움직임 1초 전에 "앞으로/뒤로" 음성을 준 조건이, 같은 음성을 움직임이 시작된 4–6초 뒤에 준 조건보다 멀미 점수가 유의하게 낮음. 실험실 모션 장치 결과라 실제 차량 효과는 추가 확인 필요 |
| 4a | Bos, J. E., Kuiper, O. X. & Schmidt, E. A. (2020). Motion predictability and sickness. *DSC 2020 Europe VR* (Driving Simulation Conference) 발표 논문, Antibes, 9–11 Sep 2020, 179–182. DOI 없음 → [TNO 원문 PDF](https://publications.tno.nl/publication/34645616/p6Xz0N58/bos-2020-driving.pdf) | 4번의 수치 인용 | 4번(실험 B)과 Kuiper 외(2020) *Human Factors* 62(8), 1339–1348 [doi:10.1177/0018720819876139](https://doi.org/10.1177/0018720819876139)(실험 A, 17명)를 묶어 정리한 학회 논문. 실험 A: 예측 불가 움직임(방향 또는 시점 무작위) 두 조건이 예측 가능 조건보다 멀미 점수 52% 높음. 실험 B: 움직임 1초 전 음성 예고가 움직임 중 음성보다 멀미 점수를 17% 낮춤. **17%는 '예고 없음'이 아니라 '움직임 중 음성' 대비**이며, 저널 초록에는 이 백분율이 없음 |
| 5 | D'Amour, S., Bos, J. E. & Keshavarz, B. (2017). The efficacy of airflow and seat vibration on reducing visually induced motion sickness. *Experimental Brain Research*, 235(9), 2811–2820. [doi:10.1007/s00221-017-5009-1](https://doi.org/10.1007/s00221-017-5009-1) | 환경 계수(수식 ④), 공조 닫힌 고리 | 82명을 4개 집단(바람, 진동, 바람+진동, 대조)에 무작위 배정, 1인칭 자전거 영상 15분. 바람은 멀미를 유의하게 줄였고, 좌석 진동은 효과 없음. 시각 유발 멀미 연구라 차량 멀미에는 근거 수준을 낮춰 표현 |
| 6 | Huppert, D., Grill, E. & Brandt, T. (2019). Survey of motion sickness susceptibility in children and adolescents aged 3 months to 18 years. *Journal of Neurology*, 266(Suppl 1), 65–73. [doi:10.1007/s00415-019-09333-w](https://doi.org/10.1007/s00415-019-09333-w) | 가족 모드, 타깃(아이 있는 가족) | 독일 인구 기반 설문 2건, 부모 3,285명이 아이 5,041명 응답. 6개월–18세 조사에서 9.2%가 멀미 취약. 4–13세에서 가장 흔하고 사춘기 이후 감소하는 역U자형. 1세 이후 발생하면 어릴수록 심하고 6–9세에서 가장 심함. 1세 미만 첫 발생은 극히 드묾(2명) |
| 8 | Gu, Y., Ohgami, N., He, T., Kagawa, T., Kurniasari, F., Tong, K., Li, X., Tazaki, A., Takeda, K., Mouri, M. & Kato, M. (2025). Just 1-min exposure to a pure tone at 100 Hz with daily exposable sound pressure levels may improve motion sickness. *Environmental Health and Preventive Medicine*, 30, 22. [doi:10.1265/ehpm.24-00247](https://doi.org/10.1265/ehpm.24-00247) | 핵심 기술: 저주파 소리 (`LowTone.kt`) | 나고야대. 생쥐 속귀 조직 실험으로 고른 100 Hz 순음을 80–85 dBZ(60.9–65.9 dBA)로 1분, 양쪽 귀에, 흔들리기 직전에 들려줌. 그네·주행 시뮬레이터·실제 차(머리받침 스피커, 10–25 km/h 정지·출발 3분, 책 읽기) 모두에서 자세 흔들림 증가가 줄었고, 시뮬레이터에서 심박 변이(HF 증가, LF/HF 감소), 실제 차에서 MSAQ 점수가 낮아짐(P < 0.05). 그네 실험 29명(멀미 잘 하는 쪽에서만 유의), 전체 등록 82명. 같은 사람이 소리 있음·없음을 모두 받는 설계이고 가짜 소리 대조는 없음. 사람에서 효과 지속 시간은 재지 않았고 생쥐에서 120분 이상. 4 kHz 청력 영향 없음. **흔들리는 중 재생 효과는 저자도 추가 연구가 필요하다고 씀.** DENSO 후원, 저자 2명(DENSO 소속)과 특허 발명자 2명. 특허 내용과 한국 출원 여부는 `research/market.md`. (J-STAGE 원문으로 2026-11-13 확인) |
| 7 | Schmidt, E. A., Kuiper, O. X., Wolter, S., Diels, C. & Bos, J. E. (2020). An international survey on the incidence and modulating factors of carsickness. *Transportation Research Part F: Traffic Psychology and Behaviour*, 71, 76–87. [doi:10.1016/j.trf.2020.03.012](https://doi.org/10.1016/j.trf.2020.03.012) | 요약 슬라이드의 "승객 차멀미 경험 46%" | 온라인 설문, 5개국(브라질·중국·독일·영국·미국) 4,479명. 46%가 지난 5년 동안 승용차 승객으로 어느 정도의 차멀미를 겪었다고 응답, 어릴 때 경험까지 넣으면 59%. 여성·젊은 층에서 더 많고, 주요 요인은 주행 방식·시각 활동·나쁜 공기. 자기 보고 설문이라 실제 발생률과는 다를 수 있음 (서지·초록은 Crossref·OpenAlex로 2026-11-12 확인) |

## 파라미터별 근거 상태

| 항목 | 현재 값 | 근거 | 상태 |
|---|---|---|---|
| 누적량 공식 \(D = (\sum w a^2 \Delta t)^{1/2}\) | — | 3 | 근거 있음 (형태만 차용) |
| 느린 흔들림 대역 | 10 s 평균 제거, 1 Hz 샘플 | 1, 2 (옆 방향 0.25 Hz 이하가 핵심) | 근거 있음 |
| 누적 기준선 `tau0` | 30 | 3: Km 1/3 → MSDV 30에서 약 10% 구토 예측 (수직 기준) | 수평 흔들림 적용은 가정 |
| 방향 가중 `wX`, `wY` | 1.0, 1.0 | 1: 옆 흔들림이 멀미와 가장 강하게 연관 | `wY`를 1보다 크게 둘 근거 있음. 시뮬레이션 점검 후 조정 후보 |
| 지수 기준선 `thetaCaution`, `thetaHigh` | 0.9, 1.5 m/s² | 시뮬레이션 합리성 점검(G2)에 맞춘 출발값 | 문헌 직접 근거 없음 → 기획서에 "초기값, 출시 후 보정" 명시 |
| 환경 계수 `gammaT`, `gammaV` | 0.04 /°C, 0.15 | 5 (바람 효과의 방향), 크기는 가정 | 방향만 근거, 크기는 가정 |
| 예고 시점 | 말 2 s + 준비 3 s | 4는 1초 전 예고로도 효과 | 근거 있음 (우리는 더 여유 있게) |
| 개인 기준선 수축 `tauPriorN` | 5 | 적은 표본에서의 수축 추정 (방법론) | — |
| 뒷좌석 우선 | 뒷좌석 탑승 시 가족 모드 제안 | 1: 뒤쪽일수록 흔들림·멀미 증가 | 근거 있음 |

## 기획서에 쓸 문장 (초안)

- 운전자는 다음 움직임을 예측하기 때문에 멀미를 덜 한다. 실험에서도 움직임을 미리 알려 주는 음성만으로 멀미가 줄었다 (Kuiper 외, 2020).
- 버스 승객 3천여 명 조사에서 멀미는 운전 방식과 0.5 Hz 미만의 옆 흔들림에 좌우됐다 (Turner & Griffin, 1999). 우리 앱이 차량 데이터로 이 느린 흔들림을 계산하는 이유다.
- 얼굴로 오는 바람은 멀미를 줄인다 (D'Amour 외, 2017). 차가 직접 공조를 바꿀 수 있는 이유가 여기에 있다.
- 아이의 멀미는 4–13세에 가장 흔하다 (Huppert 외, 2019). 멀미 케어의 효과가 아이에게 가장 클 것으로 보는 이유다.
- 100 Hz 소리를 1분 들려주면 뒤이은 흔들림에서 멀미가 줄었다 (Gu 외, 2025). 차는 언제 흔들릴지 알기 때문에 그 때를 잡아 소리를 틀 수 있다.

## 출처 링크

- Turner & Griffin 1999: https://eprints.soton.ac.uk/406261
- Donohew & Griffin 2004: https://core.ac.uk/works/412108
- ISO 2631-1 해설과 한계: https://arxiv.org/pdf/2501.17664
- Kuiper 외 2020: https://researchonline.rca.ac.uk/4350/
- D'Amour 외 2017: https://cir.nii.ac.jp/crid/1364233270594795648
- Bos 외 2020 (DSC): https://publications.tno.nl/publication/34645616/p6Xz0N58/bos-2020-driving.pdf
- D'Amour 외 2017 (PubMed): https://pubmed.ncbi.nlm.nih.gov/28634889/
- Huppert 외 2019 (PubMed): https://pubmed.ncbi.nlm.nih.gov/31087161/
- Schmidt 외 2020: https://doi.org/10.1016/j.trf.2020.03.012
- Gu 외 2025 (나고야대 100 Hz): https://www.jstage.jst.go.jp/article/ehpm/30/0/30_24-00247/_html/-char/en
