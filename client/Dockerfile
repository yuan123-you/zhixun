# ==================== 阶段1：Node.js 构建 ====================
# 注意：docker-compose.yml 中 build.context 已改为项目根目录，路径以 client/ 开头
FROM node:20-alpine AS builder

WORKDIR /app

# 先复制共享类型包（@zhixun/shared-types 是 file: 引用，需在 npm install 前就位）
# npm install 会按 file:../packages/shared-types 解析，即 /packages/shared-types
COPY packages/shared-types/ /packages/shared-types/
COPY packages/shared-types/ /app/node_modules/@zhixun/shared-types/

# 复制 package 文件，利用 Docker 缓存加速依赖安装
COPY client/package.json client/package-lock.json ./

# 安装依赖（--legacy-peer-deps 解决 peer 冲突，--ignore-scripts 跳过不必要的脚本）
RUN npm install --legacy-peer-deps

# 复制客户端源代码
COPY client/ .

# 构建 Vue 3 SPA（跳过 vue-tsc 类型检查，直接用 Vite 生产构建）
RUN npx vite build

# ==================== 阶段2：运行环境（Nginx 提供静态资源服务）====================
FROM nginx:1.27-alpine

# 删除默认配置
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# 复制 Vite 构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 SPA 路由回退配置（SPA History 模式需要 try_files）
COPY <<'EOF' /etc/nginx/conf.d/default.conf
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # SPA History 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public";
        access_log off;
    }

    # 健康检查
    location = /healthz {
        access_log off;
        return 200 "ok\n";
        add_header Content-Type text/plain;
    }
}
EOF

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=15s --timeout=10s --start-period=10s --retries=3 \
    CMD wget --spider -q http://localhost:80/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
