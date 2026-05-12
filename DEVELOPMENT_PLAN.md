# 美食打卡点评系统 — 软件开发全流程计划

## 一、项目概述

| 项 | 内容 |
|---|---|
| **项目名称** | 美食打卡点评系统 |
| **技术栈** | Spring Boot 3.5.5 + Thymeleaf + Spring Security + JPA + MySQL + Tailwind CSS |
| **团队** | 102B 小组：梁佑泽、吴开泰、黄玉康、黄翔宇 |
| **参考文档** | 需求分析报告、需求获取报告、开题报告 |

---

## 二、需求摘要（从文档提炼）

### 2.1 用户角色
| 角色 | 说明 |
|---|---|
| **普通用户 (Custom)** | 注册、登录、打卡、评价、发帖、图鉴收集、好友交互 |
| **商家 (Merchant)** | 入驻申请、管理店铺/菜品、查看评价、审核许可状态 |
| **管理员 (Administrator)** | 审核评价、审核商家入驻、管理违规内容 |

### 2.2 核心功能模块（共 8 大模块）

| 模块 | 功能点 |
|---|---|
| **1. 用户管理** | 注册、登录、注销、修改个人信息、修改密码 |
| **2. 商家入驻** | 商家申请入驻、营业执照上传、审核流程、店铺管理 |
| **3. 菜品管理** | 菜品上架/下架、菜品信息修改（名称/价格/描述/图片） |
| **4. 美食打卡** | GPS 定位、拍照打卡、美食类型选择、评论、打卡记录 |
| **5. 美食图鉴** | 图鉴模板、打卡解锁机制、成就收集、解锁进度展示 |
| **6. 评价系统** | 上传评价（图文）、审核评价、修改/删除评价、评价查找 |
| **7. 帖子社区** | 发布帖子（含图片+定位）、评论帖子、美食小圈交互 |
| **8. 好友交互** | 添加好友、信息转发、好友动态 |

---

## 三、当前进度评估

### ✅ 已完成
- [x] 用户实体 (User) + Repository
- [x] 用户注册（表单验证、唯一性检查、BCrypt 加密）
- [x] 用户登录/登出（Spring Security 表单认证）
- [x] 修改密码
- [x] SecurityConfig 路径权限配置
- [x] CustomUserDetailsService
- [x] 8 个前端页面 UI 骨架（Thymeleaf 模板）

### 🔶 部分完成（有 UI 无后端）
- [ ] 商家列表 / 商家详情页 — 前端完成，后端返回空数据
- [ ] 美食图鉴页 — 前端硬编码数据
- [ ] 美食打卡页 — 前端 UI 完成（相机+定位），未对接后端
- [ ] 个人中心统计 — 硬编码为 0

### ❌ 未实现
- [ ] 商家/店铺/菜品实体与数据表
- [ ] 打卡记录实体与数据表
- [ ] 评价/帖子/评论实体与数据表
- [ ] 美食图鉴实体与数据表
- [ ] 好友关系实体与数据表
- [ ] Service 层（业务逻辑层）
- [ ] 图片上传/存储方案
- [ ] 商家入驻审核流程
- [ ] 评价审核流程
- [ ] 帖子社区模块
- [ ] 好友交互模块
- [ ] 搜索功能
- [ ] 管理员后台

---

## 四、数据库设计（基于需求分析报告类图）

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   users     │     │   shops     │    │    dishes   │
├─────────────┤     ├─────────────┤     ├─────────────┤
│ id (PK)     │◄──┐ │ id (PK)     │◄──┐ │ id (PK)     │
│ username    │   │ │ shop_name   │   │ │ dish_name   │
│ email       │   │ │ user_id(FK) │   └─│ shop_id(FK) │
│ password    │   │ │ location    │     │ price       │
│ phone       │   │ │ start_time  │     │ description │
│ name        │   │ │ introduction│     │ image_url   │
│ gender      │   │ │ cover_url   │     │ status      │
│ age         │   └─│ category    │     └─────────────┘
│ user_class  │     │ status      │
│ avatar_url  │     └─────────────┘
└──────┬──────┘
       │
       │    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
       │    │  check_ins  │     │   reviews   │     │   posts     │
       │    ├─────────────┤     ├─────────────┤     ├─────────────┤
       └───►│ id (PK)     │     │ id (PK)     │     │ id (PK)     │
            │ user_id(FK) │     │ user_id(FK) │     │ user_id(FK) │
            │ dish_id(FK) │     │ shop_id(FK) │     │ title       │
            │ shop_id(FK) │     │ dish_id(FK) │     │ content     │
            │ photo_url   │     │ rating      │     │ image_urls  │
            │ comment     │     │ content     │     │ location    │
            │ latitude    │     │ image_urls  │     │ latitude    │
            │ longitude   │     │ status      │     │ longitude   │
            │ food_type   │     └──────┬──────┘     │ created_at  │
            │ created_at  │            │            └──────┬──────┘
            └─────────────┘            │                   │
                              ┌───────▼──────┐    ┌───────▼──────┐
                              │  comments    │    │  friendships │
                              ├──────────────┤    ├──────────────┤
                              │ id (PK)      │    │ id (PK)      │
                              │ post_id(FK)  │    │ user_id(FK)  │
                              │ review_id(FK)│    │ friend_id(FK)│
                              │ user_id(FK)  │    │ status       │
                              │ content      │    │ created_at   │
                              │ created_at   │    └──────────────┘
                              └──────────────┘
                    ┌──────────────────┐
                    │  food_badges     │
                    ├──────────────────┤
                    │ id (PK)          │
                    │ badge_name       │
                    │ food_type        │
                    │ description      │
                    │ image_url        │
                    │ required_count   │
                    └───────┬──────────┘
                            │
                    ┌───────▼──────────┐
                    │  user_badges     │
                    ├──────────────────┤
                    │ id (PK)          │
                    │ user_id (FK)     │
                    │ badge_id (FK)    │
                    │ unlocked_at      │
                    └──────────────────┘
```

---

## 五、开发阶段计划（共 6 个阶段）

---

### 阶段一：基础架构重构（预计 2-3 天）

**目标**：建立规范的项目分层架构，为后续开发打好基础。

| 任务 | 说明 |
|---|---|
| 1.1 引入分层架构 | 创建 `controller` / `service` / `repository` / `entity` / `dto` / `config` 包 |
| 1.2 拆分 AuthController | 将认证逻辑拆为 `AuthController` + `UserService` |
| 1.3 创建 DTO 层 | 分离 `CheckinRequest`、`Location` 等为独立 DTO 类 |
| 1.4 统一响应格式 | 创建 `ApiResponse<T>` 统一返回格式 |
| 1.5 全局异常处理 | 创建 `GlobalExceptionHandler` |
| 1.6 配置优化 | DDL 策略改为 `update`；数据库密码改为环境变量 |
| 1.7 删除无用文件 | 删除 `templates/package.json`（Next.js 残留） |

**交付物**：规范化的项目目录结构、分层代码

---

### 阶段二：核心实体与数据层（预计 3-4 天）

**目标**：完成所有实体类、Repository、数据库表创建。

| 任务 | 说明 |
|---|---|
| 2.1 User 实体增强 | 增加 phone、name、gender、age、avatarUrl、userClass 字段 |
| 2.2 Shop 实体 | 店铺实体 + `ShopRepository` |
| 2.3 Dish 实体 | 菜品实体 + `DishRepository` |
| 2.4 CheckIn 实体 | 打卡记录实体 + `CheckInRepository` |
| 2.5 Review 实体 | 评价实体 + `ReviewRepository` |
| 2.6 Post 实体 | 帖子实体 + `PostRepository` |
| 2.7 Comment 实体 | 评论实体 + `CommentRepository` |
| 2.8 FoodBadge 实体 | 图鉴模板实体 + `UserBadge` 实体 |
| 2.9 Friendship 实体 | 好友关系实体 + `FriendshipRepository` |
| 2.10 数据库初始化 | 编写 `data.sql` 插入示例商家、菜品、图鉴数据 |

**交付物**：完整的 JPA 实体类、Repository 接口、可运行的数据库 Schema

---

### 阶段三：Service 业务层（预计 4-5 天）

**目标**：实现所有业务逻辑。

| 任务 | 说明 |
|---|---|
| 3.1 UserService | 注册、登录验证、个人信息 CRUD、密码管理 |
| 3.2 ShopService | 商家入驻申请、店铺信息管理、店铺列表/搜索/详情 |
| 3.3 DishService | 菜品 CRUD、按店铺查询、上下架管理 |
| 3.4 CheckInService | 打卡提交、打卡记录查询、个人统计（次数/足迹） |
| 3.5 ReviewService | 评价 CRUD、按商家/菜品筛选、评价审核 |
| 3.6 PostService | 帖子 CRUD、带图片上传、按时间/热度排序 |
| 3.7 CommentService | 评论 CRUD、按帖子/评价筛选 |
| 3.8 FoodBadgeService | 图鉴列表查询、打卡解锁逻辑、进度计算 |
| 3.9 FriendshipService | 添加好友、好友列表、删除好友 |
| 3.10 ImageService | 图片上传/存储/访问（本地文件系统方案） |

**交付物**：完整的 Service 层，所有业务逻辑可单元测试

---

### 阶段四：Controller + 前后端对接（预计 5-6 天）

**目标**：将前端页面与后端 Service 完整对接。

| 任务 | 说明 |
|---|---|
| 4.1 商家列表页对接 | `GET /merchants` 返回真实商家数据，支持搜索/分页 |
| 4.2 商家详情页对接 | `GET /merchant/{id}` 返回店铺+菜品+评价 |
| 4.3 美食打卡页对接 | `POST /checkin` 实际保存打卡记录（照片+定位+评论） |
| 4.4 美食图鉴页对接 | `GET /food-gallery` 返回真实图鉴+解锁状态 |
| 4.5 个人中心页对接 | 打卡次数/解锁图鉴/收藏商家 真实统计 |
| 4.6 评价系统页面 | 评价列表、提交评价、修改评价页面 |
| 4.7 帖子社区页面 | 帖子列表、发帖、帖子详情+评论 |
| 4.8 好友模块页面 | 好友列表、添加好友、好友动态 |
| 4.9 商家管理后台 | 商家登录后可管理店铺/菜品/查看评价 |
| 4.10 管理员后台 | 审核商家入驻、审核评价、管理违规内容 |

**交付物**：所有页面功能完整可用，前后端完全打通

---

### 阶段五：功能完善与优化（预计 3-4 天）

**目标**：完善细节、提升用户体验。

| 任务 | 说明 |
|---|---|
| 5.1 搜索功能 | 商家搜索、菜品搜索、帖子搜索 |
| 5.2 分页功能 | 所有列表页实现分页 |
| 5.3 图片上传优化 | 支持多图上传、缩略图生成、文件大小限制 |
| 5.4 表单验证增强 | 前端+后端双重验证 |
| 5.5 消息提示优化 | 使用 Toast/Modal 替代 alert |
| 5.6 响应式适配 | 移动端页面适配 |
| 5.7 数据库索引优化 | 为常用查询字段添加索引 |
| 5.8 CSRF 防护 | 重新启用 CSRF 并为 AJAX 配置 Token |

**交付物**：功能完善的可交付系统

---

### 阶段六：测试与部署（预计 2-3 天）

**目标**：确保系统稳定可运行。

| 任务 | 说明 |
|---|---|
| 6.1 单元测试 | Service 层核心业务逻辑测试 |
| 6.2 集成测试 | Controller 层接口测试 |
| 6.3 数据库迁移 | `ddl-auto` 改为 `validate`，编写 `schema.sql` |
| 6.4 打包部署 | `mvn clean package`，生成可执行 JAR |
| 6.5 用户手册 | 编写简要使用说明 |
| 6.6 项目文档整理 | README 更新、API 文档 |

**交付物**：可部署的完整系统包 + 文档

---

## 六、里程碑时间线

```
Week 1        Week 2        Week 3        Week 4        Week 5
├─────────────┼─────────────┼─────────────┼─────────────┤
│ 阶段一       │ 阶段二       │ 阶段三       │ 阶段四       │ 阶段五六     │
│ 架构重构     │ 实体/数据层   │ Service层   │ 前后端对接    │ 测试/部署    │
│ (2-3天)     │ (3-4天)      │ (4-5天)     │ (5-6天)      │ (5-7天)     │
└─────────────┴─────────────┴─────────────┴─────────────┘
                                          ↑
                                    核心功能可用
```

---

## 七、风险与注意事项

| 风险 | 应对措施 |
|---|---|
| `.doc` 文件为旧格式，需求获取报告未能完全提取 | 可手动用 Word 打开查看，或转换为 `.docx` |
| 数据库密码明文存储 | 改用环境变量或配置加密 |
| DDL 策略为 `create`，每次启动清空数据 | 阶段一改为 `update`，最终改为 `validate` |
| 缺少 Service 层，逻辑全在 Controller | 阶段一完成分层重构 |
| 图片存储方案未确定 | 阶段三采用本地文件系统 + 静态资源映射 |
| 开题报告提到 Android APP，但当前项目仅为 Web | 建议先完成 Web 端，APP 可作为扩展 |
