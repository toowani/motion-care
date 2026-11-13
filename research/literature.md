# STEP 2 · 공식과 상수의 근거

<img src="../docs/img/doc_literature_1.svg" width="100%" alt="shared/params.json 값의 출처. 서지는 2026-11-02에 PubMed·Crossref 서지와 초록, 공개 원문 PDF(4번 두 편)로 재확인, ISO 2631-1 원문은 유료라 2차 문헌으로만 확인. 1 Turner & Griffin 1999: 버스·코치 56회 운행 승객 3,256명, 0.5 Hz 미만 옆 흔들림이 멀미와 가장 잘 맞음. 2 Donohew & Griffin 2004: 옆 방향 가중 0.0315–0.25 Hz 평평, 0.25–0.8 Hz 옥타브당 12 dB 감소. 3 ISO 2631-1: 구토 비율 ≈ Km × MSDVz, Km = 1/3, 수직 기준이라 공식 형태만 차용.">

## 핵심 근거 8편

<img src="../docs/img/doc_literature_2.svg" width="100%" alt="4 Kuiper 외 2020: 움직임 1초 전 음성 예고가 4–6초 뒤 음성보다 멀미 점수 유의하게 낮음(20명, 썰매). 4a Bos 외 2020: 예측 불가 움직임은 멀미 점수 52% 높음, 1초 전 음성 예고는 움직임 중 음성 대비 17% 낮음(예고 없음 대비 아님).">

<img src="../docs/img/doc_literature_3.svg" width="100%" alt="5 D'Amour 외 2017: 82명, 바람은 멀미를 유의하게 줄이고 좌석 진동은 효과 없음(시각 유발 멀미). 6 Huppert 외 2019: 아이 5,041명 중 9.2%가 멀미 취약, 4–13세에 가장 흔한 역U자형.">

<img src="../docs/img/doc_literature_4.svg" width="100%" alt="8 Gu 외 2025(나고야대): 100 Hz 순음 80–85 dBZ 1분을 흔들리기 직전 양쪽 귀에 들려주면 자세 흔들림 증가와 MSAQ 점수가 줄어듦(P < 0.05), 가짜 소리 대조 없음, 흔들리는 중 재생은 미검증, DENSO 후원. 7 Schmidt 외 2020: 5개국 4,479명 중 46%가 5년 안에 차멀미(어릴 때까지 59%).">

참고문헌 (DOI·원문 링크):

1. Turner & Griffin (1999) *Ergonomics* 42(12): [doi:10.1080/001401399184730](https://doi.org/10.1080/001401399184730)
2. Donohew & Griffin (2004) *Aviat Space Environ Med* 75(8): [PubMed 15328780](https://pubmed.ncbi.nlm.nih.gov/15328780/) (DOI 없음)
3. ISO 2631-1:1997: [iso.org/standard/7612](https://www.iso.org/standard/7612.html) (DOI 없음)
4. Kuiper, Bos, Diels & Schmidt (2020) *Applied Ergonomics* 85: [doi:10.1016/j.apergo.2020.103068](https://doi.org/10.1016/j.apergo.2020.103068)
4a. Bos, Kuiper & Schmidt (2020) DSC 2020 Europe VR: [TNO 원문 PDF](https://publications.tno.nl/publication/34645616/p6Xz0N58/bos-2020-driving.pdf) (DOI 없음) · 실험 A: Kuiper 외 (2020) *Human Factors* 62(8): [doi:10.1177/0018720819876139](https://doi.org/10.1177/0018720819876139)
5. D'Amour, Bos & Keshavarz (2017) *Exp Brain Res* 235(9): [doi:10.1007/s00221-017-5009-1](https://doi.org/10.1007/s00221-017-5009-1)
6. Huppert, Grill & Brandt (2019) *J Neurol* 266(Suppl 1): [doi:10.1007/s00415-019-09333-w](https://doi.org/10.1007/s00415-019-09333-w)
8. Gu, Ohgami, He, Kagawa, Kurniasari, Tong, Li, Tazaki, Takeda, Mouri & Kato (2025) *Environ Health Prev Med* 30: 22: [doi:10.1265/ehpm.24-00247](https://doi.org/10.1265/ehpm.24-00247) (특허는 [research/market.md](market.md))
7. Schmidt, Kuiper, Wolter, Diels & Bos (2020) *Transp Res Part F* 71: [doi:10.1016/j.trf.2020.03.012](https://doi.org/10.1016/j.trf.2020.03.012)

## 파라미터별 근거 상태

<img src="../docs/img/doc_literature_5.svg" width="100%" alt="파라미터별 근거 상태 표. 근거 있음: 누적량 공식 D = (Σ w a² Δt)^1/2 (3, 형태만 차용), 느린 흔들림 대역(10 s 평균 제거, 1 Hz 샘플; 1·2), 예고 시점(말 2 s + 준비 3 s; 4), 뒷좌석 우선(1). 일부 가정: tau0 = 30(3, 수직 기준), wX·wY = 1.0·1.0(1), gammaT·gammaV = 0.04 /°C·0.15(방향만 근거). 근거 없음: thetaCaution·thetaHigh = 0.9·1.5 m/s²(시뮬레이션 점검 G2 출발값, 출시 후 보정). tauPriorN = 5는 수축 추정 방법론.">

## 기획서에 쓸 문장 (초안)

<img src="../docs/img/doc_literature_6.svg" width="100%" alt="기획서 문장 초안 다섯 개: 예고 음성만으로 멀미 감소(Kuiper 2020), 0.5 Hz 미만 옆 흔들림이 멀미를 좌우(Turner & Griffin 1999), 얼굴 바람이 멀미를 줄임(D'Amour 2017), 아이 멀미는 4–13세에 가장 흔함(Huppert 2019), 100 Hz 소리 1분이 멀미를 줄임(Gu 2025).">

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
