
FROM openjdk:11-jre-slim

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    && rm -rf /var/lib/apt/lists/*

# 复制jar文件
COPY target/deepfake-0.0.1-SNAPSHOT.jar app.jar

# 创建必要的目录
RUN mkdir -p /app/logs /app/uploads

# 创建非root用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 更改文件所有者
RUN chown -R appuser:appuser /app

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8888


# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8888/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java -jar app.jar --spring.profiles.active=prod"]

