# ==================== 阶段1：Node.js 构建 ====================
FROM node:20-alpine AS builder

# 构建时 API 地址（Docker 内部网络，nginx 反代后可省略）
ARG API_BASE=http://server:8080
ENV API_BASE=${API_BASE}

# 设置工作目录
WORKDIR /app

# 先复制 package 文件，利用 Docker 缓存加速依赖安装
COPY package.json package-lock.json ./

# 安装依赖
RUN npm install --legacy-peer-deps

# 复制源代码
COPY . .

# 构建 Vue 3 SPA（Vite）
RUN npm run build

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

    # SPA History 路由回退：所有未命中的请求回退到 index.html
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
