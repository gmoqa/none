/**
 * Lazy Lottie Loader - Performance Optimized
 * Only loads lottie-player.js when animations are near viewport
 * Reduces initial JavaScript bundle size by ~93KB
 */

(function() {
    'use strict';

    let lottieLoaded = false;
    let lottieLoading = false;

    /**
     * Dynamically load lottie-player script
     */
    function loadLottiePlayer() {
        if (lottieLoaded || lottieLoading) {
            return Promise.resolve();
        }

        lottieLoading = true;

        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = '/lottie-player.js';
            script.defer = true;

            script.onload = () => {
                lottieLoaded = true;
                lottieLoading = false;
                console.log('✅ Lottie player loaded');
                resolve();
            };

            script.onerror = () => {
                lottieLoading = false;
                console.error('❌ Failed to load lottie-player.js');
                reject(new Error('Failed to load lottie-player'));
            };

            document.head.appendChild(script);
        });
    }

    /**
     * Initialize lazy loading for lottie animations
     */
    function initLazyLottie() {
        // Find all lottie-player elements
        const lottieElements = document.querySelectorAll('lottie-player');

        if (lottieElements.length === 0) {
            return; // No animations on this page
        }

        // Check if browser supports Intersection Observer
        if (!('IntersectionObserver' in window)) {
            // Fallback: load immediately for old browsers
            loadLottiePlayer();
            return;
        }

        // Create Intersection Observer
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                // If animation is near viewport (within 200px)
                if (entry.isIntersecting) {
                    // Load lottie-player script
                    loadLottiePlayer().then(() => {
                        // Script loaded, lottie-player will auto-initialize
                        console.log('🎬 Lottie animations ready');
                    });

                    // Stop observing once loaded
                    observer.disconnect();
                }
            });
        }, {
            rootMargin: '200px', // Load 200px before entering viewport
            threshold: 0
        });

        // Observe all lottie elements
        lottieElements.forEach(element => {
            observer.observe(element);
        });
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initLazyLottie);
    } else {
        // DOM already loaded
        initLazyLottie();
    }
})();
