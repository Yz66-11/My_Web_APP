// ===================== Image Lazy Loader =====================
class LazyImageLoader {
    constructor() {
        this.observer = new IntersectionObserver(
            (entries) => this.loadImages(entries),
            { rootMargin: '200px', threshold: 0.01 }
        );
        this.init();
    }

    loadImages(entries) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                const src = img.dataset.src;
                const srcset = img.dataset.srcset;
                if (src) img.src = src;
                if (srcset) img.srcset = srcset;
                img.classList.add('loaded');
                this.observer.unobserve(img);
            }
        });
    }

    init() {
        document.addEventListener('DOMContentLoaded', () => {
            const images = document.querySelectorAll('img[data-src]');
            images.forEach(img => this.observer.observe(img));
        });
    }
}

// ===================== Toast Auto-Dismiss =====================
document.addEventListener('DOMContentLoaded', function() {
    // Auto-dismiss toasts after 4 seconds
    document.querySelectorAll('.toast').forEach(function(toast) {
        setTimeout(function() {
            toast.classList.add('toast-out');
            setTimeout(function() { toast.remove(); }, 300);
        }, 4000);
    });

    // City emoji map (for food-gallery)
    const cityEmojiMap = {
        '北京': '\u{1F3D9}', '上海': '\u{1F3E0}', '广州': '\u{1F3EF}', '深圳': '\u{1F3D6}',
        '成都': '\u{1F377}', '重庆': '\u{1F35B}', '杭州': '\u{1F3D3}', '西安': '\u{1F3F7}',
        '武汉': '\u{1F4A1}', '南京': '\u{1F3F0}', '长沙': '\u{1F525}', '苏州': '\u{1F3DE}',
        '天津': '\u{1F3F4}', '青岛': '\u{26F5}', '大连': '\u{1F30A}', '厦门': '\u{1F334}'
    };
    document.querySelectorAll('[data-city]').forEach(function(el) {
        const city = el.getAttribute('data-city');
        if (cityEmojiMap[city]) el.textContent = cityEmojiMap[city];
    });
});

// ===================== Smooth Scroll (if not reduced motion) =====================
if (window.matchMedia('(prefers-reduced-motion: no-preference)').matches) {
    document.documentElement.style.scrollBehavior = 'smooth';
}

// Initialize lazy loader if there are images
new LazyImageLoader();
