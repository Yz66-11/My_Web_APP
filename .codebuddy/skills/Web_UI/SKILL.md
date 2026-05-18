---
name: gourmet-app-design
description: |
  美食品鉴Web应用前端设计指南。当用户需要构建美食相关应用、餐厅推荐、食谱展示、美食社区、
  菜品评价、烹饪教程、美食打卡、探店分享等功能时使用此skill。适用于面向年轻群体的、
  追求活泼风格与高级质感平衡的美食类前端界面设计。
---

# 美食品鉴应用前端设计指南

## 设计理念

核心原则：**活力与精致的平衡** —— 既要有年轻人喜欢的活泼感，又要保持美食应有的高级质感。
参考样式：/reference/componentsw.md

### 设计关键词
- 新鲜感（Fresh）
- 食欲感（Appetizing）  
- 轻盈感（Light）
- 温暖感（Warm）
- 流畅感（Fluid）

---

## 色彩系统

### 主色调选择策略

使用「美食色」作为主色 —— 从食材和烹饪中提取灵感：

**方案A: 暖橙调** - 唤起食欲，活力四射
`--primary: 24 95% 53%;` (#F97316)

**方案B: 番茄红** - 热情洋溢，美食经典色
`--primary: 0 84% 60%;` (#EF4444)

**方案C: 抹茶绿** - 清新健康，年轻时尚
`--primary: 142 71% 45%;` (#22C55E)

**方案D: 焦糖棕** - 温暖高级，精致质感
`--primary: 30 80% 45%;` (#CC7722)

### 完整色彩配置

背景使用温暖的米白色，避免冷白：
- `--background: 40 40% 98%;` 暖米白
- `--foreground: 20 14% 15%;` 深咖啡黑
- `--card: 40 30% 99%;` 略带暖调的白
- `--muted: 40 20% 96%;` 温暖的中性色
- `--accent: 45 93% 58%;` 蜂蜜金，用于高亮
- `--border: 30 15% 90%;` 柔和的暖灰

### 色彩原则
1. 背景永远温暖 —— 避免纯白(#FFF)
2. 食物图片主导 —— UI色彩起辅助作用
3. 渐变要克制 —— 选择同色系微妙过渡

---

## 字体排版

推荐使用 `Noto Sans SC` 作为中文正文，`Playfair Display` 作为英文标题增添精致感。

### 字号层级
- Hero标题: `text-4xl md:text-5xl font-bold tracking-tight`
- 区域标题: `text-2xl md:text-3xl font-semibold`
- 卡片标题: `text-lg md:text-xl font-medium`
- 正文: `text-base leading-relaxed`
- 辅助文字: `text-sm text-muted-foreground`

### 排版原则
- 行高宽松（leading-relaxed）
- 中文内容使用 `text-balance` 优化换行
- 价格、评分使用加粗处理

---

## 布局与空间

### 卡片式设计

美食卡片基础结构：
- 图片区占据视觉主导，使用 `aspect-[4/3]`
- 信息区简洁明了，`p-4 space-y-2`
- 悬浮效果：`hover:shadow-lg transition-shadow duration-300`
- 圆角：`rounded-2xl`

### 响应式网格
`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6`

### 留白原则
- 大量留白让美食图片「呼吸」
- 卡片间距 `gap-6` 或 `gap-8`
- 页面边距 `px-4 md:px-8 lg:px-12`

---

## 图片处理

### 图片比例规范
| 场景 | 比例 | 用途 |
|------|------|------|
| 菜品卡片 | 4:3 | 列表展示 |
| 详情大图 | 16:9 | 详情页头图 |
| 正方形 | 1:1 | 头像、缩略图 |
| 竖版 | 3:4 | 探店打卡、故事流 |

### 图片最佳实践
- 使用 Next.js Image 组件优化
- 添加底部渐变遮罩让文字可读
- 骨架屏 + 模糊占位提升加载体验
- 悬浮时微缩放：`group-hover:scale-105 transition-transform duration-500`

---

## 交互与动效

### 微交互原则
年轻用户期待有趣但不干扰的交互反馈：
- 点赞按钮：`active:scale-90` 弹性按压
- 收藏状态：`fill-red-500 scale-110` 视觉反馈
- 按钮悬浮：`hover:shadow-lg hover:-translate-y-1`

### 常用动效
- 卡片悬浮：`transition-all duration-300`
- 图片缩放：`transition-transform duration-500`
- 渐入动画：使用 framer-motion 实现滚动渐入

---

## 组件设计模式

### 评分展示
星级评分使用半星支持，颜色 `fill-amber-400 text-amber-400`

### 标签系统
圆角胶囊样式：`rounded-full text-xs font-medium px-2.5 py-0.5`
变体：default/spicy(红)/vegan(绿)/new(主色)

### 价格展示
- 现价突出：`text-xl font-bold text-primary`
- 原价划线：`text-sm text-muted-foreground line-through`

---

## 导航结构

### 底部导航栏（移动端首选）
- 固定底部：`fixed bottom-0 inset-x-0`
- 毛玻璃效果：`bg-background/80 backdrop-blur-lg`
- 五项导航：发现/搜索/发布/收藏/我的

### 顶部筛选条
- 粘性定位：`sticky top-0 z-10`
- 横向滚动：`overflow-x-auto scrollbar-hide`
- 胶囊按钮：`rounded-full` 选中态使用主色

---

## 年轻化细节

### 空状态设计
使用有趣的图标和温暖的文案，避免冰冷的提示

### Toast提示
简短有趣的反馈文案

### 加载状态
骨架屏保持布局稳定，使用 `animate-pulse`

---

## 可访问性

- 所有美食图片添加描述性 alt 文本
- 颜色对比度满足 WCAG 2.1 AA 标准
- 可交互元素有明确的 focus 状态
- 触摸目标最小 44x44px

---

## 禁止事项

- 不使用纯白背景
- 不滥用渐变
- 不使用低质量图片
- 不过度使用动画
- 不使用过小的字号（移动端至少14px）
- 不忽视暗色模式
- 不使用emoji作为正式图标的替代