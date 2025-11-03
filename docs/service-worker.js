// Service Worker for NONE App - Performance Optimized Caching
// Version 1.6.3 - Update this version when you need to force cache refresh

const CACHE_VERSION = 'none-v1.6.3';
const CACHE_NAMES = {
  static: `${CACHE_VERSION}-static`,
  images: `${CACHE_VERSION}-images`,
  fonts: `${CACHE_VERSION}-fonts`,
  external: `${CACHE_VERSION}-external`
};

// Resources to cache immediately on install
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/styles.css',
  '/script.js',
  '/lazy-lottie.js',
  '/manifest.json',
  '/assets/none_white.png',
  '/assets/none_splash.json',
  '/assets/arrows_up.json',
  '/assets/flags/EN.png',
  '/assets/flags/ES.png',
  '/assets/flags/PT.png',
  '/assets/flags/FR.png',
  '/assets/flags/DE.png',
  '/assets/favicon.png',
  '/assets/google-play-badge-en.png',
  '/assets/google-play-badge-es.png',
  '/assets/google-play-badge-pt.png',
  '/assets/google-play-badge-fr.png',
  '/assets/google-play-badge-de.png',
  '/assets/app-store-badge-en.svg',
  '/assets/app-store-badge-es.svg',
  '/assets/app-store-badge-pt.svg',
  '/assets/app-store-badge-fr.svg',
  '/assets/app-store-badge-de.svg'
];

// Note: lottie-player.js is NOT precached - it loads lazily when needed

// Install event - cache core resources
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAMES.static)
      .then((cache) => {
        console.log('[SW] Precaching app shell');
        return cache.addAll(PRECACHE_URLS);
      })
      .then(() => self.skipWaiting())
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            // Delete old caches that don't match current version
            if (cacheName.startsWith('none-v') && !Object.values(CACHE_NAMES).includes(cacheName)) {
              console.log('[SW] Deleting old cache:', cacheName);
              return caches.delete(cacheName);
            }
          })
        );
      })
      .then(() => self.clients.claim())
  );
});

// Fetch event - smart caching strategies
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip non-GET requests
  if (request.method !== 'GET') {
    return;
  }

  // Skip Chrome extensions and other protocols
  if (!url.protocol.startsWith('http')) {
    return;
  }

  // Strategy 1: HTML - Network first, fallback to cache
  if (request.headers.get('accept')?.includes('text/html')) {
    event.respondWith(networkFirstStrategy(request, CACHE_NAMES.static));
    return;
  }

  // Strategy 2: CSS/JS - Cache first, fallback to network (long-term cache)
  if (url.pathname.endsWith('.css') ||
      url.pathname.endsWith('.js') ||
      url.pathname.endsWith('.json')) {
    event.respondWith(cacheFirstStrategy(request, CACHE_NAMES.static));
    return;
  }

  // Strategy 3: Images - Cache first (very long-term cache)
  if (url.pathname.match(/\.(png|jpg|jpeg|svg|gif|webp|ico)$/)) {
    event.respondWith(cacheFirstStrategy(request, CACHE_NAMES.images));
    return;
  }

  // Strategy 4: Fonts - Cache first (immutable, long-term cache)
  if (url.pathname.match(/\.(woff|woff2|ttf|eot)$/) ||
      url.hostname === 'fonts.googleapis.com' ||
      url.hostname === 'fonts.gstatic.com') {
    event.respondWith(cacheFirstStrategy(request, CACHE_NAMES.fonts));
    return;
  }

  // Strategy 5: External resources (YouTube, Google Analytics, etc.) - Stale while revalidate
  if (url.hostname !== self.location.hostname) {
    event.respondWith(staleWhileRevalidateStrategy(request, CACHE_NAMES.external));
    return;
  }

  // Default: Network first with cache fallback
  event.respondWith(networkFirstStrategy(request, CACHE_NAMES.static));
});

// Caching Strategies

/**
 * Cache First Strategy - Best for static assets that rarely change
 * Serves from cache if available, fetches from network if not
 */
async function cacheFirstStrategy(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cachedResponse = await cache.match(request);

  if (cachedResponse) {
    return cachedResponse;
  }

  try {
    const networkResponse = await fetch(request);

    // Cache successful responses (status 200-299)
    if (networkResponse.ok) {
      // Clone the response before caching (response can only be used once)
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.error('[SW] Cache first strategy failed:', error);

    // Return offline fallback if available
    return new Response('Offline - Resource not available', {
      status: 503,
      statusText: 'Service Unavailable'
    });
  }
}

/**
 * Network First Strategy - Best for HTML and content that updates frequently
 * Tries network first, falls back to cache if offline
 */
async function networkFirstStrategy(request, cacheName) {
  const cache = await caches.open(cacheName);

  try {
    const networkResponse = await fetch(request);

    // Cache successful responses
    if (networkResponse.ok) {
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.log('[SW] Network failed, trying cache:', request.url);

    const cachedResponse = await cache.match(request);

    if (cachedResponse) {
      return cachedResponse;
    }

    // Return offline fallback
    return new Response('Offline - Page not available', {
      status: 503,
      statusText: 'Service Unavailable'
    });
  }
}

/**
 * Stale While Revalidate Strategy - Best for external resources
 * Serves cached version immediately while updating cache in background
 */
async function staleWhileRevalidateStrategy(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cachedResponse = await cache.match(request);

  // Fetch from network in the background
  const fetchPromise = fetch(request).then((networkResponse) => {
    if (networkResponse.ok) {
      cache.put(request, networkResponse.clone());
    }
    return networkResponse;
  }).catch((error) => {
    console.log('[SW] Background fetch failed:', error);
  });

  // Return cached response immediately if available, otherwise wait for network
  return cachedResponse || fetchPromise;
}

// Listen for messages from the client
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }

  if (event.data && event.data.type === 'CLEAR_CACHE') {
    event.waitUntil(
      caches.keys().then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => caches.delete(cacheName))
        );
      })
    );
  }
});
