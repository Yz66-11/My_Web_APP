# 美食应用组件代码参考

## 美食卡片组件

```tsx
import Image from "next/image"
import { Card, CardContent } from "@/components/ui/card"
import { Star, Heart } from "lucide-react"
import { cn } from "@/lib/utils"

interface DishCardProps {
  dish: {
    id: string
    name: string
    description: string
    image: string
    price: number
    rating: number
  }
  onFavorite?: (id: string) => void
  isFavorited?: boolean
}

export function DishCard({ dish, onFavorite, isFavorited }: DishCardProps) {
  return (
    <Card className="group overflow-hidden rounded-2xl border-0 shadow-sm hover:shadow-lg transition-all duration-300">
      <div className="relative aspect-[4/3] overflow-hidden">
        <Image
          src={dish.image}
          alt={dish.name}
          fill
          sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
          className="object-cover group-hover:scale-105 transition-transform duration-500"
        />
        <button
          onClick={() => onFavorite?.(dish.id)}
          aria-label={isFavorited ? "取消收藏" : "添加收藏"}
          className="absolute top-3 right-3 p-2 rounded-full bg-white/80 backdrop-blur-sm hover:bg-white transition-colors"
        >
          <Heart
            className={cn(
              "w-5 h-5 transition-all",
              isFavorited ? "fill-red-500 text-red-500" : "text-muted-foreground"
            )}
          />
        </button>
      </div>
      <CardContent className="p-4 space-y-2">
        <h3 className="font-medium text-lg line-clamp-1">{dish.name}</h3>
        <p className="text-muted-foreground text-sm line-clamp-2">{dish.description}</p>
        <div className="flex items-center justify-between pt-1">
          <span className="text-primary font-bold text-lg">?{dish.price}</span>
          <div className="flex items-center gap-1">
            <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
            <span className="text-sm font-medium">{dish.rating.toFixed(1)}</span>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
```
## 星级评分组件

```tsx
import { Star } from "lucide-react"
import { cn } from "@/lib/utils"

interface RatingStarsProps {
  rating: number
  size?: "sm" | "md" | "lg"
  showValue?: boolean
  interactive?: boolean
  onChange?: (rating: number) => void
}

export function RatingStars({
  rating,
  size = "sm",
  showValue = true,
  interactive = false,
  onChange,
}: RatingStarsProps) {
  const fullStars = Math.floor(rating)
  const hasHalf = rating % 1 >= 0.5

  const sizeClasses = {
    sm: "w-4 h-4",
    md: "w-5 h-5",
    lg: "w-6 h-6",
  }

  return (
    <div className="flex items-center gap-0.5">
      {[...Array(5)].map((_, i) => (
        <button
          key={i}
          type="button"
          disabled={!interactive}
          onClick={() => interactive && onChange?.(i + 1)}
          className={cn(
            interactive && "cursor-pointer hover:scale-110 transition-transform"
          )}
        >
          <Star
            className={cn(
              sizeClasses[size],
              "transition-colors",
              i < fullStars
                ? "fill-amber-400 text-amber-400"
                : i === fullStars && hasHalf
                ? "fill-amber-400/50 text-amber-400"
                : "text-muted-foreground/30"
            )}
          />
        </button>
      ))}
      {showValue && (
        <span className="ml-1.5 text-sm font-medium">{rating.toFixed(1)}</span>
      )}
    </div>
  )
}
```
## 美食标签组件

```tsx
import { cn } from "@/lib/utils"

type TagVariant = "default" | "spicy" | "vegan" | "new" | "popular" | "discount"

interface FoodTagProps {
  label: string
  variant?: TagVariant
  size?: "sm" | "md"
}

const variantStyles: Record<TagVariant, string> = {
  default: "bg-muted text-muted-foreground",
  spicy: "bg-red-100 text-red-600",
  vegan: "bg-green-100 text-green-600",
  new: "bg-primary/10 text-primary",
  popular: "bg-amber-100 text-amber-700",
  discount: "bg-rose-100 text-rose-600",
}

export function FoodTag({ label, variant = "default", size = "sm" }: FoodTagProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full font-medium",
        size === "sm" ? "px-2 py-0.5 text-xs" : "px-3 py-1 text-sm",
        variantStyles[variant]
      )}
    >
      {label}
    </span>
  )
}
```

## 底部导航栏
```tsx
import { cn } from "@/lib/utils"

type TagVariant = "default" | "spicy" | "vegan" | "new" | "popular" | "discount"

interface FoodTagProps {
  label: string
  variant?: TagVariant
  size?: "sm" | "md"
}

const variantStyles: Record<TagVariant, string> = {
  default: "bg-muted text-muted-foreground",
  spicy: "bg-red-100 text-red-600",
  vegan: "bg-green-100 text-green-600",
  new: "bg-primary/10 text-primary",
  popular: "bg-amber-100 text-amber-700",
  discount: "bg-rose-100 text-rose-600",
}

export function FoodTag({ label, variant = "default", size = "sm" }: FoodTagProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full font-medium",
        size === "sm" ? "px-2 py-0.5 text-xs" : "px-3 py-1 text-sm",
        variantStyles[variant]
      )}
    >
      {label}
    </span>
  )
}
```

## 筛选标签栏
```tsx
"use client"

import { cn } from "@/lib/utils"

interface Filter {
  id: string
  label: string
}

interface FilterTabsProps {
  filters: Filter[]
  activeId: string
  onChange: (id: string) => void
}

export function FilterTabs({ filters, activeId, onChange }: FilterTabsProps) {
  return (
    <div className="sticky top-0 z-10 bg-background/80 backdrop-blur-lg py-3 -mx-4 px-4">
      <div className="flex gap-2 overflow-x-auto scrollbar-hide">
        {filters.map((filter) => (
          <button
            key={filter.id}
            onClick={() => onChange(filter.id)}
            className={cn(
              "flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium transition-all",
              activeId === filter.id
                ? "bg-primary text-primary-foreground shadow-sm"
                : "bg-muted text-muted-foreground hover:bg-muted/80"
            )}
          >
            {filter.label}
          </button>
        ))}
      </div>
    </div>
  )
}
```

## 骨架屏组件
```tsx
import { cn } from "@/lib/utils"

export function DishCardSkeleton() {
  return (
    <div className="rounded-2xl overflow-hidden bg-card">
      <div className="aspect-[4/3] bg-muted animate-pulse" />
      <div className="p-4 space-y-3">
        <div className="h-5 bg-muted rounded animate-pulse w-2/3" />
        <div className="h-4 bg-muted rounded animate-pulse w-full" />
        <div className="flex justify-between items-center pt-1">
          <div className="h-5 bg-muted rounded animate-pulse w-16" />
          <div className="h-4 bg-muted rounded animate-pulse w-12" />
        </div>
      </div>
    </div>
  )
}

export function DishGridSkeleton({ count = 6 }: { count?: number }) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      {Array.from({ length: count }).map((_, i) => (
        <DishCardSkeleton key={i} />
      ))}
    </div>
  )
}
```

## 空状态组件
```tsx
import { UtensilsCrossed, Search, MessageSquare, Bookmark } from "lucide-react"
import { Button } from "@/components/ui/button"

type EmptyType = "favorites" | "search" | "reviews" | "dishes"

interface EmptyStateProps {
  type: EmptyType
  action?: {
    label: string
    onClick: () => void
  }
}

const emptyContent: Record<EmptyType, { icon: typeof UtensilsCrossed; title: string; description: string }> = {
  favorites: {
    icon: Bookmark,
    title: "收藏夹还是空的",
    description: "发现好吃的就点个收藏吧！",
  },
  search: {
    icon: Search,
    title: "没有找到相关美食",
    description: "换个关键词试试？",
  },
  reviews: {
    icon: MessageSquare,
    title: "还没有评价",
    description: "来做第一个点评的人吧！",
  },
  dishes: {
    icon: UtensilsCrossed,
    title: "暂无美食",
    description: "稍后再来看看吧",
  },
}

export function EmptyState({ type, action }: EmptyStateProps) {
  const { icon: Icon, title, description } = emptyContent[type]

  return (
    <div className="flex flex-col items-center justify-center py-16 text-center px-4">
      <div className="w-16 h-16 rounded-full bg-muted flex items-center justify-center mb-4">
        <Icon className="w-8 h-8 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-medium mb-2">{title}</h3>
      <p className="text-muted-foreground text-sm max-w-xs">{description}</p>
      {action && (
        <Button onClick={action.onClick} className="mt-6">
          {action.label}
        </Button>
      )}
    </div>
  )
}
```

## 价格展示组件
```tsx
import { cn } from "@/lib/utils"

interface PriceProps {
  value: number
  original?: number
  size?: "sm" | "md" | "lg"
  className?: string
}

export function Price({ value, original, size = "md", className }: PriceProps) {
  const sizeClasses = {
    sm: "text-base",
    md: "text-xl",
    lg: "text-2xl",
  }

  return (
    <div className={cn("flex items-baseline gap-2", className)}>
      <span className={cn("font-bold text-primary", sizeClasses[size])}>
        ?{value}
      </span>
      {original && original > value && (
        <span className="text-sm text-muted-foreground line-through">
          ?{original}
        </span>
      )}
    </div>
  )
}
```

## 全局样式配置
```tsx
/* globals.css 美食应用主题 */
@layer base {
  :root {
    --background: 40 40% 98%;
    --foreground: 20 14% 15%;
    --card: 40 30% 99%;
    --card-foreground: 20 14% 15%;
    --popover: 40 30% 99%;
    --popover-foreground: 20 14% 15%;
    --primary: 24 95% 53%;
    --primary-foreground: 0 0% 100%;
    --secondary: 40 20% 96%;
    --secondary-foreground: 20 14% 15%;
    --muted: 40 20% 96%;
    --muted-foreground: 25 10% 45%;
    --accent: 45 93% 58%;
    --accent-foreground: 20 14% 15%;
    --destructive: 0 84% 60%;
    --destructive-foreground: 0 0% 100%;
    --border: 30 15% 90%;
    --input: 30 15% 90%;
    --ring: 24 95% 53%;
    --radius: 0.75rem;
  }

  .dark {
    --background: 20 14% 10%;
    --foreground: 40 20% 96%;
    --card: 20 14% 12%;
    --card-foreground: 40 20% 96%;
    --popover: 20 14% 12%;
    --popover-foreground: 40 20% 96%;
    --primary: 24 95% 53%;
    --primary-foreground: 0 0% 100%;
    --secondary: 20 14% 18%;
    --secondary-foreground: 40 20% 96%;
    --muted: 20 14% 18%;
    --muted-foreground: 40 10% 55%;
    --accent: 45 93% 58%;
    --accent-foreground: 20 14% 10%;
    --border: 20 14% 20%;
    --input: 20 14% 20%;
    --ring: 24 95% 53%;
  }
}

/* 隐藏滚动条 */
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}

/* 安全区域适配 */
.pb-safe {
  padding-bottom: env(safe-area-inset-bottom);
}

/* 渐入动画 */
@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in-up {
  animation: fade-in-up 0.4s ease-out;
}
```