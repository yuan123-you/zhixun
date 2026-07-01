<div align="center">

# 知讯

**互联网内容资讯发布与管理系统**

创作 · 阅读 · 连接

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-brightgreen.svg)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-6.x-646CFF.svg)](https://vitejs.dev/)
[![License](https://img.shields.io/badge/License-Educational-blue.svg)](LICENSE)

[功能特性](#-功能特性) · [快速开始](#-快速开始) · [技术架构](#-技术架构) · [部署指南](#-部署指南) · [API 文档](#-api-文档) · [贡献指南](#-贡献指南)

</div>

---

## 📖 项目概述

知讯是面向中小型团队的互联网内容资讯发布与管理系统，提供从内容创作、发布、审核到数据分析的全链路能力。系统采用前后端分离架构，支持多角色协作，具备智能个性化推荐、内容安全审核、社交互动、多端响应式适配、运营数据看板等企业级特性。

### 核心亮点

- **智能推荐引擎** — 基于用户行为的协同过滤算法，首页信息流千人千面
- **全链路内容管理** — 创作 → 审核 → 发布 → 数据分析，一站式完成
- **实时社交互动** — WebSocket 驱动的即时私信与群组聊天
- **多维度搜索引擎** — OpenSearch 全文检索，支持作品/用户/图片聚合搜索与关键词高亮
- **企业级安全体系** — JWT 双 Token、RBAC 权限、数据加密、越权防护、审计日志
- **全端响应式适配** — 手机/平板/电脑一致体验，6 档断点自适应布局
- **AI 辅助创作** — 集成智谱 AI，支持 AI 写作与 AI 生成图片

### 用户角色

| 角色 | 标识 | 能力范围 |
|:-----|:-----|:---------|
| 超级管理员 | `SUPER_ADMIN` | 系统配置、角色权限管理、全部后台功能 |
| 运营管理员 | `ADMIN` | 内容审核、内容管理、数据查看 |
| 注册用户 | `USER` | 内容创作、浏览互动、社交、个人设置 |

---

## ✨ 功能特性

### 内容创作与阅读

| 功能 | 描述 |
|:-----|:-----|
| 富文本编辑器 | 支持封面图上传、草稿自动保存、定时发布、发布审核流 |
| 作品状态机 | 草稿 → 待审核 → 已发布/驳回 → 已下架，全流程管控 |
| 分类与标签 | 树形多级分类（6 大领域 200+ 子分类） + 多标签关联，灵活组织内容 |
| 内容模板 | 官方/用户自定义模板，快速开始创作 |
| 话题系统 | 官方/用户话题，话题关注与文章关联 |
| 协作编辑 | 邀请协作者共同创作，权限管理 |
| 个性化推荐 | 基于浏览/点赞/收藏行为的协同过滤推荐，支持刷新与分页 |
| 热点排行榜 | 多维度加权热度分（浏览×1 + 点赞×5 + 评论×3 + 收藏×8），日/周/月榜切换 |
| 最新发布 | 按发布时间倒序展示 |
| 关注动态 | 查看已关注用户的最新发布内容 |

### 搜索系统

| 功能 | 描述 |
|:-----|:-----|
| 综合搜索 | 聚合搜索作品/用户/图片，按相关性加权混合排序 |
| 作品搜索 | 标题/正文/标签全文检索，分类/时间/热度多维筛选，关键词高亮 |
| 用户搜索 | 按用户名/昵称/拼音搜索，已关注用户优先展示 |
| 图片搜索 | 搜索作品封面图及内嵌图，点击跳转原文 |
| 搜索建议 | 输入时实时补全建议（用户 + 作品 + 标签） |
| 热门搜索 | 近 24h 搜索频次 Top 10 |

### 社交与互动

| 功能 | 描述 |
|:-----|:-----|
| 点赞 | 对作品/评论点赞与取消，防重复 |
| 收藏 | 收藏作品，支持收藏夹分组管理 |
| 评论 | 多级评论与回复，支持图片评论，按最新/最热排序 |
| 关注/粉丝 | 关注取关、关注列表、粉丝列表、关注/粉丝数统计 |
| 私信 | WebSocket 实时一对一聊天，支持文字/图片/语音/文件消息，未读消息计数 |
| 群组聊天 | 创建群组、群号搜索、群成员管理、@提及、WebSocket 实时群聊 |
| 在线状态 | 用户在线/离线展示，支持隐私开关控制 |
| 通知 | 系统通知、审核通知、互动通知、关注通知、私信通知、评论回复通知 |
| 举报 | 举报作品/评论/用户，后台审核处理 |
| 分享 | 生成分享海报与链接，支持二维码 |

### 个人中心与设置

| 功能 | 描述 |
|:-----|:-----|
| 个人资料 | 头像、昵称、简介、性别、省份、联系方式编辑 |
| 个人主页 | 自定义主题颜色与背景、文章列表展示 |
| 我的作品 | 已发布/草稿/审核中作品列表及数据统计 |
| 我的收藏 | 按收藏夹分组查看 |
| 浏览历史 | 按日期筛选回看 |
| 推荐偏好 | 设置感兴趣/屏蔽的分类和标签 |
| 通知设置 | 各类通知开关控制 |
| 隐私设置 | 在线状态展示、私信权限、浏览历史记录开关 |
| 免打扰 | 免打扰时段设置 |

### 管理后台

| 功能 | 描述 |
|:-----|:-----|
| 内容审核 | 待审核队列、人工审核（通过/驳回）、敏感词自动检测、违规下架 |
| 内容管理 | 作品/分类/标签/评论增删改查、置顶、推荐 |
| 用户管理 | 用户查询、封禁/解封、角色分配 |
| 数据看板 | 用户总量、作品总量、日活、浏览量、互动量及趋势分析 |
| 敏感词库 | 敏感词增删改查，低级/中级/高级三级管理，白名单机制 |
| 轮播图管理 | 首页轮播图增删改查、排序、时间控制 |
| 公告管理 | 系统公告发布、置顶、时间控制 |
| 通知管理 | 发送通知（全部用户/指定用户）、通知列表 |
| 群组管理 | 群组列表查看、成员管理、消息记录 |
| 角色管理 | 角色权限分配与管理 |
| 操作日志 | 管理员操作审计追溯 |
| 登录日志 | 登录记录查询、异常登录追踪 |
| 安全审计 | 安全事件记录与查看 |
| AI 用量统计 | AI 功能调用统计 |
| 缓存管理 | Redis 缓存查看与清理 |

---

## 🏗 技术架构

### 系统架构

```
                        ┌─────────────┐
                        │    Nginx    │
                        │  反向代理    │
                        └──────┬──────┘
                               │
               ┌───────────────┼───────────────┐
               │               │               │
        ┌──────┴──────┐ ┌──────┴──────┐ ┌──────┴──────┐
        │   Client    │ │    Admin    │ │   Server    │
        │  Vue 3 SPA  │ │  Vue 3 SPA │ │ Spring Boot │
        │  :3500      │ │  :3001     │ │  :8080      │
        └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
               │               │               │
               └───────────────┼───────────────┘
                               │
        ┌──────────┬───────────┼───────────┬──────────┐
        │          │           │           │          │
   ┌────┴────┐ ┌───┴───┐ ┌────┴────┐ ┌────┴────┐ ┌───┴───┐
   │  MySQL  │ │ Redis │ │RabbitMQ │ │  MinIO  │ │OpenSearch│
   │  8.0    │ │  7   │ │  3.12   │ │ latest  │ │  2.11  │
   └─────────┘ └───────┘ └─────────┘ └─────────┘ └───────┘
```

### 技术选型

| 分类 | 技术 | 版本 | 说明 |
|:-----|:-----|:-----|:-----|
| **后端框架** | Spring Boot | 3.2.5 | RESTful API，Spring Security 认证授权 |
| **ORM** | MyBatis-Plus | 3.5.5 | 增强版 MyBatis，简化数据访问 |
| **JWT** | JJWT | 0.12.5 | 双 Token 认证（Access + Refresh） |
| **C端前端** | Vue 3 + Vite 6 | 3.5 / 6.x | SPA，Tailwind CSS 3 原子化样式 |
| **管理后台** | Vue 3 + Element Plus | 3.5 / 2.8 | Vite 构建，ECharts 数据看板 |
| **UI 组件** | Element Plus | 2.8 | 自动按需导入 |
| **状态管理** | Pinia | 2.2 | 轻量级状态管理 |
| **CSS 框架** | Tailwind CSS | 3.x | 原子化 CSS，6 档响应式断点 |
| **关系数据库** | MySQL | 8.0 | 主存储，主从读写分离 |
| **缓存** | Redis | 7 | 分布式缓存、排行榜、会话、限流 |
| **本地缓存** | Caffeine | — | JVM 级缓存，多级缓存（Redis + Caffeine） |
| **搜索引擎** | OpenSearch | 2.11 | 全文检索，IK 中文分词，替代 Elasticsearch |
| **消息队列** | RabbitMQ | 3.12 | 异步通知、审核流、削峰填谷 |
| **对象存储** | MinIO | latest | 自建对象存储，兼容 S3 API |
| **实时通信** | Spring WebSocket | — | 私信/群聊实时推送、在线状态同步 |
| **限流熔断** | Sentinel | 1.8.7 | 接口限流、熔断降级、系统保护 |
| **AI 服务** | 智谱 AI (GLM-4) | — | AI 写作、AI 生成图片 |
| **HTML 净化** | OWASP Sanitizer | 20240325.1 | XSS 防护，HTML 白名单过滤 |
| **工具集** | Hutool | 5.8.27 | Java 工具类库 |
| **负载均衡** | Nginx | — | 七层反向代理、Gzip 压缩、静态资源缓存 |
| **容器化** | Docker Compose | — | 容器编排，一键部署 |

### 项目结构

```
zhixun/
├── server/                          # 后端服务 (Spring Boot 3.2.5)
│   ├── src/main/java/com/zhixun/
│   │   ├── common/                  # 通用工具（异常、结果、注解、工具类）
│   │   ├── config/                  # 配置类（Security、Redis、RabbitMQ、OpenSearch、多级缓存等）
│   │   ├── controller/              # REST 控制器（27 个模块）
│   │   ├── dto/                     # 请求 DTO（按模块分包，45 个请求类）
│   │   ├── entity/                  # 数据库实体（40 个实体类）
│   │   ├── enums/                   # 枚举定义（10 个枚举）
│   │   ├── mapper/                  # MyBatis-Plus Mapper（40 个）
│   │   ├── mq/                      # 消息队列消费者
│   │   ├── security/                # Spring Security（JWT 过滤器、XSS/CSRF/安全头过滤器）
│   │   ├── service/                 # 业务逻辑层（38 个接口 + 实现）
│   │   ├── task/                    # 定时任务
│   │   ├── vo/                      # 响应 VO（32 个）
│   │   ├── websocket/               # WebSocket 处理器（私聊 + 群聊）
│   │   └── ZhixunApplication.java
│   ├── src/main/resources/
│   │   ├── db/                      # 数据库脚本
│   │   │   ├── 01-schema.sql       # 建表脚本（26 张核心表）
│   │   │   ├── 02-indexes.sql      # 索引优化
│   │   │   ├── 03-seed-data.sql    # 种子数据
│   │   │   └── migration/          # 版本迁移（V2-V8）
│   │   ├── opensearch/              # OpenSearch 索引映射
│   │   ├── scripts/                 # Lua 脚本（限流器）
│   │   └── application.yml          # 应用配置
│   └── pom.xml
├── client/                          # C端前端（Vue 3 + Vite 6 SPA）
│   ├── src/
│   │   ├── api/                     # API 接口封装（20 个模块）
│   │   ├── components/              # 公共组件（45 个，含聊天组件 7 个）
│   │   ├── composables/             # 组合式函数（40+ 个）
│   │   ├── layouts/                 # 布局组件
│   │   ├── middleware/              # 路由中间件
│   │   ├── pages/                   # 页面路由（30+ 页面）
│   │   ├── plugins/                 # 插件（API、WebSocket、Web Vitals）
│   │   ├── router/                  # 路由配置
│   │   ├── stores/                  # Pinia 状态管理（4 个）
│   │   ├── types/                   # TypeScript 类型
│   │   └── utils/                   # 工具函数（7 个）
│   ├── tests/                       # 单元测试
│   └── vite.config.ts               # Vite 配置（端口 3500）
├── admin/                           # 管理后台（Vue 3 + Element Plus）
│   ├── src/
│   │   ├── api/                     # API 接口封装（25 个模块）
│   │   ├── components/              # 公共组件（8 个）
│   │   ├── composables/             # 组合式函数
│   │   ├── layouts/                 # 布局组件
│   │   ├── router/                  # 路由配置与守卫
│   │   ├── stores/                  # Pinia 状态管理（2 个）
│   │   ├── views/                   # 页面视图（22 个模块）
│   │   └── utils/                   # 工具函数
│   └── vite.config.ts               # Vite 配置（端口 3001，base /admin/）
├── packages/
│   └── shared-types/                # 前后端共享 TypeScript 类型
├── nginx/                           # Nginx 反向代理配置
├── opensearch/                      # OpenSearch Dockerfile
├── docs/                            # 项目文档
│   └── PORTS.md                     # 端口分配规范（唯一事实来源）
├── docker-compose.yml               # Docker Compose 编排
├── .env.example                     # 环境变量模板
├── .env.production.example          # 生产环境变量模板
├── deploy.sh                        # 部署脚本
└── diagnose.sh                      # 诊断脚本
```

---

## 🚀 快速开始

### 前置条件

| 依赖 | 版本要求 | 说明 |
|:-----|:---------|:-----|
| JDK | 17+ | 后端运行环境 |
| Node.js | 18+ | 前端运行环境 |
| Maven | 3.8+ | 后端构建工具 |
| Docker | 20.10+ | 容器化部署 |
| Docker Compose | 2.0+ | 容器编排 |

### Docker Compose 一键部署

```bash
# 克隆项目
git clone <repository-url>
cd zhixun

# 配置环境变量
cp .env.example .env
# 根据实际环境修改 .env 中的配置项

# 启动所有服务
docker compose up -d

# 查看服务状态
docker compose ps
```

启动完成后访问以下地址：

| 服务 | 地址 | 用途 |
|:-----|:-----|:-----|
| 知讯首页 | http://localhost | 阅读、创作、社交 |
| 管理后台 | http://localhost/admin | 审核、管理、数据看板 |
| 后端 API | http://localhost/api | RESTful API 接口 |
| MinIO 控制台 | http://localhost:9001 | 对象存储管理 |
| RabbitMQ 管理 | http://localhost:15672 | 消息队列监控 |

> 首次启动需等待各服务初始化完成，通常 1-2 分钟。可通过 `docker compose ps` 查看健康状态。

### 本地开发

<details>
<summary>后端服务</summary>

```bash
cd server

# 确保本地已启动以下中间件：
# - MySQL 8.0 (localhost:3306)
# - Redis 7 (localhost:6379)
# - RabbitMQ 3.12 (localhost:5672)
# - MinIO (localhost:9000)
# - OpenSearch 2.x (localhost:9200)

# 启动后端服务（默认端口 8080）
mvn spring-boot:run

# 或指定配置文件
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

</details>

<details>
<summary>C端前端</summary>

```bash
cd client

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:3500
```

</details>

<details>
<summary>管理后台</summary>

```bash
cd admin

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:3001
```

</details>

---

## 📦 部署指南

### 环境变量配置

复制 `.env.example` 为 `.env`，根据部署环境修改配置：

| 变量 | 默认值 | 说明 |
|:-----|:-------|:-----|
| `MYSQL_HOST` | `localhost` | MySQL 主机地址 |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_PASSWORD` | — | MySQL 密码（生产环境务必修改） |
| `REDIS_HOST` | `localhost` | Redis 主机地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ 主机地址 |
| `RABBITMQ_USER` | `guest` | RabbitMQ 用户名 |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ 密码 |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO 端点 |
| `MINIO_ACCESS_KEY` | — | MinIO Access Key |
| `MINIO_SECRET_KEY` | — | MinIO Secret Key |
| `OPENSEARCH_HOSTS` | `http://localhost:9201` | OpenSearch 地址 |
| `SERVER_PORT` | `8080` | 后端服务端口 |
| `CLIENT_PORT` | `3500` | C端前端端口 |
| `ADMIN_PORT` | `3001` | 管理后台端口 |
| `JWT_ACCESS_SECRET` | — | Access Token 签名密钥（生产环境务必修改） |
| `JWT_REFRESH_SECRET` | — | Refresh Token 签名密钥（生产环境务必修改） |
| `JWT_ACCESS_EXPIRATION` | `604800` | Access Token 有效期（秒，默认 7 天） |
| `JWT_REFRESH_EXPIRATION` | `604800` | Refresh Token 有效期（秒，默认 7 天） |
| `AES_SECRET_KEY` | — | AES-256-GCM 加密密钥（32 字符，生产环境务必修改） |
| `MAIL_HOST` | `smtp.qq.com` | 邮件 SMTP 主机 |
| `MAIL_PORT` | `465` | 邮件 SMTP 端口（SSL） |
| `MAIL_USERNAME` | — | 邮箱地址 |
| `MAIL_PASSWORD` | — | 邮箱授权码（非邮箱密码） |
| `ZHIPU_API_KEY` | — | 智谱 AI API Key |
| `TENCENT_MAP_KEY` | — | 腾讯地图 Key（IP 定位，为空时降级到公共接口） |
| `NGINX_HTTP_PORT` | `80` | Nginx HTTP 端口 |
| `NGINX_HTTPS_PORT` | `443` | Nginx HTTPS 端口 |

> **生产环境警告**：部署前务必将所有密钥和密码替换为强随机值，切勿使用默认配置。

### 数据库初始化

数据库建表脚本位于 `server/src/main/resources/db/01-schema.sql`，Docker Compose 首次启动时自动执行。如需手动初始化：

```bash
mysql -u root -p zhixun < server/src/main/resources/db/01-schema.sql
```

后续版本迁移脚本位于 `server/src/main/resources/db/migration/`，应用启动时自动执行。

### Nginx 配置

Nginx 配置文件位于 `nginx/`，已预配置：

- `/` → C端前端（Vue 3 SPA，端口 3500）
- `/admin` → 管理后台（Vue 3 SPA，端口 3001）
- `/api` → 后端 API 服务（端口 8080）
- `/ws` → WebSocket 连接（端口 8080）
- Gzip 压缩、静态资源缓存、安全响应头

---

## 📡 API 文档

### 通用约定

| 项目 | 规范 |
|:-----|:-----|
| 基础路径 | `/api/v1` |
| 认证方式 | httpOnly Cookie（JWT），WebSocket 通过 Cookie 传递 Token |
| 请求格式 | `application/json` |
| 分页参数 | `page`（页码，从 1 开始）、`page_size`（每页条数，默认 20，最大 100） |

### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 错误码

| 错误码 | 说明 |
|:-------|:-----|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如重复点赞） |
| 422 | 业务逻辑校验失败 |
| 429 | 请求过于频繁（限流） |
| 500 | 服务器内部错误 |

### 接口模块

| 模块 | 路径前缀 | 说明 |
|:-----|:---------|:-----|
| 认证 | `/auth` | 注册、登录、登出、Token 刷新、密码管理、OAuth 第三方登录 |
| 作品 | `/articles` | 作品 CRUD、状态变更、相关推荐、协作管理 |
| 推荐与排行 | `/feed` `/rank` | 个性化推荐、最新发布、关注动态、热点排行榜 |
| 搜索 | `/search` | 综合搜索、搜索建议、热门搜索 |
| 互动 | `/articles/{id}/like` 等 | 点赞、收藏、评论 |
| 用户 | `/user` | 个人资料、全局设置、浏览历史、主页主题 |
| 社交 | `/users/{id}/follow` 等 | 关注、粉丝、在线状态、私信 |
| 群组 | `/groups` | 群组创建、加入、成员管理、群聊消息 |
| 话题 | `/topics` | 话题列表、关注、关联文章 |
| 模板 | `/templates` | 内容模板管理 |
| 文件 | `/files` | 图片/语音/文件上传、MinIO 文件代理 |
| AI | `/ai` | AI 写作、AI 生成图片 |
| 通知 | `/notifications` | 通知列表、标记已读 |
| 公告 | `/announcements` | 系统公告 |
| 轮播图 | `/banners` | 首页轮播图 |
| 举报 | `/reports` | 举报作品/评论/用户 |
| 管理 | `/admin` | 审核、用户管理、数据概览、敏感词、操作日志、角色管理 |

<details>
<summary>核心接口示例</summary>

**用户登录**

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "zhangsan",
  "password": "Password123"
}
```

**发布作品**

```
POST /api/v1/articles
Cookie: access_token=<jwt>
Content-Type: application/json

{
  "title": "作品标题",
  "content": "作品内容",
  "category_id": 1,
  "tag_ids": [1, 2],
  "status": 1
}
```

**个性化推荐**

```
GET /api/v1/feed/recommend?page=1&page_size=20
Cookie: access_token=<jwt>
```

**综合搜索**

```
GET /api/v1/search?keyword=深度学习&type=all&page=1&page_size=20
```

**关注用户**

```
POST /api/v1/users/2/follow
Cookie: access_token=<jwt>
```

**AI 写作**

```
POST /api/v1/ai/write
Cookie: access_token=<jwt>
Content-Type: application/json

{
  "prompt": "写一篇关于量子计算的科普文章",
  "style": "popular_science"
}
```

</details>

---

## 🗄 数据模型

系统共 38 张数据表，核心实体关系如下：

```
用户(1) ──< (N)作品
用户(1) ──< (N)评论
用户(1) ──< (N)点赞
用户(1) ──< (N)收藏
用户(1) ──< (N)关注（关注者/被关注者）
用户(1) ──< (N)私信（发送者/接收者）
用户(1) ──1(1)偏好设置
用户(1) ──1(1)主页主题
分类(1) ──< (N)作品
作品(1) ──< (N)评论
作品(1) ──< (N)作品图片
作品(1) ──< (N)协作者
标签(M) >──< (N)作品
话题(M) >──< (N)作品
群组(1) ──< (N)群成员
群组(1) ──< (N)群消息
```

| 表名 | 说明 |
|:-----|:-----|
| `sys_user` | 用户表（UID、在线状态、关注/粉丝/作品计数冗余、微信/QQ OpenID） |
| `cms_article` | 作品表（热度分、可见性、IP 属地、软删除） |
| `cms_category` | 分类表（6 大领域 200+ 子分类，树形结构） |
| `cms_tag` | 标签表（120 个标签，文章计数） |
| `cms_article_tag` | 作品-标签关联表 |
| `cms_article_image` | 作品图片表（封面图/内嵌图） |
| `cms_article_collaborator` | 作品协作者表 |
| `cms_article_report` | 作品举报表 |
| `cms_like` | 点赞表（联合唯一防重复，支持作品/评论点赞） |
| `cms_collect` | 收藏表（收藏夹分组） |
| `cms_comment` | 评论表（多级评论，支持图片评论 JSON 列） |
| `cms_comment_report` | 评论举报表 |
| `cms_view_history` | 浏览记录表 |
| `cms_banner` | 轮播图/横幅表 |
| `cms_template` | 内容模板表 |
| `cms_topic` | 话题表（热度分、官方/用户创建） |
| `cms_topic_article` | 话题文章关联表 |
| `cms_topic_follow` | 话题关注表 |
| `cms_group_info` | 群组表（群号、公开/私密、最大成员数） |
| `cms_group_member` | 群组成员表（角色、群昵称、禁言） |
| `cms_group_message` | 群组消息表（AES 加密，@提及） |
| `cms_group_join_request` | 群组加入申请表 |
| `user_follow` | 关注关系表 |
| `user_message` | 私信表（AES-256-GCM 加密存储，支持文字/图片/语音/文件） |
| `user_settings` | 用户偏好设置表（通知开关、隐私、免打扰、主题、字号） |
| `user_preferred_category` | 偏好分类关联表（感兴趣/屏蔽） |
| `user_preferred_tag` | 偏好标签关联表（感兴趣/屏蔽） |
| `user_tag_follow` | 标签关注表 |
| `user_profile_theme` | 用户主页主题表（主题色、背景图、卡片样式） |
| `sys_notification` | 通知消息表（分组键、已读状态、发送者、目标范围） |
| `sys_announcement` | 系统公告表 |
| `sys_sensitive_word` | 敏感词表（低级/中级/高级三级） |
| `sys_sensitive_whitelist` | 敏感词白名单表 |
| `sys_operation_log` | 操作日志表 |
| `sys_login_log` | 登录日志表 |
| `sys_security_audit_log` | 安全审计日志表 |
| `sys_user_report` | 用户举报表 |
| `migration_history` | 数据库迁移历史表 |

---

## 🔒 安全设计

| 领域 | 方案 |
|:-----|:-----|
| **认证授权** | JWT 双 Token 机制（Access 7d + Refresh 7d），httpOnly Cookie 存储，RBAC 三级权限模型 |
| **越权防护** | 水平越权（数据所有权校验）、垂直越权（接口角色校验）、IDOR 防护（禁止 ID 遍历） |
| **输入安全** | XSS 过滤（OWASP HTML Sanitizer 白名单 + DOMPurify 客户端净化）、SQL 注入防护（参数化查询）、CSRF 防护（双重提交 Cookie） |
| **数据加密** | 密码 bcrypt 哈希（salt rounds ≥ 10）、手机号/邮箱 AES-256-GCM 加密、私信内容加密存储 |
| **接口防护** | 令牌桶限流（Redis + Lua 脚本）、CORS 白名单 |
| **文件上传** | 白名单扩展名 + 文件头魔数校验 + 5MB 大小限制 + 随机重命名 |
| **审计日志** | 登录日志、操作日志、安全审计日志，日志只增不改不删 |
| **传输安全** | 全站 HTTPS（TLS 1.2+）、HSTS、安全响应头 |

---

## ⚡ 性能优化

| 层级 | 策略 |
|:-----|:-----|
| **前端** | Vue 3 SPA + 路由懒加载、代码分割与 Tree Shaking、手动 Chunk 拆分（vendor-element-plus / vendor-vue / vendor-utils）、WebP 图片 + 懒加载、HTTP 强缓存、Nginx 反向代理 |
| **后端** | Redis + Caffeine 多级缓存（Cache Aside + 缓存穿透/击穿/雪崩防护）、RabbitMQ 异步处理、HikariCP 连接池、接口聚合、Gzip 压缩 |
| **数据库** | 索引优化、游标分页（关注动态 Feed 避免深分页 OFFSET）、读写分离（1 主 2 从） |
| **缓存** | 多级缓存管理器、缓存预热、缓存击穿互斥锁、缓存雪崩 TTL 随机偏移、空值缓存 + 布隆过滤器防穿透 |

### 性能指标

| 指标 | 目标值 |
|:-----|:-------|
| 首屏渲染（FCP） | 桌面 ≤ 1.2s / 移动 ≤ 1.8s |
| 最大内容渲染（LCP） | 桌面 ≤ 2.0s / 移动 ≤ 2.5s |
| 接口响应 P50 | < 100ms |
| 接口响应 P99 | < 500ms（列表）/ < 200ms（详情） |
| 搜索响应 P95 | 综合 ≤ 300ms / 作品 ≤ 200ms |
| 峰值 QPS | 支持 3000 QPS |
| 系统可用率 | ≥ 99.9% |

---

## 📱 多端适配

| 断点 | 宽度 | 设备 | 布局策略 |
|:-----|:-----|:-----|:---------|
| `xs` | 320–479px | 小屏手机 | 单列布局，底部 Tab 导航 |
| `sm` | 480–767px | 大屏手机 | 单列布局，底部 Tab 导航 |
| `md` | 768–1023px | 平板竖屏 | 双列布局，侧边栏折叠 |
| `lg` | 1024–1199px | 平板横屏/小笔记本 | 双列/三列布局，侧边导航展开 |
| `xl` | 1200–1919px | 桌面端 | 三列布局（导航 + 内容 + 推荐） |
| `xxl` | ≥ 1920px | 大屏显示器 | 三列布局，内容区最大宽度限制 |

- 触摸设备优化：44px 最小点击区域、手势支持（下拉刷新/双指缩放）、触摸反馈
- 横竖屏平滑切换，状态保持
- 兼容 Chrome 100+、Firefox 100+、Safari 15+、Edge 100+

---

## 🧪 测试

```bash
# 后端单元测试
cd server && mvn test

# 代码覆盖率报告（JaCoCo + SonarQube）
cd server && mvn verify

# C端前端单元测试
cd client && npm run test

# C端前端测试覆盖率
cd client && npm run test:coverage
```

---

## 🗺 开发路线

| 阶段 | 目标 | 核心交付 |
|:-----|:-----|:---------|
| **P0 — MVP** | 最小可用系统 | 注册登录、作品发布审核、分类管理、点赞收藏、关注私信、个性化推荐、热点排行榜、基础搜索、数据看板、基础安全防护 |
| **P1 — 完善** | 功能完整系统 | 评论系统、标签体系、搜索增强（综合/图片/建议/高亮/拼音/同义词）、敏感词过滤、通知系统、关注动态 Feed、群组聊天、读写分离、越权防护、安全审计、AI 辅助创作、OAuth 第三方登录 |
| **P2 — 增强** | 运营增强系统 | 数据报表、深色模式、多语言、负载均衡多实例、限流熔断降级、全量安全测试与压测验证 |

---

## 🤝 贡献指南

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/your-feature`)
3. 提交更改 (`git commit -m 'feat: add some feature'`)
4. 推送到分支 (`git push origin feature/your-feature`)
5. 发起 Pull Request

### 提交规范

| 类型 | 说明 |
|:-----|:-----|
| `feat` | 新功能 |
| `fix` | 修复 Bug |
| `docs` | 文档变更 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构 |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具变更 |

---

## 📄 License

本项目仅供学习与实训使用。
