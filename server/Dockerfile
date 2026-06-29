# ==================== 阶段1：Maven 构建 ====================
FROM maven:3.9-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 先复制 pom.xml，利用 Docker 缓存加速依赖下载
COPY pom.xml .

# 下载依赖（仅当 pom.xml 变化时重新下载）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建项目，跳过测试
RUN mvn clean package -DskipTests -B

# ==================== 阶段2：运行环境 ====================
FROM eclipse-temurin:17-jre

# 安装 curl 用于健康检查，设置时区
RUN apt-get update && apt-get install -y --no-install-recommends curl tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

# 创建非 root 用户运行应用和日志目录
RUN groupadd -r appgroup && useradd -r -g appgroup appuser \
    && mkdir -p /home/appuser/logs/csp \
    && chown -R appuser:appgroup /home/appuser/logs

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/*.jar app.jar

# 创建本地上传目录（MinIO 降级时的 fallback 存储路径）
RUN mkdir -p /app/uploads/images /app/uploads/files /app/uploads/voices

# 修改文件所有者
RUN chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# JVM 参数：容器环境优化，显式指定 UTF-8 编码
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

# 健康检查
HEALTHCHECK --interval=15s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
