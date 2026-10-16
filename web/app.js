/* Earlln History — 웹앱(PWA). 안드로이드 앱과 같은 history.json 을 읽는다.
 * 주소(#/…)로 화면을 나눠서 브라우저 · 아이폰의 뒤로 가기가 그대로 통한다.
 *   #/?s=korean&m=era|key&c=분류   홈 (과목 · 보기 · 분류)
 *   #/n/<id>                       세부 화면
 *   #/search?q=…                   검색
 */
'use strict';

const LINK = /\[\[([^\]|]+)(?:\|([^\]]+))?]]/g;
const WORD = /[가-힣A-Za-z0-9]+/g;
const CATEGORY_ORDER = ['인물', '사건·전쟁', '제도·정책', '국가·단체', '사상·종교', '문화·유산', '경제·사회', '조약·문서'];
const ENDINGS = ['에서는', '으로는', '에서', '으로', '에게', '까지', '부터', '처럼', '보다', '이다', '였다', '했다', '하여', '하고', '하는', '하며', '되어', '되었다',
  '을', '를', '은', '는', '의', '에', '와', '과', '한', '된'];
const CHOSEONG = 'ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ';
const TYPE_LABEL = { subject: '과목', era: '시대', topic: '주제', key: '키워드' };

let DATA = null;          // { subjects, nodes }
const backlinks = {};     // id → [이 항목을 언급한 id]
const subjectCache = {};

const $view = document.getElementById('view');
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const node = id => DATA.nodes[id];
const children = id => (node(id)?.children || []).map(node).filter(Boolean);

function ancestors(id) {
  const out = [];
  let cur = node(id)?.parent;
  while (cur && node(cur)) { out.unshift(node(cur)); cur = node(cur).parent; }
  return out;
}
function subjectOf(id) {
  if (id in subjectCache) return subjectCache[id];
  let n = node(id);
  while (n && n.type !== 'subject') n = node(n.parent);
  return (subjectCache[id] = n ? n.id : null);
}
const eraOf = id => ancestors(id).find(a => a.type === 'era');
const plain = t => t.replace(LINK, (_, id, label) => label || node(id.trim())?.title || id);

function searchTerm(word) {
  for (const e of ENDINGS) if (word.endsWith(e) && word.length - e.length >= 2) return word.slice(0, -e.length);
  return word;
}
const googleUrl = term => 'https://www.google.com/search?q=' + encodeURIComponent(term);

function initialOf(title) {
  const c = title.charCodeAt(0);
  if (c >= 0xAC00 && c <= 0xD7A3) {
    const ch = CHOSEONG[Math.floor((c - 0xAC00) / 588)];
    return ({ 'ㄲ': 'ㄱ', 'ㄸ': 'ㄷ', 'ㅃ': 'ㅂ', 'ㅆ': 'ㅅ', 'ㅉ': 'ㅈ' })[ch] || ch;
  }
  return /[A-Za-z]/.test(title[0]) ? 'A–Z' : '#';
}

/* 링크 아닌 글: 낱말마다 구글 검색 링크 (숫자만인 낱말은 그냥 글자) */
function searchable(text) {
  let out = '', last = 0;
  for (const m of text.matchAll(WORD)) {
    out += esc(text.slice(last, m.index));
    const w = m[0];
    out += /^\d+$/.test(w) ? w
      : `<a class="w" href="${googleUrl(searchTerm(w))}" target="_blank" rel="noopener">${esc(w)}</a>`;
    last = m.index + w.length;
  }
  return out + esc(text.slice(last));
}

/* [[id|글자]] → 앱 안 링크, 나머지 → 낱말 검색 링크 */
function linked(text) {
  let out = '', last = 0;
  for (const m of text.matchAll(LINK)) {
    out += searchable(text.slice(last, m.index));
    const id = m[1].trim();
    const target = node(id);
    const label = m[2] || target?.title || id;
    out += target ? `<a class="k" href="#/n/${id}">${esc(label)}</a>` : searchable(label);
    last = m.index + m[0].length;
  }
  return out + searchable(text.slice(last));
}

function bodyHtml(body) {
  let html = '', list = false;
  const close = () => { if (list) { html += '</ul>'; list = false; } };
  for (const raw of body.split('\n')) {
    const line = raw.trimEnd();
    if (!line.trim()) { close(); continue; }
    if (line.startsWith('## ')) { close(); html += `<h4>${searchable(line.slice(3))}</h4>`; continue; }
    if (line.startsWith('- ')) { if (!list) { html += '<ul>'; list = true; } html += `<li>${linked(line.slice(2))}</li>`; continue; }
    close();
    html += `<p>${linked(line)}</p>`;
  }
  close();
  return html;
}

function typeTag(type) { return `<span class="tag t-${type}">${TYPE_LABEL[type] || type}</span>`; }

/* ---------- 홈 ---------- */
function homeView(params) {
  const subject = DATA.subjects.includes(params.get('s')) ? params.get('s') : DATA.subjects[0];
  const mode = params.get('m') === 'key' ? 'key' : 'era';
  const cat = params.get('c') || '';
  const q = (o) => '#/?' + new URLSearchParams({ s: subject, m: mode, ...o }).toString();
  try { localStorage.setItem('home', q({ c: cat })); } catch (e) { /* 저장 못 해도 그만 */ }

  let html = '<nav class="tabs">' + DATA.subjects.map(s =>
    `<a href="#/?s=${s}&m=${mode}" class="${s === subject ? 'on' : ''}">${esc(node(s).title)}</a>`).join('') + '</nav>';
  html += `<div class="seg"><a href="${q({ m: 'era' })}" class="${mode === 'era' ? 'on' : ''}">연대별 흐름</a>`
        + `<a href="${q({ m: 'key' })}" class="${mode === 'key' ? 'on' : ''}">핵심 키워드</a></div>`;

  if (mode === 'era') {
    html += `<p class="intro">${linked(node(subject).summary)}</p>`;
    for (const era of children(subject)) {
      html += `<a class="era" href="#/n/${era.id}"><span class="dot"></span><div>
        <div class="period">${esc(era.period)}</div><h3>${esc(era.title)}</h3>
        <p>${esc(plain(era.summary))}</p>
        <div class="topics">${children(era.id).map(t => esc(t.title)).join(' · ')}</div></div></a>`;
    }
  } else {
    const all = Object.values(DATA.nodes).filter(n => n.type === 'key' && subjectOf(n.id) === subject)
      .sort((a, b) => a.title.localeCompare(b.title, 'ko'));
    const present = new Set(all.map(n => n.category).filter(Boolean));
    const cats = CATEGORY_ORDER.filter(c => present.has(c)).concat([...present].filter(c => !CATEGORY_ORDER.includes(c)).sort());
    html += '<div class="chips">'
      + `<a class="chip ${cat ? '' : 'on'}" href="${q({ m: 'key' })}">전체 ${all.length}</a>`
      + cats.map(c => `<a class="chip ${cat === c ? 'on' : ''}" href="${q({ m: 'key', c: cat === c ? '' : c })}">${esc(c)} ${all.filter(n => n.category === c).length}</a>`).join('')
      + '</div>';
    const shown = cat ? all.filter(n => n.category === cat) : all;
    let group = null;
    for (const k of shown) {
      const ini = initialOf(k.title);
      if (ini !== group) { html += `<div class="initial">${ini}</div>`; group = ini; }
      const era = eraOf(k.id);
      html += `<a class="row" href="#/n/${k.id}"><div class="head"><b>${esc(k.title)}</b>`
        + (k.category ? `<span class="tag cat">${esc(k.category)}</span>` : '')
        + (era ? `<span class="tag cat-era">${esc(era.title)}</span>` : '')
        + `</div><p>${esc(plain(k.summary))}</p></a>`;
    }
  }
  return html;
}

/* ---------- 세부 ---------- */
function nodeView(id) {
  const n = node(id);
  if (!n) return `<p class="empty">항목을 찾을 수 없습니다: ${esc(id)}</p>`;
  const anc = ancestors(id);
  const kids = children(id);
  const mentioned = (backlinks[id] || []).filter(m => m !== n.parent && !n.children.includes(m)).map(node);
  const sibs = n.parent ? node(n.parent).children : [];
  const i = sibs.indexOf(id);
  const prev = node(sibs[i - 1]), next = node(sibs[i + 1]);

  let html = '<article class="detail">';
  if (anc.length) {
    html += '<div class="crumbs">' + anc.map(a =>
      a.type === 'subject' ? `<a href="#/?s=${a.id}">${esc(a.title)}</a>` : `<a href="#/n/${a.id}">${esc(a.title)}</a>`
    ).join('<span>›</span>') + '</div>';
  }
  html += '<div class="tags">' + typeTag(n.type) + (n.category ? `<span class="tag plain">${esc(n.category)}</span>` : '') + '</div>';
  html += `<h1>${searchable(n.title)}</h1>`;
  if (n.period) html += `<div class="period">${esc(n.period)}</div>`;
  if (n.summary) html += `<div class="summary">${linked(n.summary)}</div>`;
  if (n.body) html += `<div class="body">${bodyHtml(n.body)}</div>`;

  if (kids.length) {
    html += `<div class="section">${{ subject: '시대', era: '주요 주제', topic: '핵심 키워드' }[n.type] || '세부 항목'}</div>`;
    for (const c of kids) {
      html += `<a class="card" href="#/n/${c.id}"><div class="head"><b>${esc(c.title)}</b>`
        + (c.period ? `<span class="p">${esc(c.period)}</span>` : '') + `</div>`
        + (c.summary ? `<p>${esc(plain(c.summary))}</p>` : '') + '</a>';
    }
  }
  if (mentioned.length) {
    html += '<div class="section">함께 보기 — 이 항목이 나오는 곳</div><div class="mentions">'
      + mentioned.map(m => `<a href="#/n/${m.id}">${esc(m.title)}</a>`).join('') + '</div>';
  }
  if (prev || next) {
    html += '<div class="pager">'
      + (prev ? `<a href="#/n/${prev.id}">‹ ${esc(prev.title)}</a>` : '<span></span>')
      + (next ? `<a href="#/n/${next.id}">${esc(next.title)} ›</a>` : '<span></span>') + '</div>';
  }
  return html + '</article>';
}

/* ---------- 검색 ---------- */
function searchResults(query) {
  const q = query.trim().toLowerCase();
  if (!q) return '';
  const nodes = Object.values(DATA.nodes).filter(n => n.type !== 'subject');
  const inTitle = nodes.filter(n => n.title.toLowerCase().includes(q)).sort((a, b) => a.title.length - b.title.length);
  const set = new Set(inTitle);
  const inText = nodes.filter(n => !set.has(n) && (plain(n.summary) + '\n' + plain(n.body)).toLowerCase().includes(q));
  const res = inTitle.concat(inText).slice(0, 80);
  if (!res.length) return '<p class="empty">찾는 항목이 없습니다.</p>';
  return res.map(n => `<a class="row" href="#/n/${n.id}"><div class="head">${typeTag(n.type)}`
    + `<span class="path">${esc(ancestors(n.id).map(a => a.title).join(' › '))}</span></div>`
    + `<b>${esc(n.title)}</b><p>${esc(plain(n.summary))}</p></a>`).join('');
}

function searchView(params) {
  const q = params.get('q') || '';
  setTimeout(() => {
    const input = document.getElementById('q');
    const out = document.getElementById('results');
    input.focus();
    input.addEventListener('input', () => {
      out.innerHTML = searchResults(input.value);
      history.replaceState(null, '', '#/search?q=' + encodeURIComponent(input.value));
    });
  });
  return `<div class="search"><input id="q" type="search" enterkeyhint="search" autocomplete="off"
    placeholder="인물 · 사건 · 제도 이름으로 찾기" value="${esc(q)}"></div><div id="results">${searchResults(q)}</div>`;
}

/* ---------- 화면 이동 ---------- */
const scrolls = {};
let current = null;

function render() {
  if (current !== null) scrolls[current] = window.scrollY;
  const hash = location.hash || '#/';
  const [path, query] = hash.slice(1).split('?');
  const params = new URLSearchParams(query || '');
  const isHome = path === '/' || path === '';

  let html;
  if (path.startsWith('/n/')) html = nodeView(decodeURIComponent(path.slice(3)));
  else if (path === '/search') html = searchView(params);
  else html = homeView(params);
  $view.innerHTML = html;

  document.getElementById('back').hidden = isHome;
  document.getElementById('home-btn').hidden = isHome;
  document.getElementById('search-btn').hidden = path === '/search';
  const n = path.startsWith('/n/') && node(decodeURIComponent(path.slice(3)));
  document.title = n ? `${n.title} — Earlln History` : 'Earlln History';

  current = hash;
  window.scrollTo(0, scrolls[hash] || 0);
}

document.getElementById('back').addEventListener('click', () => {
  if (history.length > 1) history.back(); else location.hash = '#/';
});
// 홈 버튼 · 로고: 마지막으로 보던 과목 · 보기로
for (const el of [document.getElementById('home-btn'), document.querySelector('.brand')]) {
  el.addEventListener('click', e => {
    let home = '#/';
    try { home = localStorage.getItem('home') || '#/'; } catch (err) { /* 무시 */ }
    e.preventDefault();
    location.hash = home;
  });
}
// 앞으로 들어가는 링크는 맨 위부터 보이게 (뒤로 가기만 스크롤 위치 복원)
document.addEventListener('click', e => {
  const a = e.target.closest('a[href^="#/"]');
  if (a) delete scrolls[a.getAttribute('href')];
});
window.addEventListener('hashchange', render);

fetch('history.json')
  .then(r => r.json())
  .then(data => {
    DATA = data;
    for (const [id, n] of Object.entries(DATA.nodes)) n.id = id;
    for (const n of Object.values(DATA.nodes)) {
      const seen = new Set();
      for (const m of (n.summary + '\n' + n.body).matchAll(LINK)) {
        const t = m[1].trim();
        if (t !== n.id && !seen.has(t)) { seen.add(t); (backlinks[t] = backlinks[t] || []).push(n.id); }
      }
    }
    if (!location.hash) {
      try { const h = localStorage.getItem('home'); if (h) history.replaceState(null, '', h); } catch (e) { /* 무시 */ }
    }
    render();
  })
  .catch(() => { $view.innerHTML = '<p class="empty">내용을 불러오지 못했습니다. 인터넷 연결을 확인해 주세요.</p>'; });

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));
}
