// ===================================
// NONE Landing Page - JavaScript
// ===================================

// ===================================
// ANIMACIONES AL SCROLL
// ===================================
const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.style.opacity = '1';
            entry.target.style.transform = 'translateY(0)';
        }
    });
}, observerOptions);

// Elementos a animar
const animateElements = document.querySelectorAll('.feature-card');

animateElements.forEach(element => {
    element.style.opacity = '0';
    element.style.transform = 'translateY(20px)';
    element.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
    observer.observe(element);
});

// ===================================
// SMOOTH SCROLL PARA ENLACES INTERNOS
// ===================================
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        const href = this.getAttribute('href');

        // Ignorar # solo (para placeholders)
        if (href === '#') {
            e.preventDefault();
            return;
        }

        const target = document.querySelector(href);

        if (target) {
            e.preventDefault();

            const targetPosition = target.getBoundingClientRect().top + window.pageYOffset - 20;

            window.scrollTo({
                top: targetPosition,
                behavior: 'smooth'
            });
        }
    });
});

// ===================================
// LAZY LOADING DE IMÁGENES
// ===================================
const lazyImages = document.querySelectorAll('img[data-src]');

const imageObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            const img = entry.target;
            img.src = img.dataset.src;
            img.removeAttribute('data-src');
            imageObserver.unobserve(img);
        }
    });
});

lazyImages.forEach(img => imageObserver.observe(img));

// ===================================
// GOOGLE ANALYTICS - EVENT TRACKING
// ===================================
function trackEvent(eventName, eventData) {
    if (typeof gtag === 'function') {
        gtag('event', eventName, eventData);
    }
}

// Clicks en botones de descarga
document.querySelectorAll('a[href*="play.google"], a[href*="github.com/gmoqa/none/releases"]').forEach(link => {
    link.addEventListener('click', () => {
        const linkType = link.href.includes('play.google') ? 'Google Play' : 'GitHub Releases';
        trackEvent('download_click', {
            'event_category': 'engagement',
            'event_label': linkType,
            'value': 1
        });
    });
});

// Scroll depth tracking
let scrollTracked = {
    '25': false,
    '50': false,
    '75': false,
    '100': false
};

window.addEventListener('scroll', () => {
    const scrollPercent = Math.round((window.scrollY + window.innerHeight) / document.body.scrollHeight * 100);

    if (scrollPercent >= 25 && !scrollTracked['25']) {
        scrollTracked['25'] = true;
        trackEvent('scroll_depth', {
            'event_category': 'engagement',
            'event_label': '25%',
            'value': 25
        });
    }
    if (scrollPercent >= 50 && !scrollTracked['50']) {
        scrollTracked['50'] = true;
        trackEvent('scroll_depth', {
            'event_category': 'engagement',
            'event_label': '50%',
            'value': 50
        });
    }
    if (scrollPercent >= 75 && !scrollTracked['75']) {
        scrollTracked['75'] = true;
        trackEvent('scroll_depth', {
            'event_category': 'engagement',
            'event_label': '75%',
            'value': 75
        });
    }
    if (scrollPercent >= 90 && !scrollTracked['100']) {
        scrollTracked['100'] = true;
        trackEvent('scroll_depth', {
            'event_category': 'engagement',
            'event_label': '100%',
            'value': 100
        });
    }
}, { passive: true });

// Clicks en enlaces externos
document.querySelectorAll('a[target="_blank"]').forEach(link => {
    link.addEventListener('click', () => {
        trackEvent('external_link_click', {
            'event_category': 'engagement',
            'event_label': link.href,
            'value': 1
        });
    });
});

// ===================================
// MANEJO DE ERRORES DE IMÁGENES
// ===================================
document.querySelectorAll('img').forEach(img => {
    img.addEventListener('error', function() {
        this.style.backgroundColor = '#e2e8f0';
        this.style.minHeight = '200px';
        this.alt = 'Imagen no disponible';
    });
});

// ===================================
// LANGUAGE SELECTOR - Accessible
// ===================================
const languageButton = document.getElementById('languageButton');
const languageDropdown = document.getElementById('languageDropdown');

if (languageButton && languageDropdown) {
    const menuItems = languageDropdown.querySelectorAll('[role="menuitem"]');
    let currentIndex = -1;

    function openDropdown() {
        languageDropdown.style.opacity = '1';
        languageDropdown.style.visibility = 'visible';
        languageDropdown.style.transform = 'translateY(0)';
        languageButton.setAttribute('aria-expanded', 'true');
        currentIndex = -1;
    }

    function closeDropdown() {
        languageDropdown.style.opacity = '0';
        languageDropdown.style.visibility = 'hidden';
        languageDropdown.style.transform = 'translateY(-8px)';
        languageButton.setAttribute('aria-expanded', 'false');
        currentIndex = -1;
    }

    function toggleDropdown() {
        const isExpanded = languageButton.getAttribute('aria-expanded') === 'true';
        if (isExpanded) {
            closeDropdown();
        } else {
            openDropdown();
        }
    }

    // Toggle dropdown on click
    languageButton.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleDropdown();
    });

    // Keyboard navigation
    languageButton.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            toggleDropdown();
        } else if (e.key === 'Escape') {
            closeDropdown();
            languageButton.focus();
        } else if (e.key === 'ArrowDown') {
            e.preventDefault();
            openDropdown();
            currentIndex = 0;
            menuItems[currentIndex].focus();
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            openDropdown();
            currentIndex = menuItems.length - 1;
            menuItems[currentIndex].focus();
        }
    });

    // Navigate within dropdown with arrow keys
    menuItems.forEach((item, index) => {
        item.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                currentIndex = (index + 1) % menuItems.length;
                menuItems[currentIndex].focus();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                currentIndex = index - 1 < 0 ? menuItems.length - 1 : index - 1;
                menuItems[currentIndex].focus();
            } else if (e.key === 'Escape') {
                e.preventDefault();
                closeDropdown();
                languageButton.focus();
            } else if (e.key === 'Tab') {
                closeDropdown();
            }
        });
    });

    // Close dropdown when clicking outside
    document.addEventListener('click', (e) => {
        if (!languageButton.contains(e.target) && !languageDropdown.contains(e.target)) {
            closeDropdown();
        }
    });
}
