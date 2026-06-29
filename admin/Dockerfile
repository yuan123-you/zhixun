# ==================== 阶段1：Node.js 构建 ====================
# 注意：docker-compose.yml 中 build.context 已改为项目根目录，路径以 admin/ 开头
FROM node:20-alpine AS builder

# 设置工作目录
WORKDIR /app

# 先复制共享类型包（@zhixun/shared-types 是 file: 引用，需在 npm install 前就位）
COPY packages/shared-types/ /app/node_modules/@zhixun/shared-types/

# 先复制 package 文件，利用 Docker 缓存加速依赖安装
COPY admin/package.json admin/package-lock.json ./

# 安装依赖
RUN npm install --legacy-peer-deps

# 复制源代码
COPY admin/ .

# 构建管理后台项目
RUN npx vite build

# ==================== 阶段2：Nginx 运行环境 ====================
FROM nginx:alpine

# 安装时区数据
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 从构建阶段复制构建产物到 Nginx 默认静态文件目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制管理后台专用 Nginx 配置
COPY admin/nginx.conf /etc/nginx/conf.d/default.conf

# 删除默认配置
RUN rm -f /etc/nginx/conf.d/default.conf.bak

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --spider -q http://localhost/ || exit 1

# 启动 Nginx（使用非守护模式）
CMD ["nginx", "-g", "daemon off;"]
