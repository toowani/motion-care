// Shared template for the README formula cards (docs/img/f_*.png).
// Each f_*.html sets window.F = {...} and loads this file; render.sh screenshots the card.
// Colours are the README's shared variable tokens: keep them identical in every figure.

const V = {
  I:   ['#2563EB', '#DBEAFE'], // 흔들림 지수
  D:   ['#7C3AED', '#EDE9FE'], // 누적 흔들림
  T:   ['#EA580C', '#FFEDD5'], // 개인 기준선 tau
  M:   ['#0D9488', '#CCFBF1'], // 환경 계수 m
  P:   ['#DC2626', '#FEE2E2'], // 멀미 가능성
  A:   ['#DB2777', '#FCE7F3'], // a_x, a_y
  W:   ['#8B5E3C', '#F3E8DD'], // omega (brown, kept apart from tau orange)
  Q:   ['#65A30D', '#ECFCCB'], // d_cue
  S:   ['#334155', '#E2E8F0'], // raw car signals
  K:   ['#6B7280', '#F3F4F6'], // constants
  Y:   ['#CA8A04', '#FEF9C3'], // 흔들림 / 보통 stage
  N:   ['#1F2340', '#E5E7EB'], // alert thresholds: neutral (grey / black dashes in charts)
};
const INK = '#1F2340', MUTED = '#6B6E86', HAIR = '#E3E4EE';

const macros = {};
for (const [k, [c]] of Object.entries(V)) macros['c' + k] = ['\\textcolor[RGB]{' + [1, 3, 5].map(i => parseInt(c.substr(i, 2), 16)).join(',') + '}{#1}', 1];

window.MathJax = {
  loader: { load: ['[tex]/color'] },
  tex: { packages: { '[+]': ['color'] }, macros },
  svg: { fontCache: 'none', mtextInheritFont: true },
  startup: {
    pageReady: () => MathJax.startup.defaultPageReady().then(() => document.fonts.ready).then(() => {
      const card = document.getElementById('card');
      document.title = 'H=' + Math.ceil(card.getBoundingClientRect().height);
    }),
  },
};

const css = `
html,body{margin:0;padding:0;background:transparent}
body.dark{background:#0d1117;padding:40px}
*{box-sizing:border-box}
.card{width:960px;background:#fff;border:1.5px solid ${HAIR};border-radius:16px;padding:30px 32px 32px;
  font-family:"Apple SD Gothic Neo","Noto Sans KR","Malgun Gothic",sans-serif;color:${INK}}
.head{display:flex;align-items:center;gap:12px;margin-bottom:18px}
.head h1{font-size:21px;font-weight:700;margin:0;letter-spacing:-0.2px}
.v{font-family:'Times New Roman',serif;font-style:italic;font-weight:600;font-size:24px}
.head .sec{margin-left:auto;font-size:12.5px;color:${MUTED};border:1px solid ${HAIR};border-radius:999px;padding:4px 11px;white-space:nowrap}
.chip{font-size:12.5px;border-radius:999px;padding:4px 11px;white-space:nowrap}
.chip.gray{background:#F3F4F6;color:#4B5563;border:1px solid #E5E7EB}
.row{display:flex;gap:16px;align-items:stretch}
.fbox{flex:1;background:#F8FAFC;border:1px solid ${HAIR};border-radius:12px;display:flex;align-items:center;justify-content:center;padding:22px 18px;min-height:130px}
.side{flex:0 0 auto;background:#F8FAFC;border:1px solid ${HAIR};border-radius:12px;padding:12px 14px;display:flex;flex-direction:column;justify-content:center}
.side .cap{font-size:12.5px;color:${MUTED};margin-bottom:4px}
.legend{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:10px 14px;margin-top:16px}
.li{display:flex;align-items:center;gap:12px;border:1px solid ${HAIR};border-radius:12px;padding:9px 12px;min-height:62px}
.sw{flex:0 0 auto;min-width:52px;padding:0 8px;height:44px;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:22px}
.tx{flex:1;min-width:0}
.tx .m{font-size:15px;font-weight:600;line-height:1.3}
.tx .s{font-size:13px;color:${MUTED};line-height:1.35;margin-top:2px}
mjx-container[display="true"]{margin:0 !important}
.li:last-child:nth-child(odd){grid-column:1/-1}
.tag{flex:0 0 auto;font-size:12.5px;border-radius:7px;padding:3px 8px;white-space:nowrap;font-weight:600}
.tag.k{color:#4B5563;background:#F3F4F6;border:1px solid #E5E7EB}
.tag.s{color:#334155;background:#E2E8F0}
.foot{margin-top:16px;background:#F8FAFC;border:1px solid ${HAIR};border-radius:12px;padding:14px 18px;display:flex;align-items:center;gap:14px;font-size:15px}
.foot .t{white-space:nowrap}
.foot .mut{color:${MUTED};font-size:13px}
.ic{flex:0 0 44px;width:44px;height:44px;border-radius:50%;display:flex;align-items:center;justify-content:center}
.ic svg{width:24px;height:24px;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
`;

// line icons, 24x24 grid, stroke 2
const ICON = {
  thermo: '<path d="M14 14.8V5a2 2 0 0 0-4 0v9.8a4 4 0 1 0 4 0Z"/><path d="M12 17v-6"/>',
  recirc: '<path d="M20 12a8 8 0 0 1-14.3 4.9"/><path d="M4 12a8 8 0 0 1 14.3-4.9"/><path d="M18.5 3.5v4h-4"/><path d="M5.5 20.5v-4h4"/>',
  down: '<path d="M12 4v16"/><path d="M6 14l6 6 6-6"/>',
  alert: '<path d="M12 3 2.5 20h19Z"/><path d="M12 10v4"/><path d="M12 17.2v.1"/>',
  turn: '<path d="M6 20v-7a4 4 0 0 1 4-4h8"/><path d="M14 5l4 4-4 4"/>',
  light: '<rect x="8" y="2.5" width="8" height="19" rx="3"/><circle cx="12" cy="7" r="1.4"/><circle cx="12" cy="12" r="1.4"/><circle cx="12" cy="17" r="1.4"/>',
  curve: '<path d="M5 21c0-6 14-5 14-11 0-3-2.5-6-7-7"/><path d="M8 3.5 12 3l-1 4"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  sum: '<path d="M17 5H7l6 7-6 7h10"/>',
  ruler: '<path d="M3 17 17 3l4 4L7 21Z"/><path d="M7 13l2 2M10 10l2 2M13 7l2 2"/>',
};
function icon(name, color, tint) {
  return `<div class="ic" style="background:${tint}"><svg viewBox="0 0 24 24" style="stroke:${color}">${ICON[name]}</svg></div>`;
}
window.icon = icon; window.V = V;

function m(tex) { return `\\(${tex}\\)`; }

function legendItem(it) {
  const [c, t] = V[it.v];
  const tag = it.k ? `<span class="tag k">${it.k}</span>` : it.sig ? `<span class="tag s">차 신호</span>` : '';
  const sym = it.icon ? icon(it.icon, c, t).replace('class="ic"', 'class="ic" style="border-radius:10px;width:52px;flex-basis:52px;background:' + t + '"') :
    `<div class="sw" style="background:${t};color:${c}">${m(it.tex)}</div>`;
  return `<div class="li">${sym}<div class="tx"><div class="m">${it.text}</div>${it.sub ? `<div class="s">${it.sub}</div>` : ''}</div>${tag}</div>`;
}

function build() {
  const F = window.F;
  const style = document.createElement('style'); style.textContent = css; document.head.appendChild(style);
  if (new URLSearchParams(location.search).get('bg') === 'dark') document.body.classList.add('dark');
  const card = document.createElement('div'); card.className = 'card'; card.id = 'card';
  card.innerHTML = `
    <div class="head"><h1>${F.title}</h1>${F.chip ? `<span class="chip gray">${F.chip}</span>` : ''}<span class="sec">${F.section}</span></div>
    <div class="row"><div class="fbox" style="font-size:${F.size || 44}px">${'\\[' + F.tex + '\\]'}</div>${F.side ? `<div class="side">${F.side}</div>` : ''}</div>
    ${F.mid || ''}
    <div class="legend" style="${F.cols ? 'grid-template-columns:' + F.cols : ''}">${F.legend.map(legendItem).join('')}</div>
    ${F.foot ? `<div class="foot">${F.foot}</div>` : ''}`;
  document.body.appendChild(card);
  if (F.after) F.after();
}

const s = document.createElement('script');
s.src = 'https://cdn.jsdelivr.net/npm/mathjax@3.2.2/es5/tex-svg.js';
document.addEventListener('DOMContentLoaded', () => { build(); });
document.head.appendChild(s);
