# ==================== 阶段1：Node.js 构建 ====================
FROM node:20-alpine AS builder

# 设置工作目录
WORKDIR /app

# 先复制 package 文件，利用 Docker 缓存加速依赖安装
COPY package.json package-lock.json ./

# 安装依赖
RUN npm install --legacy-peer-deps

# 复制源代码
COPY . .

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
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 删除默认配置
RUN rm -f /etc/nginx/conf.d/default.conf.bak

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --spider -q http://localhost/ || exit 1

# 启动 Nginx（使用非守护模式）
CMD ["nginx", "-g", "daemon off;"]
