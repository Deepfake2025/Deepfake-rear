# GitHub Actions CI/CD 配置说明

## 工作流概述

本项目使用 GitHub Actions 实现完整的 CI/CD 流程，包括：

1. **构建阶段**：在 Ubuntu runner 上编译 Java 项目并构建 JAR 包
2. **镜像构建**：使用 Docker Buildx 构建并推送容器镜像到 GitHub Container Registry
3. **部署阶段**：连接云服务器，拉取镜像并运行容器

## 触发条件

- 推送到 `main` 分支时执行完整的构建、打包、部署流程
- 创建针对 `main` 分支的 Pull Request 时执行构建和测试

## 必需的 GitHub Secrets

在 GitHub 仓库设置中需要配置以下 Secrets：

### 1. SSH 连接配置
- `SSH_HOST`: 云服务器 IP 地址或域名
- `SSH_USER`: SSH 登录用户名
- `SSH_KEY`: SSH 私钥内容

### 2. 镜像仓库配置
使用 GitHub Container Registry (ghcr.io)，无需额外配置，会自动使用 `GITHUB_TOKEN`

## 云服务器准备

### 1. 安装 Docker
```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 启动 Docker 服务
sudo systemctl enable docker
sudo systemctl start docker
```

### 2. 创建必要目录
```bash
sudo mkdir -p /app/logs /app/uploads
sudo chown -R $USER:$USER /app
```

### 3. 配置防火墙（如果需要）
```bash
# 允许 8888 端口访问
sudo ufw allow 8888
```

## 部署流程

1. **代码检出**：获取最新源代码
2. **Java 构建**：使用 Maven 构建 JAR 包
3. **Docker 镜像**：构建并推送镜像到 `ghcr.io`
4. **服务器部署**：
   - SSH 连接到云服务器
   - 登录镜像仓库
   - 拉取最新镜像
   - 停止旧容器
   - 启动新容器
   - 清理旧镜像

## 容器配置

- **容器名称**：deepfake-app
- **端口映射**：8888:8888
- **重启策略**：unless-stopped
- **数据卷**：
  - `/app/logs:/app/logs` - 日志目录
  - `/app/uploads:/app/uploads` - 上传文件目录

## 环境变量

- `JAVA_OPTS`: JVM 参数配置
- `SPRING_PROFILES_ACTIVE`: Spring 配置文件（prod）

## 监控和日志

部署完成后可以通过以下方式监控：

```bash
# 查看容器状态
docker ps

# 查看容器日志
docker logs deepfake-app

# 实时查看日志
docker logs -f deepfake-app

# 查看容器资源使用
docker stats deepfake-app
```

## 故障排除

### 常见问题

1. **SSH 连接失败**
   - 检查 SSH 密钥是否正确
   - 确认服务器防火墙配置
   - 验证用户权限

2. **Docker 镜像拉取失败**
   - 检查网络连接
   - 确认镜像仓库访问权限
   - 验证镜像标签

3. **容器启动失败**
   - 查看容器日志：`docker logs deepfake-app`
   - 检查端口是否被占用
   - 验证环境变量配置

### 手动回滚

如需回滚到上一个版本：

```bash
# 查看可用镜像
docker images ghcr.io/your-repo/deepfake

# 停止当前容器
docker stop deepfake-app
docker rm deepfake-app

# 运行上一个版本的镜像
docker run -d \
  --name deepfake-app \
  --restart unless-stopped \
  -p 8888:8888 \
  ghcr.io/your-repo/deepfake:main-<previous-commit-sha>
```

## 安全注意事项

1. 使用最小权限原则配置 SSH 用户
2. 定期轮换 SSH 密钥
3. 监控容器和服务器访问日志
4. 及时更新基础镜像和依赖