# ==================== 阶段1：Node.js 构建 ====================
FROM node:20-alpine AS builder

# 构建时API地址（Docker内部网络，SSR用）
ARG API_BASE=http://server:8080
ENV API_BASE=${API_BASE}

# 设置工作目录
WORKDIR /app

# 先复制 package 文件，利用 Docker 缓存加速依赖安装
COPY package.json package-lock.json ./

# 安装依赖
RUN npm install

# 复制源代码
COPY . .

# 构建 Nuxt 项目
RUN npm run build

# ==================== 阶段2：运行环境 ====================
FROM node:20-alpine

# 安装时区数据
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 创建非 root 用户运行应用
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 设置工作目录
WORKDIR /app

# 从构建阶段复制构建产物
COPY --from=builder /app/.output ./.output
COPY --from=builder /app/package.json ./

# 修改文件所有者
RUN chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 3000

# 环境变量
ENV HOST=0.0.0.0
ENV PORT=3000
ENV NODE_ENV=production
# Nuxt 运行时环境变量（通过 NUXT_ 前缀覆盖 runtimeConfig）
# 这些值可在 docker-compose 中通过环境变量覆盖
ENV NUXT_API_BASE=http://server:8080
ENV NUXT_PUBLIC_API_BASE=/api/v1
ENV NUXT_PUBLIC_WS_BASE=/ws

# 健康检查
HEALTHCHECK --interval=15s --timeout=10s --start-period=30s --retries=3 \
    CMD wget --spider -q http://localhost:3000/ || exit 1

# 启动 Nuxt 服务
CMD ["node", ".output/server/index.mjs"]
