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

// ===================== Sound Manager (零延迟版) =====================
const SoundManager = {
    _muted: localStorage.getItem('soundMuted') === 'true',
    _currentAudio: null,
    _audioCache: {},
    _loaded: {},

    /** 对指定路径进行预加载：创建 Audio 并开始缓冲，但不播放 */
    preload(src) {
        if (this._audioCache[src]) return;
        var a = new Audio(src);
        a.preload = 'auto';
        a.volume = 0.5;
        this._audioCache[src] = a;
        // 主动触发缓冲（部分浏览器需要）
        a.load();
    },

    /** 预加载所有需要的音频（在 init 中被调用） */
    _preloadAll() {
        this.preload('/audio/1.m4a');
        this.preload('/audio/2.m4a');
        this.preload('/audio/3.m4a');
    },

    init() {
        // 尊重无障碍偏好：弱动画使用者默认静音
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            this._muted = true;
            localStorage.setItem('soundMuted', 'true');
        }
        // 尽早开始缓冲所有音频文件
        this._preloadAll();
        this._createToggleButton();
        // 预暖 AudioContext（用户首次交互时）
        var warmup = function() {
            try {
                var ctx = new (window.AudioContext || window.webkitAudioContext)();
                ctx.resume().then(function() { ctx.close(); }).catch(function(){});
            } catch(e) {}
            document.removeEventListener('click', warmup);
            document.removeEventListener('touchstart', warmup);
        };
        document.addEventListener('click', warmup, { once: true });
        document.addEventListener('touchstart', warmup, { once: true });
    },

    isMuted() {
        return this._muted;
    },

    toggleMute() {
        this._muted = !this._muted;
        localStorage.setItem('soundMuted', this._muted ? 'true' : 'false');
        this._updateButtonIcon();
        if (this._currentAudio && !this._currentAudio.paused) {
            this._currentAudio.volume = this._muted ? 0 : 0.5;
        }
        return !this._muted;
    },

    /** 播放已预加载的音频（近乎零延迟） */
    play(src) {
        if (this._muted) return;
        // 停止当前播放
        if (this._currentAudio && !this._currentAudio.paused) {
            this._currentAudio.pause();
            this._currentAudio.currentTime = 0;
        }
        // 如果还没创建（兜底），立即创建并触发加载
        if (!this._audioCache[src]) {
            this.preload(src);
        }
        this._currentAudio = this._audioCache[src];
        this._currentAudio.currentTime = 0;
        this._currentAudio.play().catch(function(err) {
            if (err.name !== 'AbortError') {
                console.debug('Audio play blocked:', err.message);
            }
        });
    },

    _createToggleButton() {
        var btn = document.createElement('button');
        btn.id = 'sound-toggle';
        btn.setAttribute('aria-label', this._muted ? '开启声音' : '静音');
        btn.setAttribute('title', this._muted ? '开启声音' : '静音');
        btn.setAttribute('style',
            'position:fixed;bottom:20px;right:20px;z-index:9999;' +
            'width:44px;height:44px;border-radius:50%;' +
            'background:white;border:2px solid #F97316;cursor:pointer;' +
            'display:flex;align-items:center;justify-content:center;' +
            'box-shadow:0 2px 12px rgba(0,0,0,0.15);' +
            'transition:transform 0.2s ease,box-shadow 0.2s ease;' +
            'color:#F97316;'
        );
        btn.innerHTML = this._getIconHTML();
        btn.addEventListener('click', function() { SoundManager.toggleMute(); });
        btn.addEventListener('mouseenter', function() {
            btn.style.transform = 'scale(1.1)';
            btn.style.boxShadow = '0 4px 16px rgba(249,115,22,0.3)';
        });
        btn.addEventListener('mouseleave', function() {
            btn.style.transform = 'scale(1)';
            btn.style.boxShadow = '0 2px 12px rgba(0,0,0,0.15)';
        });
        document.body.appendChild(btn);
    },

    _getIconHTML() {
        if (this._muted) {
            return '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 5L6 9H2v6h4l5 4V5z"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>';
        }
        return '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 5L6 9H2v6h4l5 4V5z"/><path d="M19.07 4.93a10 10 0 010 14.14"/><path d="M15.54 8.46a5 5 0 010 7.07"/></svg>';
    },

    _updateButtonIcon() {
        var btn = document.getElementById('sound-toggle');
        if (btn) {
            btn.innerHTML = this._getIconHTML();
            btn.setAttribute('aria-label', this._muted ? '开启声音' : '静音');
            btn.setAttribute('title', this._muted ? '开启声音' : '静音');
        }
    }
};

// ===================== 全局辅助函数 =====================

/** 播放音频（各页面可直接调用） */
function playSound(src) {
    SoundManager.play(src);
}

/** 播放音频后延迟提交表单，让音频衔接自然 */
function playSoundAndSubmit(form, src) {
    SoundManager.play(src);
    setTimeout(function() { form.submit(); }, 150);
    return false;
}

/** 播放音频后延迟跳转，让音频衔接自然 */
function playSoundAndNavigate(src, targetUrl) {
    SoundManager.play(src);
    setTimeout(function() { window.location.href = targetUrl; }, 150);
}

// 页面加载完毕初始化 SoundManager（early: 在 DOMContentLoaded 中尽早执行预加载）
document.addEventListener('DOMContentLoaded', function() {
    SoundManager.init();
});
