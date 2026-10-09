# 知讯 · 内容资讯与社区平台

知讯是基于 **Vue 3、Spring Boot 与 MySQL** 的内容资讯与社区项目，包含用户端、管理后台和 Java 业务服务。平台围绕作品创作、审核发布、信息流阅读、搜索发现与社交互动展开，并提供 AI 文本辅助及群聊 AI 入口。

## 导航

- [功能概览](#功能概览)
- [技术架构](#技术架构)
- [目录结构](#目录结构)
- [本地开发](#本地开发)
- [配置说明](#配置说明)
- [构建与测试](#构建与测试)
- [容器与部署](#容器与部署)
- [当前实现边界](#当前实现边界)
- [源码索引](#源码索引)
- [项目文档](#项目文档)

## 功能概览

| 模块 | 主要功能 |
| --- | --- |
| 账号与身份 | 注册、登录、退出、令牌刷新、密码管理、验证码及用户资料 |
| 内容创作 | 作品编辑、草稿、发布审核、分类、标签、话题与协作相关接口 |
| 阅读与发现 | 推荐、最新、热门、关注信息流，搜索与排行榜 |
| 社区互动 | 评论、点赞、收藏、关注、通知与举报 |
| 实时沟通 | 私信、会话、群组、入群申请、成员管理与 WebSocket 推送 |
| 管理后台 | 内容审核、用户与内容管理、公告、运营与系统管理页面 |
| AI 辅助 | 文本创作、摘要、内容审核建议和群聊 AI 调用；图片接口仍为占位实现 |
| 文件服务 | 上传与文件查看接口，MinIO 存储及本地上传相关实现 |

角色由 `RoleEnum` 定义为 `USER`、`ADMIN`、`SUPER_ADMIN`。前端路由守卫控制页面访问体验，后端 Spring Security 与业务逻辑负责接口权限校验。

### 业务流程

- **内容生命周期**：`ArticleStatusEnum` 定义草稿、待审核、已发布、已拒绝与已下线状态；内容可见性与发布状态分别由对应规则处理。
- **信息流推荐**：`FeedServiceImpl` 组合协同过滤、内容推荐、冷启动探索、时效性与多样性重排。未登录用户走热门信息流。
- **实时消息**：WebSocket 注册 `/ws/chat` 与 `/ws/group-chat`，结合后端 `/api` context path，客户端连接路径包含 `/api`。群组服务检查成员身份，并处理消息与管理操作。
- **AI 内容辅助**：文本功能通过配置的模型接口调用；未配置 API Key 时返回明确提示。AI 输出需要复核，不替代人工内容审核。

## 技术架构

| 层次 | 技术与职责 |
| --- | --- |
| 用户端 | Vue 3、TypeScript、Vite 6、Vue Router、Pinia、Element Plus、Tailwind CSS |
| 管理后台 | Vue 3、Element Plus、ECharts、Vite；独立构建与部署 |
| 共享类型 | 本地 `@zhixun/shared-types` 包，由两个前端通过 `file:` 引用 |
| 业务后端 | Java 17、Spring Boot 3.2.5、Spring Security、MyBatis-Plus 3.5.5 |
| 数据与缓存 | MySQL、Redis、Caffeine；配置主 / 从数据源与缓存服务 |
| 搜索与消息 | OpenSearch、RabbitMQ、Spring WebSocket |
| 文件与安全组件 | MinIO、JJWT、AES 工具、HTML Sanitizer、Sentinel |
| AI 接入 | 智谱兼容接口；模型、备用模型与密钥由配置管理 |
| 验证工具 | Maven、JUnit、Vitest、Vue Test Utils、vue-tsc |

```text
用户端 client/          管理后台 admin/
        └────── /api ───────┘
                  │
             Spring Boot
                  ├── MyBatis-Plus → MySQL 主 / 从数据源
                  ├── Redis / Caffeine
                  ├── RabbitMQ
                  ├── OpenSearch
                  ├── MinIO / 文件服务
                  ├── WebSocket 私信与群聊
                  └── AI 模型接口
```

API 由 `server.servlet.context-path=/api` 与 Controller 的 `/v1/...` 路径组合，例如认证入口位于 `/api/v1/auth`。主 / 从数据源配置不等同于数据库复制已部署；单机开发可指向同一个隔离数据库。

## 目录结构

```text
.
├── client/                      # 用户端 Vue SPA
├── admin/                       # 管理后台 Vue SPA
├── packages/shared-types/       # 两端共享类型
├── server/
│   ├── pom.xml                  # Java 依赖与 Maven 构建
│   └── src/
│       ├── main/java/com/zhixun/
│       │   ├── controller/      # REST API
│       │   ├── service/         # 内容、推荐、消息与 AI 业务
│       │   ├── mapper/          # MyBatis 数据访问
│       │   ├── entity/          # 数据实体
│       │   ├── config/          # 数据源、缓存、迁移等配置
│       │   ├── security/        # 认证与安全过滤
│       │   ├── mq/              # 消息消费者
│       │   └── websocket/       # 实时通信处理器
│       ├── main/resources/      # YAML 配置、数据库脚本与搜索资源
│       └── test/                # 后端测试
├── opensearch/                  # 搜索镜像与插件构建
├── nginx/                       # 反向代理配置
├── deploy/                      # 部署相关资源
├── docs/                        # 项目设计、报告与运行资料
├── docker-compose.yml           # 基础设施与应用容器定义
└── .env.example                 # 环境变量模板
```

根目录当前没有可用的 `package.json`；两个前端需分别安装依赖并执行启动命令。

## 本地开发

### 1. 环境与前置条件

- Java 17、可用的 Maven。
- Node.js：本地测试建议使用 **22.22.2+**；当前前端 Docker 构建镜像使用 Node 20，两者不要混为同一验证环境。
- 可访问的 MySQL、Redis、RabbitMQ，以及相应功能需要的 MinIO、OpenSearch。
- 保持 `client/`、`admin/` 与 `packages/shared-types/` 的相对路径不变。

数据库应先准备与当前实体、Mapper 和迁移兼容的结构。`server/src/main/resources/db/` 包含建表、种子与增量脚本，**不要将目录内所有 SQL 无差别重复导入**。选择匹配版本的基线，再对照迁移与目标结构补齐；不要对现有业务库执行演示数据导入。

### 2. 配置后端环境

`.env.example` 是 Compose 配置模板。直接运行 Maven 时，应将所需值注入当前进程环境；当前 Java 入口没有读取根目录 `.env` 的逻辑，复制文件本身不会使 Maven 自动加载这些变量。

以下 PowerShell 示例只展示非敏感连接参数。数据库身份、主 / 从库凭据及管理员初始化凭据须通过受控环境另行注入：

```powershell
$env:MYSQL_HOST = "localhost"
$env:MYSQL_DB = "zhixun"
$env:MYSQL_SLAVE_HOST = $env:MYSQL_HOST
$env:MYSQL_SLAVE_DB = $env:MYSQL_DB
```

还需设置 `JWT_ACCESS_SECRET`、`JWT_REFRESH_SECRET` 与 `AES_SECRET_KEY`。使用三个独立生成的 Base64 编码密钥；JWT 密钥至少 32 字节，AES 密钥精确 32 字节。可用 `openssl rand -base64 32` 分别生成，不要提交实际密钥。

根据实际服务配置 Redis、RabbitMQ、MinIO 与 OpenSearch；默认本地 OpenSearch 地址为 `http://localhost:9201`，Compose 默认端口为 `9200`，请通过 `OPENSEARCH_HOSTS` 显式统一。

### 3. 启动后端

从仓库根目录开始：

```bash
cd server
mvn spring-boot:run
```

默认端口为 `8080`。健康检查位于 `http://localhost:8080/api/actuator/health`，可能受到依赖服务状态或访问策略影响，不等同于完整业务验收。

### 4. 启动两个前端

分别在两个终端中，从仓库根目录开始执行：

```bash
cd client
npm ci
npm run dev
```

```bash
cd admin
npm ci
npm run dev
```

| 服务 | 默认本地入口 | 配置来源 |
| --- | --- | --- |
| 用户端 | `http://localhost:3500` | `client/vite.config.ts` 的 `CLIENT_PORT` |
| 管理后台 | `http://localhost:3001` | `admin/vite.config.ts` 的 `ADMIN_PORT` |
| 后端 | `http://localhost:8080/api` | `application.yml` |

两个 Vite 服务代理 `/api`。用户端默认目标为 `http://[::1]:8080`，后台默认目标为 `http://127.0.0.1:8080`；可通过启动 Vite 进程时的 `VITE_API_TARGET` 统一目标。

## 配置说明

| 配置组 | 环境变量 / 说明 |
| --- | --- |
| 数据库 | `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DB`、`MYSQL_USER`、`MYSQL_PASSWORD`，以及对应 `MYSQL_SLAVE_*` |
| 缓存 | `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DATABASE` |
| 消息队列 | `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USER`、`RABBITMQ_PASSWORD`、`RABBITMQ_VHOST` |
| 身份与加密 | `JWT_ACCESS_SECRET`、`JWT_REFRESH_SECRET`、`AES_SECRET_KEY` |
| 文件存储 | `MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`MINIO_BUCKET` |
| 搜索 | `OPENSEARCH_HOSTS`、`OPENSEARCH_USERNAME`、`OPENSEARCH_PASSWORD` |
| AI 文本 | `ZHIPU_API_KEY`、`ZHIPU_BASE_URL`、`ZHIPU_MODEL`、`ZHIPU_FALLBACK_MODEL` |
| 邮件 | `MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD`；相关功能需有效服务配置 |
| 管理员初始化 | `ADMIN_USERNAME`、`ADMIN_PASSWORD`；仅初始化不存在的指定账号 |
| 跨域与其他集成 | `CORS_ALLOWED_ORIGINS`、OAuth、地图等配置按实际启用功能设置 |

基础配置默认使用 `dev`；仓库没有独立 `application-dev.yml`。生产 profile 的覆盖项见 `application-prod.yml`，包括 `SSR_SECRET` 等额外变量；保留这些配置不意味着当前 Vue 用户端是 SSR 应用。

## 构建与测试

以下代码块各自从仓库根目录开始，测试配置必须与生产数据隔离。

**后端**

```bash
cd server
mvn test
mvn package
```

**用户端**

```bash
cd client
npm ci
npm test
npm run typecheck
npm run build
```

**管理后台**

```bash
cd admin
npm ci
npm test
npx vue-tsc --noEmit
npm run build
```

`build` 脚本包含类型检查。前端 Dockerfile 当前直接执行 `npx vite build`，后端 Dockerfile 使用 `-DskipTests` 打包；镜像构建成功不能替代上述类型检查和测试。`lint` 脚本含 `--fix`，执行时会修改文件，不是纯只读检查。

## 容器与部署

```powershell
Copy-Item .env.example .env
# 按实际环境补齐配置后检查展开结果，勿输出或共享敏感值。
docker compose config --quiet
```

Compose 定义基础设施、两个后端实例、用户端、管理后台，以及 `standalone` profile 下的 Nginx。部署前检查：

1. **数据库初始化**：MySQL 挂载整个 `db/` 目录到初始化目录，目录中包含基线、种子与增量 SQL；首次创建数据卷前必须审核初始化集合。已有数据卷不会因重启而重新执行初始化脚本。
2. **迁移覆盖**：自定义迁移执行器使用显式文件列表，不是 Flyway 自动扫描；缺失的迁移需结合目标结构确认处理。
3. **前端代理**：生产 SPA 容器不使用 Vite 开发代理，需由实际 Nginx 转发 `/api` 与 WebSocket；直接访问 SPA 端口不代表 API 路由已正确配置。
4. **网关前置条件**：`standalone` Nginx 使用宿主机证书与目录挂载。未准备证书和对应目录时，不能将它视为通用的一键本地入口。
5. **凭据与暴露面**：更换默认管理密码与模板凭据，不将数据库、缓存、队列或搜索管理接口暴露到公网。

反向代理须保留 `/api` 路径，正确配置 WebSocket Upgrade，以及前端 History 路由回退。实际端口以 Compose 插值与运行配置为准。

## 当前实现边界

| 项目 | 源码现状与使用注意 |
| --- | --- |
| AI 图片 | `AIServiceImpl.generateImage` 返回提示占位文本，没有调用真实图片模型；暂不支持图片生成 |
| AI 文本 | 未配置密钥时返回明确提示；真实回答质量、延迟与可用性需单独验证 |
| 自动迁移 | `DatabaseMigrationRunner` 当前只列出 V2、V3、V4 与一个 V5 脚本；目录内其他迁移不会因此自动执行 |
| 迁移失败 | 执行器记录异常但不阻止应用启动；进程启动成功不代表结构升级完整 |
| 默认管理员 | 初始化器未配置密码时仍使用内置默认值；已有同名账号跳过创建，修改环境变量不会自动修改其密码。部署前必须显式配置并核对实际账号 |
| 数据源 | 存在读写路由实现与主 / 从配置；单机 Compose 将两者指向同一 MySQL，不代表已经建立复制集群 |

## 源码索引

| 说明 | 当前实现入口 |
| --- | --- |
| 内容状态 | [ArticleStatusEnum](server/src/main/java/com/zhixun/enums/ArticleStatusEnum.java) |
| 推荐信息流 | [FeedServiceImpl](server/src/main/java/com/zhixun/service/impl/FeedServiceImpl.java) |
| AI 接口与实现 | [AIController](server/src/main/java/com/zhixun/controller/AIController.java)、[AIServiceImpl](server/src/main/java/com/zhixun/service/impl/AIServiceImpl.java) |
| 群组业务 | [GroupServiceImpl](server/src/main/java/com/zhixun/service/impl/GroupServiceImpl.java) |
| 实时通信 | [WebSocketConfig](server/src/main/java/com/zhixun/config/WebSocketConfig.java) |
| 数据库升级 | [DatabaseMigrationRunner](server/src/main/java/com/zhixun/config/DatabaseMigrationRunner.java)、[数据库脚本](server/src/main/resources/db/) |
| 默认账号 | [DefaultUserDataInitializer](server/src/main/java/com/zhixun/config/DefaultUserDataInitializer.java) |
| 启动与部署配置 | [application.yml](server/src/main/resources/application.yml)、[Compose](docker-compose.yml) |
| 前端命令 | [client/package.json](client/package.json)、[admin/package.json](admin/package.json) |

## 项目文档

- [系统设计](docs/system_design.md)
- [项目需求](docs/项目需求文档.md)
- [项目现状分析](docs/项目现状分析报告.md)
- [技术提升指导](docs/技术提升指导说明.md)
- [端口约定](docs/PORTS.md)
- [需求分析报告](docs/软件技术方向综合设计(二)需求分析报告.docx)
- [详细设计报告](docs/软件技术方向综合设计(二)详细设计报告.docx)
- [系统测试报告](docs/软件技术方向综合设计(二)系统测试报告.docx)
- [部署运行手册](docs/软件技术方向综合设计(二)部署运行手册.docx)

### 敏感配置与历史数据

- 用户数据导出已移出应用资源目录，保存在不受 Git 跟踪的本地私有目录，不随源码或镜像发布。
- 种子账号不再携带可用密码或静态密码哈希。真实账号初始化使用显式环境凭据。
- 旧公开 JWT / AES 密钥只保留不可用于认证的 SHA-256 拒绝指纹，不再保存原值；启动时拒绝继续使用这些已暴露配置。
- AES 密钥轮换涉及历史数据解密与重新加密，须保留旧密钥的私有备份并设计迁移，不能直接替换后丢弃旧值。
- 当前文件清理不会删除 Git 历史或自动改变线上凭据。

敏感信息回归检查：在仓库根目录运行 `python tests/test_secret_hygiene.py`。
