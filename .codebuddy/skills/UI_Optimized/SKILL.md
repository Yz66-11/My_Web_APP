---
name: frontend-food-gallery-style
description: >
  美食图鉴类 Web 应用前端风格与性能优化 Skill。
  提供完整的设计系统（色彩、字体、阴影）、组件规范（卡片、网格）、
  性能优化方案（懒加载、虚拟滚动、骨架屏）、响应式布局及性能监控指南。
  适用于需要构建高质量图片展示类 Web 应用的开发者。
  关键词：前端风格、CSS 设计系统、性能优化、懒加载、响应式、美食图鉴、卡片组件。
---

# 美食图鉴 Web 应用 - 前端风格与性能优化

## 使用场景

当需要构建**图片展示类 Web 应用**（如美食图鉴、商品展示、相册等）时，使用本 Skill 获取：
- 完整的设计系统规范（色彩、字体、阴影）
- 高性能组件实现方案
- 图片加载与渲染性能优化策略
- 响应式布局最佳实践

---

## 一、设计系统

### 1.1 色彩体系

```css
:root {
  /* 暖色调主色 */
  --color-primary: #e65c00;
  --color-primary-light: #ff8c42;
  --color-primary-dark: #b34500;

  /* 中性色 */
  --color-bg: #fffbf5;        /* 暖白背景 */
  --color-surface: #ffffff;   /* 卡片背景 */
  --color-text: #2d2d2d;      /* 主文字 */
  --color-text-secondary: #6b6b6b;
  --color-border: #f0ebe3;

  /* 功能色 */
  --color-success: #2ecc71;
  --color-warning: #f39c12;
  --color-error: #e74c3c;
}
```

**使用规则**：
- 背景：主背景 `#fffbf5`，模态框纯白
- 文字：正文 `#2d2d2d`，辅助文字 `#6b6b6b`
- 强调：主色用于按钮、评分、标签等关键元素
- 留白：内容边距至少 24px

### 1.2 字体系统

```css
:root {
  --font-family: 'Inter', -apple-system, BlinkMacSystemFont, 
                 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
  --font-family-display: 'Playfair Display', var(--font-family);

  /* 字体大小 */
  --text-xs: 0.75rem;    /* 12px */
  --text-sm: 0.875rem;   /* 14px */
  --text-base: 1rem;     /* 16px */
  --text-lg: 1.125rem;   /* 18px */
  --text-xl: 1.25rem;    /* 20px */
  --text-2xl: 1.5rem;    /* 24px */
  --text-3xl: 1.875rem;  /* 30px */

  /* 字重 */
  --font-normal: 400;
  --font-medium: 500;
  --font-semibold: 600;
  --font-bold: 700;
}
```

### 1.3 阴影系统

```css
:root {
  /* 轻量悬浮 - 普通卡片 */
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.04), 
               0 4px 16px rgba(0,0,0,0.02);

  /* 中等悬浮 - 悬停状态 */
  --shadow-md: 0 8px 24px rgba(0,0,0,0.06),
               0 2px 4px rgba(0,0,0,0.02);

  /* 重度悬浮 - 模态框 */
  --shadow-lg: 0 20px 40px rgba(0,0,0,0.1),
               0 4px 12px rgba(0,0,0,0.06);

  /* 对话框层级 */
  --shadow-xl: 0 30px 60px rgba(0,0,0,0.12);
}
```

---

## 二、核心组件规范

### 2.1 美食卡片组件

**HTML 结构**：

```html
<article class="dish-card">
  <div class="dish-card__image-wrapper">
    <picture>
      <source type="image/webp" srcset="dish.webp">
      <img 
        class="dish-card__image"
        src="dish.jpg"
        alt="菜品名称"
        loading="lazy"
        decoding="async"
        width="300"
        height="225"
      >
    </picture>
    <span class="dish-card__badge">🔥 招牌菜</span>
  </div>

  <div class="dish-card__content">
    <h3 class="dish-card__title">菜品名称</h3>
    <div class="dish-card__meta">
      <span class="dish-card__rating">
        <span class="star">⭐</span> 4.8
      </span>
      <span class="dish-card__time">⏱️ 30min</span>
      <span class="dish-card__price">¥68</span>
    </div>
    <p class="dish-card__description">
      简短诱人的菜品描述...
    </p>
    <button class="dish-card__button" aria-label="收藏">
      ♡ 收藏
    </button>
  </div>
</article>
```

**CSS 样式（性能优化版）**：

```css
.dish-card {
  /* GPU 加速 */
  transform: translateZ(0);
  backface-visibility: hidden;

  /* 布局隔离 */
  contain: layout style paint;

  /* 视觉样式 */
  background: var(--color-surface);
  border-radius: 20px;
  box-shadow: var(--shadow-sm);

  /* 只过渡高性能属性 */
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  will-change: transform;
}

.dish-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

/* 图片容器 - 固定宽高比 */
.dish-card__image-wrapper {
  position: relative;
  width: 100%;
  padding-bottom: 75%;
  overflow: hidden;
  border-radius: 16px;
}

.dish-card__image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.dish-card:hover .dish-card__image {
  transform: scale(1.05);
}

/* 徽章样式 */
.dish-card__badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  background: rgba(0,0,0,0.7);
  backdrop-filter: blur(4px);
  color: white;
  border-radius: 20px;
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
}

/* 内容区域 */
.dish-card__content {
  padding: 16px;
}

.dish-card__title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  margin-bottom: 8px;
  color: var(--color-text);
}

.dish-card__meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.dish-card__description {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-bottom: 16px;

  /* 限制行数 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
```

### 2.2 网格布局

```css
.food-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .food-grid {
    gap: 16px;
    padding: 16px;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}
```

---

## 三、性能优化

### 3.1 图片懒加载

**原生 Intersection Observer 实现**：

```javascript
class LazyImageLoader {
  constructor() {
    this.observer = new IntersectionObserver(
      (entries) => this.loadImages(entries),
      {
        rootMargin: '200px',
        threshold: 0.01
      }
    );
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

  observe(images) {
    images.forEach(img => this.observer.observe(img));
  }
}

// 使用示例
const lazyLoader = new LazyImageLoader();
const images = document.querySelectorAll('img[data-src]');
lazyLoader.observe(images);
```

**现代图片格式适配**：

```html
<picture>
  <source 
    type="image/avif" 
    srcset="dish-small.avif 300w, dish-medium.avif 600w"
    sizes="(max-width: 768px) 50vw, 300px"
  >
  <source 
    type="image/webp"
    srcset="dish-small.webp 300w, dish-medium.webp 600w"
    sizes="(max-width: 768px) 50vw, 300px"
  >
  <img 
    src="dish-small.jpg"
    loading="lazy"
    decoding="async"
    width="300"
    height="225"
    alt="菜品描述"
  >
</picture>
```

### 3.2 虚拟滚动（长列表）

**React 示例**：

```javascript
import { FixedSizeGrid as Grid } from 'react-window';

const FoodGrid = ({ foods }) => {
  const columnCount = 3;
  const rowCount = Math.ceil(foods.length / columnCount);

  const Cell = ({ columnIndex, rowIndex, style }) => {
    const index = rowIndex * columnCount + columnIndex;
    const food = foods[index];

    if (!food) return null;

    return (
      <div style={style}>
        <FoodCard food={food} />
      </div>
    );
  };

  return (
    <Grid
      columnCount={columnCount}
      columnWidth={320}
      height={600}
      rowCount={rowCount}
      rowHeight={400}
      width={1000}
    >
      {Cell}
    </Grid>
  );
};
```

### 3.3 CSS 性能优化

```css
/* 1. 使用 CSS containment */
.dish-card {
  contain: layout style paint;
}

/* 2. 避免触发重排的属性 */
/* ❌ 避免 */
.element:hover {
  width: 200px;
  margin-left: 10px;
}

/* ✅ 推荐 */
.element:hover {
  transform: translateX(10px);
}

/* 3. 动画只使用 transform 和 opacity */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 4. 骨架屏 - 高性能脉冲动画 */
.skeleton {
  background: linear-gradient(
    90deg,
    #f0f0f0 25%,
    #e8e8e8 50%,
    #f0f0f0 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
```

### 3.4 代码分割与懒加载

```javascript
import { lazy, Suspense } from 'react';

// 路由级别代码分割
const FoodDetail = lazy(() => import('./pages/FoodDetail'));
const ReviewList = lazy(() => import('./components/ReviewList'));

function App() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <Router>
        <Route path="/food/:id" component={FoodDetail} />
      </Router>
    </Suspense>
  );
}
```

---

## 四、交互动效

### 4.1 微交互规范

```css
/* 按钮点击反馈 */
.button {
  transition: transform 0.1s ease;
  cursor: pointer;
}

.button:active {
  transform: scale(0.98);
}

/* 收藏动画 */
@keyframes heartBeat {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
}

.favorite-button.active {
  animation: heartBeat 0.3s ease;
  color: #e74c3c;
}

/* 页面转场动画 */
.page-enter {
  opacity: 0;
  transform: translateY(20px);
}

.page-enter-active {
  opacity: 1;
  transform: translateY(0);
  transition: opacity 0.3s, transform 0.3s;
}

.page-exit {
  opacity: 1;
  transform: translateY(0);
}

.page-exit-active {
  opacity: 0;
  transform: translateY(-20px);
  transition: opacity 0.3s, transform 0.3s;
}
```

### 4.2 骨架屏组件

```html
<div class="skeleton-card">
  <div class="skeleton skeleton-image"></div>
  <div class="skeleton skeleton-title"></div>
  <div class="skeleton skeleton-text"></div>
  <div class="skeleton skeleton-text-short"></div>
</div>
```

```css
.skeleton-card {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.skeleton {
  background: linear-gradient(
    90deg,
    #f0f0f0 25%,
    #e8e8e8 50%,
    #f0f0f0 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-image {
  width: 100%;
  padding-bottom: 75%;
}

.skeleton-title {
  height: 24px;
  margin: 16px;
  width: 60%;
  border-radius: 4px;
}

.skeleton-text {
  height: 16px;
  margin: 12px 16px;
  width: 90%;
  border-radius: 4px;
}

.skeleton-text-short {
  height: 16px;
  margin: 12px 16px;
  width: 70%;
  border-radius: 4px;
}
```

---

## 五、响应式设计

### 5.1 断点设置

```css
:root {
  --breakpoint-sm: 640px;
  --breakpoint-md: 768px;
  --breakpoint-lg: 1024px;
  --breakpoint-xl: 1280px;
}
```

### 5.2 移动端优化

```css
@media (max-width: 768px) {
  .food-grid {
    display: flex;
    overflow-x: auto;
    scroll-snap-type: x mandatory;
    gap: 16px;
    padding: 16px;
    scrollbar-width: thin;
  }

  .dish-card {
    flex: 0 0 85%;
    scroll-snap-align: start;
  }

  /* 增加点击区域 */
  .dish-card__button,
  .favorite-button {
    min-height: 44px;
    min-width: 44px;
  }

  /* 底部操作栏 */
  .mobile-action-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: rgba(255,255,255,0.95);
    backdrop-filter: blur(10px);
    padding: 12px 16px;
    display: flex;
    justify-content: space-around;
    border-top: 1px solid var(--color-border);
    z-index: 100;
  }
}
```

---

## 六、性能监控

### 6.1 核心 Web Vitals 监控

```javascript
class PerformanceMonitor {
  constructor() {
    this.metrics = {};
  }

  monitorLCP() {
    const observer = new PerformanceObserver((list) => {
      const entries = list.getEntries();
      const lastEntry = entries[entries.length - 1];
      this.metrics.lcp = lastEntry.startTime;
      console.log(`LCP: ${lastEntry.startTime}ms`);
    });
    observer.observe({ entryTypes: ['largest-contentful-paint'] });
  }

  monitorFID() {
    const observer = new PerformanceObserver((list) => {
      const entries = list.getEntries();
      const firstEntry = entries[0];
      this.metrics.fid = firstEntry.processingStart - firstEntry.startTime;
      console.log(`FID: ${this.metrics.fid}ms`);
    });
    observer.observe({ entryTypes: ['first-input'] });
  }

  monitorCLS() {
    let clsValue = 0;
    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (!entry.hadRecentInput) {
          clsValue += entry.value;
        }
      }
      this.metrics.cls = clsValue;
      console.log(`CLS: ${clsValue}`);
    });
    observer.observe({ entryTypes: ['layout-shift'] });
  }

  report() {
    if (window.gtag) {
      window.gtag('event', 'performance_metrics', {
        lcp: Math.round(this.metrics.lcp),
        fid: Math.round(this.metrics.fid),
        cls: this.metrics.cls
      });
    }
  }
}

// 初始化监控
const monitor = new PerformanceMonitor();
monitor.monitorLCP();
monitor.monitorFID();
monitor.monitorCLS();

// 页面关闭前上报
window.addEventListener('beforeunload', () => {
  monitor.report();
});
```

### 6.2 Lighthouse 优化检查清单

- [ ] 图片使用懒加载和现代格式（WebP/AVIF）
- [ ] CSS 只过渡 transform 和 opacity
- [ ] 关键 CSS 内联，非关键异步加载
- [ ] 使用字体 display: swap
- [ ] 减少未使用的 JavaScript
- [ ] 启用文本压缩（Gzip/Brotli）
- [ ] 设置合适的缓存策略
- [ ] 避免过大的 DOM 树（< 1500 节点）
- [ ] 使用被动事件监听器优化滚动
- [ ] 减少第三方脚本的使用

---

## 七、实施优先级

| 阶段 | 优先级 | 内容 |
|------|--------|------|
| 第1周 | P0 | 字体系统、阴影规范、卡片组件重构、基础悬停效果 |
| 第2-3周 | P1 | 图片懒加载、骨架屏、响应式布局、移动端交互优化 |
| 第4周 | P2 | 虚拟滚动、代码分割、性能监控、CDN 与缓存策略 |

---

## 八、工具与资源

| 类型 | 推荐工具 |
|------|----------|
| 性能测试 | Lighthouse, WebPageTest |
| 图片优化 | Squoosh, ImageOptim |
| 字体优化 | Google Fonts, Font Squirrel |
| 图标库 | Lucide Icons, Heroicons |
| 动画库 | Framer Motion, Anime.js |

---

## 九、浏览器兼容性

- Chrome ≥ 60
- Firefox ≥ 55
- Safari ≥ 12
- Edge ≥ 79

**Polyfills（如需支持旧版浏览器）**：

```javascript
import 'intersection-observer';
import 'resize-observer-polyfill';
import 'css-vars-ponyfill';

cssVars({ watch: true });
```

---

## 故障排查速查

| 问题 | 排查方向 |
|------|----------|
| 布局偏移 | 检查图片是否设置宽高属性 |
| 滚动卡顿 | 检查是否触发强制同步布局 |
| 内存泄漏 | 检查 Intersection Observer 是否正确断开 |
| 白屏时间过长 | 检查关键 CSS 是否内联 |
