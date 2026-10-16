/* 오프라인용: 앱 파일과 내용을 캐시해 두고, 새 버전이 올라오면 캐시를 갈아 끼운다. */
const CACHE = 'earlln-history-__VERSION__-__BUILD__';
const FILES = [
  './', 'index.html', 'style.css?v=__BUILD__', 'app.js?v=__BUILD__', 'history.json',
  'manifest.webmanifest', 'icons/apple-touch-icon.png', 'icons/icon-192.png', 'icons/icon-512.png',
];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

// 같은 사이트 파일만: 먼저 네트워크, 안 되면 캐시 (구글 검색 등 바깥 주소는 건드리지 않음)
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== location.origin) return;
  e.respondWith(
    fetch(e.request)
      .then(res => {
        const copy = res.clone();
        caches.open(CACHE).then(c => c.put(e.request, copy));
        return res;
      })
      .catch(() => caches.match(e.request, { ignoreSearch: false }).then(r => r || caches.match('index.html')))
  );
});
