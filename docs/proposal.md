# 기획서 초안 · 멀미 케어

**차 신호로 때를 잡는 저주파 소리 · 환기 · 음성 안내** · 팀 CRAVE

> 접수 양식 항목에 맞춰 옮기기 전의 초안입니다. 수치의 근거는 [`docs/validation.md`](validation.md), 문헌은 [`research/literature.md`](../research/literature.md)에 있습니다. [확인 필요] 표시는 제출 전에 채웁니다.

## 1. 한 줄 소개

<img src="img/doc_proposal_1.svg" width="100%" alt="한 줄 소개: 저주파, 환기, 음성 안내로 개인 기준을 학습하는 Pleos 차량용 멀미 앱, 핵심은 차 신호로 때를 잡는 100 Hz 소리 1분">

## 2. 문제

<img src="img/doc_proposal_2.svg" width="100%" alt="문제: 운전자와 달리 동승자는 예고 없이 흔들려 멀미, 버스 3,256명 · 승객 46% · 4-13세 조사, 멀미가 난 뒤에야 대처">

## 3. 해결 방법 (저주파 · 환기 · 음성 안내)

<img src="img/doc_proposal_3.svg" width="100%" alt="해결 방법 네 가지: 저주파 소리를 때맞춰 틀기, 흔들림 먼저 계산, 실내 공기 바꾸기(닫힌 고리), 멀미 완화 버튼 하나. P는 확률이 아니라 보정 전 점수">

<img src="img/doc_proposal_4.svg" width="100%" alt="저주파 소리 특허: 나고야대 4개국 등록, 한국 출원 없음, 해외 판매는 라이선스 필요, 후속 출원은 청구항이 다름, sound spice 상표 미사용">

### 사용자 경험 (UX)

<img src="img/doc_proposal_5.svg" width="100%" alt="UX: 보지 않아도 되는 안내, 하나의 색 척도, 아이가 고르는 테마, 지도와 분석 두 탭, 내비와 함께 쓰기, 설정, 앱 아이콘">

## 4. 차량특화성 (왜 차여야 하나)

<img src="img/doc_proposal_6.svg" width="100%" alt="폰 앱과 멀미 케어 비교: 환기 읽기, 바꾸기, 창문 열기, 백그라운드 실행은 차에서만 가능">

### 다른 멀미 대응 서비스와 비교

출처와 자세한 메모는 [`research/market.md`](../research/market.md)에 있습니다.

<img src="img/doc_proposal_7.svg" width="100%" alt="다른 멀미 대응 서비스 10종과의 비교 표와 현대차 기아 멀미 Zero 참고 사항">

## 5. 창의성

<img src="img/doc_proposal_8.svg" width="100%" alt="창의성: 첫 시도는 아니지만 조치(공조 창문)와 개인 기준선 학습의 조합, 앱 마켓 배포, 확장 가능성, 멀미 Zero와 겹치는 부분">

확장 기능 코드 위치: `android/app/src/main/kotlin/ai/motioncare/app/later/`

## 6. 기술 활용성 (Pleos SDK)

<img src="img/doc_proposal_9.svg" width="100%" alt="Pleos SDK 8종의 쓰는 곳, 엔진과 SDK 분리(2,162개 샘플 일치), 계산 요약 D, m, 개인 기준선, 예고 거리">

## 7. 검증

<img src="img/doc_proposal_10.svg" width="100%" alt="검증 세 단계: 시나리오 시뮬레이션 6개 점검 10개 통과, comma2k19 45초 비교 0.97배, Pleos 에뮬레이터. 실제 탑승자 데이터는 없음, 판단 기준 Spearman 0.6 / 0.3">

실제 탑승자 데이터는 없습니다. 동승 조사 계획: [`docs/ride-study.md`](ride-study.md), 점검 기록: [`docs/qa.md`](qa.md).

한계 (README 8절과 같음):

<img src="img/doc_proposal_11.svg" width="100%" alt="한계 9가지: 정답 데이터 없음, 실차 시험 불가, 창문, 주행 비교, 알림 카드, 지도 탭, 핸들 상수, 자동 실행, 시연">

## 8. 사업성

<img src="img/doc_proposal_12.svg" width="100%" alt="사업성: 타깃과 예상 MAU 약 4,200명(보수 1,400, 낙관 9,700) 계산식과 [가정] 근거">

<img src="img/doc_proposal_13.svg" width="100%" alt="사업성: OEM 기능 대비 앱의 몫과 위험, 수익 모델, 승차감 정량 지표">

<img src="img/doc_proposal_14.svg" width="100%" alt="확대 계획과 B2B: 택시 24만 6,258대, 통학버스 약 4만 3,857대, 자율주행 시범운행지구 42곳">

출처:
- [현대차그룹 보도자료, 2026.4.30](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-group-redefines-in-vehicle-experience-with-pleos-connect-next-generation-infotainment-system)
- [현대차그룹 보도자료, 2026.8.5](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-all-new-avante)
- [현대차 2025 실적](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-sales-4138180-units-2025)
- [컨슈머인사이트, 2026.2](https://www.consumerinsight.co.kr/autoinsight/abview?no=3859&id=pr4_list)
- [전국개인택시운송사업조합연합회](https://gntx.kr/information/information_02.php)
- [헤럴드경제, 2026.3.31](https://www.heraldk.com/article/2026033123432142372)
- [서울경제, 2025.4.1](https://www.sedaily.com/article/14047575)
- 월별 실적 보도자료 등 자세한 메모: [`research/market.md`](../research/market.md)

## 9. 개인정보 · 10. 팀과 역할

<img src="img/doc_proposal_15.svg" width="100%" alt="개인정보는 차량 기기에만 저장, 14세 미만은 법정대리인 동의. 팀 CRAVE 역할 기획 연구 개발 검증">
