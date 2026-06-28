# Zhixun 端口分配规范

> **本文件是所有端口号与主机名的唯一事实来源（Single Source of Truth）。**
> 任何代码、配置、脚本、Nginx、Docker 都不应硬编码端口，必须读取本表中的环境变量。

最后更新：2026-06-28

---

## 1. 总体拓扑

```
                       ┌────────────────────────────────────────┐
                       │  Nginx (80/443) — 反向代理 + WebSocket  │
                       └────────────────────────────────────────┘
                              │              │              │
                ┌─────────────┘              │              └──────────────┐
                ▼                            ▼                              ▼
     ┌───────────────────┐         ┌───────────────────┐         ┌───────────────────┐
     │  Client (3500)    │         │  Admin  (3001)    │         │  Backend (8080)   │
     │  Vue 3 + Vite     │         │  Vue 3 + Vite     │         │  Spring Boot      │
     │  C 端前台          │         │  管理后台           │         │  zhixun-server    │
     └───────────────────┘         └───────────────────┘         └────────┬──────────┘
                                                                         │
        ┌──────────────────────┬──────────────────┬──────────────────┬──┴──────┐
        ▼                      ▼                  ▼                  ▼         ▼
  ┌──────────┐          ┌──────────┐         ┌──────────┐       ┌──────────┐ ┌──────────┐
  │ MySQL    │          │ Redis    │         │ RabbitMQ │       │ MinIO    │ │OpenSearch│
  │ 3306     │          │ 6379     │         │ 5672/72  │       │ 9000/01  │ │ 9200/9600│
  └──────────┘          └──────────┘         └──────────┘       └──────────┘ └──────────┘
```

所有服务**仅通过固定端口 + 主机名**直连，**未引入** Nacos / Eureka / Consul 等服务发现组件。
（本项目为单体 Spring Boot + 2 个前端，无微服务拆分。）

---

## 2. 应用服务端口

| 服务             | 端口  | 主机（Docker）| 主机（本地）  | 协议      | 环境变量           | 默认值  |
|------------------|-------|---------------|---------------|-----------|--------------------|---------|
| Backend          | 8080  | `backend`     | `localhost`   | HTTP      | `SERVER_PORT`      | 8080    |
| Client (C 端)    | 3500  | `client`      | `localhost`   | HTTP      | `CLIENT_PORT`      | 3500    |
| Admin (后台)     | 3001  | `admin`       | `localhost`   | HTTP      | `ADMIN_PORT`       | 3001    |
| Nginx (生产)     | 80    | `nginx`       | `localhost`   | HTTP      | `NGINX_HTTP_PORT`  | 80      |
| Nginx TLS (生产) | 443   | `nginx`       | `localhost`   | HTTPS     | `NGINX_HTTPS_PORT` | 443     |

> ⚠️ **历史变更**：C 端端口早期为 3000，现统一改为 **3500**（与 `start-dev.ps1` 一致）。
> 如本地仍运行在 3000，请同步更新 `client/vite.config.ts` 的 `server.port` 默认值。

---

## 3. 中间件端口

| 中间件        | 端口  | 用途             | 环境变量                | 默认值 |
|---------------|-------|------------------|-------------------------|--------|
| MySQL         | 3306  | 主库 + 从库       | `MYSQL_PORT`            | 3306   |
| Redis         | 6379  | 缓存 / Session   | `REDIS_PORT`            | 6379   |
| RabbitMQ AMQP | 5672  | 消息队列         | `RABBITMQ_PORT`         | 5672   |
| RabbitMQ Mgmt | 15672 | Web 管理控制台   | `RABBITMQ_MGMT_PORT`    | 15672  |
| MinIO API     | 9000  | 对象存储 API     | `MINIO_API_PORT`        | 9000   |
| MinIO Console | 9001  | Web 控制台       | `MINIO_CONSOLE_PORT`    | 9001   |
| OpenSearch    | 9200  | HTTP API         | `OPENSEARCH_HTTP_PORT`  | 9200   |
| OpenSearch    | 9600  | Transport        | `OPENSEARCH_TRANSPORT_PORT` | 9600 |
| Sentinel      | 8719  | Dashboard 通信   | `SENTINEL_TRANSPORT_PORT` | 8719 |
| Mail SMTP SSL | 465   | 邮件发送（QQ）   | `MAIL_PORT`             | 465    |

---

## 4. 服务间调用地址（直连，**不经过服务发现**）

### 4.1 Backend → 中间件（`server/src/main/resources/application*.yml`）

| 目标         | 主机（本地）   | 主机（Docker） | 地址模板                                                            | 配置项                                     |
|--------------|----------------|----------------|----------------------------------------------------------------------|--------------------------------------------|
| MySQL 主库   | localhost      | mysql          | `jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DB:zhixun}` | `spring.datasource.master.jdbc-url`        |
| MySQL 从库   | localhost      | mysql          | `jdbc:mysql://${MYSQL_SLAVE_HOST:localhost}:${MYSQL_SLAVE_PORT:3306}/${MYSQL_SLAVE_DB:zhixun}` | `spring.datasource.slave.jdbc-url` |
| Redis        | localhost      | redis          | `${REDIS_HOST:localhost}:${REDIS_PORT:6379}`                        | `spring.data.redis.host/port`              |
| RabbitMQ     | localhost      | rabbitmq       | `${RABBITMQ_HOST:localhost}:${RABBITMQ_PORT:5672}`                  | `spring.rabbitmq.host/port`                |
| MinIO        | localhost      | minio          | `${MINIO_ENDPOINT:http://localhost:9000}`                           | `minio.endpoint`                           |
| OpenSearch   | localhost      | opensearch     | `${OPENSEARCH_HOSTS:http://localhost:9200}`                         | `opensearch.hosts`                         |
| Sentinel     | localhost      | —              | `${SENTINEL_DASHBOARD:localhost:8080}`                              | `sentinel.dashboard`                       |

### 4.2 Frontend → Backend

| 来源             | 目标     | 主机（本地）                | 主机（Docker）              | 配置文件                          |
|------------------|----------|-----------------------------|------------------------------|-----------------------------------|
| Client (3500)    | Backend  | `/api/v1` → `localhost:8080`| `API_BASE=http://server:8080` | `client/vite.config.ts` proxy + `client/Dockerfile` ARG `API_BASE` |
| Admin  (3001)    | Backend  | `/api/v1` → `localhost:8080`| `http://backend:8080`        | `admin/vite.config.ts` proxy     |
| Nginx (生产)     | Backend  | `http://127.0.0.1:8080`     | `http://backend:8080`        | `nginx/*.conf` `proxy_pass`      |

> C 端在容器中通过 `server:8080` 主机名直连；本地开发走 Vite proxy `/api/v1 → localhost:8080`。
> Admin 同理，proxy `/api/v1 → http://localhost:8080`。

### 4.3 Nginx → Frontends

| 来源          | 目标             | proxy_pass                  | 配置文件                                |
|---------------|------------------|------------------------------|------------------------------------------|
| Nginx `/api`  | Backend          | `http://127.0.0.1:8080`     | `nginx/glint.novo.ccwu.cc.conf`          |
| Nginx `/admin`| Admin SPA        | `http://127.0.0.1:3001`     | 同上                                     |
| Nginx `/`     | Client SPA       | `http://127.0.0.1:3500`     | 同上                                     |
| Nginx `/minio`| MinIO API        | `http://127.0.0.1:9000/`    | 同上                                     |
| Nginx `/ws`   | Backend WebSocket| `http://127.0.0.1:8080`     | 同上                                     |

---

## 5. 配置项索引（grep 用）

```
SERVER_PORT=8080
CLIENT_PORT=3500
ADMIN_PORT=3001
MYSQL_HOST=localhost       # Docker 中改为 mysql
MYSQL_PORT=3306
REDIS_HOST=localhost       # Docker 中改为 redis
REDIS_PORT=6379
RABBITMQ_HOST=localhost    # Docker 中改为 rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_MGMT_PORT=15672
MINIO_ENDPOINT=http://localhost:9000   # Docker 中改为 http://minio:9000
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
OPENSEARCH_HOSTS=http://localhost:9200 # Docker 中改为 http://opensearch:9200
OPENSEARCH_HTTP_PORT=9200
OPENSEARCH_TRANSPORT_PORT=9600
SENTINEL_DASHBOARD=localhost:8080      # Docker 中改为 backend:8080
SENTINEL_TRANSPORT_PORT=8719
MAIL_HOST=smtp.qq.com
MAIL_PORT=465
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443
CORS_ALLOWED_ORIGINS=http://localhost:3500,http://localhost:3001
```

---

## 6. 冲突检测

所有端口在 `1000-10000` 段均唯一，详见上文第 2、3 节。
冲突矩阵：

```
8080  → Backend            (app)
3500  → Client             (app)
3001  → Admin              (app)
3306  → MySQL              (middleware)
6379  → Redis              (middleware)
5672  → RabbitMQ AMQP      (middleware)
15672 → RabbitMQ Mgmt       (middleware)
9000  → MinIO API          (middleware)
9001  → MinIO Console      (middleware)
9200  → OpenSearch HTTP    (middleware)
9600  → OpenSearch Trans   (middleware)
8719  → Sentinel           (app)
465   → Mail SMTP SSL      (external)
80    → Nginx HTTP         (reverse proxy)
443   → Nginx HTTPS        (reverse proxy)
```

无冲突。

---

## 7. 变更记录

| 日期       | 变更内容                                  | 影响文件                                                                              |
|------------|-------------------------------------------|---------------------------------------------------------------------------------------|
| 2026-06-28 | 创建端口分配文档，确立唯一事实来源         | `docs/PORTS.md`（本文件）                                                              |
| 2026-06-28 | C 端端口 `3000 → 3500`（与启动脚本一致） | `.env.example`, `.env.production.example`, `docker-compose.yml`, `client/vite.config.ts` |
| 2026-06-28 | 统一 `Sentinel dashboard` 默认 `localhost:8080` | `server/src/main/resources/application.yml`                                            |
| 2026-06-28 | CORS 改为 `3500, 3001`                   | `.env.example`, `server/src/main/resources/application*.yml`                          |
