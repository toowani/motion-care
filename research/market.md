# 시장·경쟁 조사 메모

`docs/proposal.md` 4·5·8절의 근거입니다. 2026-11-02 웹 조사. 1차 출처(회사 보도자료·논문)를 먼저 적고, 없으면 기사로 대신했습니다.

## 1. 경쟁 서비스

### 폰 기능·앱

- **Apple Vehicle Motion Cues** — 2024년 5월 발표, iOS 18·iPadOS 18. 화면 가장자리 점이 차의 움직임을 따라 움직여 보는 것과 느끼는 것의 어긋남을 줄임. 폰 센서로 차 안인지 감지. 2025년 Mac, 2026년 visionOS로 확대.
  - [Apple 보도자료 2024.5](https://www.apple.com/newsroom/2024/05/apple-announces-new-accessibility-features-including-eye-tracking/)
  - [Apple 지원 문서](https://support.apple.com/guide/iphone/iphone-comfortably-riding-a-vehicle-iph55564cb22/ios)
- **Google Motion Assist** — 2024년 12월 "Motion Cues"라는 이름으로 코드에서 발견됐고, 2026년 8월 Android 17 Pixel부터 단계적으로 배포. Apple과 같은 화면 점 방식. Google 공식 발표는 확인하지 못함 (기사 기준).
  - [Android Authority](https://www.androidauthority.com/google-android-motion-sickness-feature-rolling-out-3702494/)
- **KineStop** (Urbandroid, 2018) — Android 앱. 다른 앱 위에 가상 수평선을 띄움. 설치 수는 집계 사이트 기준이라 기획서에는 쓰지 않음.
  - [AppGoblin](https://appgoblin.info/apps/com.urbandroid.kinestop)
- Honor MagicOS 9에도 비슷한 기능이 있다는 기사가 있으나 원문 확인 못함.

### 착용 도구

- **Citroën Seetroën** — 2018년. Boarding Ring 기술. 안경테 속 액체가 인공 수평선 역할. 증상이 시작되면 10–12분 착용, 10세 이상. 효과 95%는 개발사 주장.
  - [SAE 기사](https://www.sae.org/news/2019/02/citroen-seetrone-anti-motion-sickness-glasses), [CNN](https://edition.cnn.com/travel/article/motion-sickness-citroen-seetroen-glasses)

### 완성차·부품사

- **현대차 스무스 모드** — 2025년 7월 '더 뉴 아이오닉 6'에 현대차 최초 적용. 가감속 반응을 부드럽게 해 전기차 멀미를 줄임. 기사 일부의 "세계 최초"는 공식 표현에서 확인 못함.
  - [경향신문](https://www.khan.co.kr/article/202507281116011), [현대차그룹 뉴스룸](https://www.hyundaimotorgroup.com/ko/story/CONT0000000000193297)
- **기아 i-페달 3.0** (EV3, 2024) — 회생제동 감속을 더 선형적으로 만들어 울컥거림 완화. 멀미 전용 기능이라기보다 승차감 개선.
- **현대모비스 멀미 저감 기술** — 자세·심박·뇌파 센서 → 화면 안내, 시원한 바람 등 감각 자극. 회사 발표 "예방 최대 70%, 증상 40% 감소" (측정 방법 미공개). 2023.12 대한민국 해병대와 상륙돌격장갑차 적용 협약. 승용차 양산은 확인 못함.
  - [현대모비스 보도자료 (PR Newswire)](https://prnewswire.com/news-releases/hyundai-mobis-to-assist-marine-corps-in-reducing-motion-sickness-in-armored-vehicles-302023203.html)
- **JLR 웰니스 점수** — 2018년 연구. 생체 신호와 차량 움직임으로 사람별 멀미 취약도 계산. 2020년 후속 단계에서 자율주행 소프트웨어의 주행 방식에 적용. 시트 조정은 확인되지 않음 (11.2 재확인). "증상 최대 60% 감소"는 보도마다 표현이 달라 기획서에는 쓰지 않음. 양산 소식 없음.
  - [IMechE](https://imeche.org/news/news-article/jlr-fights-motion-sickness-with-autonomous-car-tech)
- **holoride** — Audi에서 2018년 분사. 차 움직임·위치에 맞춘 VR 콘텐츠. 2022년 €699 키트 출시, 2024년 파산 신청.
  - [Road to VR](https://roadtovr.com/audi-holoride-bankruptcy-vr-entertainment/)
- **Mercedes-Benz 특허** (바람·LED 조명으로 움직임 감각), **BMW 특허** (천장 화면에 차 움직임과 맞는 영상) — 특허 단계, 기사 기준.
  - [EVXL (Mercedes)](https://evxl.co/2026/03/02/mercedes-benz-patents-anti-motion-sickness), [BMWBLOG](https://www.bmwblog.com/2024/01/19/bmw-7-series-theater-screen-aims-to-combat-motion-sickness-in-cars)

### 연구 프로토타입

- **Diels, C., Dugenetet, P., Brietzke, A. & Pham Xuan, R. (2023).** Design strategies to alleviate motion sickness in rear seat passengers – a test track study. IEEE ITSC 2023. Bentley·Volkswagen 후원.
  - 뒷좌석 16명, e-리더를 읽으며 반복 가감속. ACC 설정·서스펜션 조정은 효과 없음. 속도를 보여 주는 측면 시각 흐름과 예측 조명 신호는 줄이는 경향이었지만 유의하지 않음. 모두 합친 "웰니스 팩"은 멀미 28% 감소(유의).
  - [RCA Research Online PDF](https://researchonline.rca.ac.uk/5638/1/Diels%20et%20al%20IEEE%20ITSC23_Design%20strategies.pdf)
- 예고 음성 실험(Kuiper 외, 2020)과 얼굴 바람 실험(D'Amour 외, 2017)은 `research/literature.md` 참고.

### 결론

- "원인을 차가 직접 줄이는 첫 시도"는 틀림. 스무스 모드(양산), 현대모비스·JLR(시범·연구)이 이미 있음.
- 찾지 못한 것: 차에 이미 있는 신호(방향지시등·내비·공조)만으로 예고와 공조를 한 고리로 묶고, 가족 중 가장 약한 사람 기준으로 맞추는 양산 서비스. 다만 모든 서비스를 다 본 것은 아니므로 "조사한 범위에서"라고 씀.

## 2. Pleos·Pleos Connect

| 항목 | 내용 | 출처 |
|---|---|---|
| 공개 | 2026.4.29 미디어 행사, 보도자료 2026.4.30 | [현대차그룹 보도자료](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-group-redefines-in-vehicle-experience-with-pleos-connect-next-generation-infotainment-system) |
| 첫 탑재 | '더 뉴 그랜저' (5월 출시 예정 → 2026.5.14 출시) | 보도자료, [파이낸셜뉴스 영문](https://en.fnnews.com/news/202605140726467026) |
| 확대 | 국내부터 현대차·기아·제네시스 글로벌 차량에 순차 적용 | 보도자료 |
| 목표 | 2030년까지 약 2천만 대 | 보도자료 |
| 앱 마켓 | 외부 앱 마켓, 개발사 플랫폼 Pleos Playground에서 차량 API·개발 도구 제공 | 보도자료 |
| 두 번째 차종 | '디 올 뉴 아반떼' 2026년 6월 부산모빌리티쇼 공개, 2026.8.5 계약 개시, 기본 트림부터 Pleos Connect | [이투데이](https://www.etoday.co.kr/news/view/2611117), [현대차그룹 뉴스룸](https://www.hyundaimotorgroup.com/ko/story/the-all-new-avante-world-premiere) |
| 참고 | 현대차 2025년 국내 판매 71만 2,954대 | [현대차 2025 실적](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-sales-4138180-units-2025) |

- 초안의 "Pleos는 2026년 그랜저부터, 2030년 2,000만 대"는 숫자가 맞음. 정확한 이름은 브랜드 Pleos가 아니라 인포테인먼트 Pleos Connect.
- MAU 계산의 가정값은 3절 "MAU 근거"에서 통계로 바꿈 (11.2). Pleos Connect 실제 누적 탑재 대수는 공식 발표가 없어 판매량으로 대신함.

## 3. MAU 근거

2026-11-02 조사. `docs/proposal.md` 8절의 예상 MAU와 B2B 시장 규모의 근거입니다.

### 바뀐 식

| 항목 | 초안 | 바꾼 값 | 근거 |
|---|---|---|---|
| 탑재 차량 | 30만 대 [가정] | 15만 1,110대 (1년) | 그랜저·아반떼 2025년 국내 판매 합 |
| 가족 차량 비율 | 40% [가정] | 20.3% | 통계청 18세 이하 자녀 가구 비율 |
| 설치율 | 10% [가정] | 10% [가정] | 통계 없음, 아래 이유 |
| 월 사용 | 60% [가정] | 60% [가정] | 통계 없음, 아래 이유 |
| MAU | 약 7,200 | 약 1,800 (1년) / 약 560 (2026.9 말 현재) | |

계산: 151,110 × 0.203 × 0.10 × 0.60 = 1,840. 현재: 45,911 × 0.203 × 0.10 × 0.60 = 559.

### 탑재 차량

- Pleos Connect가 들어간 국내 차종은 지금 '더 뉴 그랜저'(2026.5.14 출시)와 '디 올 뉴 아반떼'(2026.8.5 계약 개시, 전 트림 기본, 가솔린 2.0은 3분기 출고·하이브리드는 친환경차 고시 뒤 출고).
  - [현대차그룹 아반떼 보도자료 2026.8.5](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-all-new-avante)
- 2025년 국내 판매: 그랜저 71,775대, 아반떼 79,335대 → 합 151,110대. 1년 탑재량의 근사값으로 씀.
  - [현대차 2025 실적](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-sales-4138180-units-2025)
- 2026년 월별 국내 판매 (현대차 월간 실적 보도자료):

| 월 | 그랜저 | 아반떼 | 출처 |
|---|---|---|---|
| 5월 | 5,183 | 4,526 | [5월](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-may-2026-sales) |
| 6월 | 10,062 | 4,316 | [6월](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-june-2026-sales) |
| 7월 | 8,532 | 2,624 | [7월](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-july-2026-sales) |
| 8월 | 5,931 | 1,462 | [8월](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-august-2026-sales) |
| 9월 | 8,898 | 7,305 | [9월](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-september-2026-sales) |

- 그랜저 5–9월 합 38,606대. 5월 보도자료는 "더 뉴 그랜저 출고가 이번 달부터 본격화"라고만 해서 5월 숫자에 구형이 섞였을 수 있음 [확인 필요].
- 아반떼는 5–8월이 구형으로 보이고(8월 급감), 9월 7,305대를 신형으로 봄. 신형·구형 나눈 숫자는 없음 [확인 필요].
- 2026년 10월 실적은 아직 발표 전 (11.2 기준).
- 기아·제네시스 적용 차종과 시기는 블로그·나무위키에만 있고 공식 발표를 찾지 못함 [확인 필요]. 2030년 2천만 대 중 국내 비중도 발표 없음.

### 가족 차량 비율

- 2024년 일반가구 22,294천 가구 중 18세 이하 자녀가 있는 가구 4,517천 가구(20.3%), 전년보다 1.6% 감소. 5세 이하 자녀 가구는 1,284천 가구(5.8%).
  - [통계청 2024 인구주택총조사(등록센서스) 보도자료, 2025.7.29](https://kostat.go.kr/board.es?mid=a10301020100&bid=203&act=view&list_no=437767) — PDF 91쪽 표 70
- 한계: 가구 기준이라 그랜저·아반떼 구매자 중 비율과 다름. 아이 있는 가구가 차를 더 많이 가질 수 있어 위로, 그랜저 구매층이 나이가 많아 아래로 치우칠 수 있음. 4–13세 자녀 가구 비율은 공표 표에 없음 [확인 필요] — KOSIS 자녀 연령별 표나 마이크로데이터로 확인 가능할 것.
- 가구당 승용차 보유 대수는 이번 계산에 필요하지 않아 찾지 않음.

### 설치율·월 사용률

- 차량 내장 앱 마켓의 앱별 설치율·월 사용률 공개 통계는 찾지 못함.
- 참고: 컨슈머인사이트 제25차 연례 자동차 기획조사(2025.7, 3년 내 새 차 구입자 중 커넥티드 카 이용자 1,489명). AI 서비스 이용 경험률 9%, 주이용률 1%. 다른 기능의 이용 경험률은 공개 본문에 없음.
  - [컨슈머인사이트 2026.2.12](https://www.consumerinsight.co.kr/autoinsight/abview?no=3859&id=pr4_list)
- 설치율 10% [가정]: 기본 기능이 아닌 부가 서비스를 써 본 비율이 한 자릿수(9%)라는 점에 맞춤.
- 월 사용 60% [가정]: 시동 때 자동 실행되지만 아이가 탄 날만 의미가 있음. 설치한 가족 중 한 달에 한 번 이상 아이를 태우는 비율을 보수적으로 잡음.
- 출시 후 앱 마켓 통계로 바로 바꿀 값.

### B2B 시장 규모

- **택시**: 전국 면허 246,258대 = 개인택시 164,806 + 일반(법인)택시 81,452. 2026.8.31 기준. 법인 등록 대수는 64,520대(면허보다 적음).
  - [전국개인택시운송사업조합연합회 면허·등록대수 현황](https://gntx.kr/information/information_02.php) (EUC-KR 페이지, '합계' 행)
  - 참고: 서울 면허 71,615대(2026.1 말, [서울시](https://news.seoul.go.kr/traffic/archives/307)).
- **어린이 통학버스**: 전국 약 43,857대. 경찰·교육부 합동 점검(그중 10%, 약 4,300대) 기사의 숫자. 기사에 집계 기준(경찰 신고 대수인지)이 없음 [확인 필요].
  - [헤럴드경제 2026.3.31](https://www.heraldk.com/article/2026033123432142372)
  - 2014년 교육부 전수조사(67,363대 중 신고 37,409대, 학원 신고율 7.1%)도 있으나 오래되고 원문 확인 못함 — 기획서에는 쓰지 않음.
- **자율주행 셔틀**: 2020년부터 9차례, 고속도로 포함 17개 시·도 42곳 시범운행지구 지정. 지정 1년이 지난 34곳 중 14곳은 서비스 미운영.
  - [서울경제 2025.4.1](https://www.sedaily.com/article/14047575)
  - 2025년 하반기 이후 추가 지정(양주, 성남 등)이 있어 현재 총수는 더 많을 수 있음. 전국 운행 차량 대수는 찾지 못함 [확인 필요]. 예: 성남 2개 노선에 1대씩 ([디지털타임스](https://www.dt.co.kr/article/12036810), 검색 요약 기준, 원문 미확인).
