# Nginx 配置说明

本配置实现了API请求和静态资源请求的分离，提供负载均衡、缓存和压缩等功能。

## 配置文件结构

```
nginx/
├── nginx.conf          # 主配置文件
├── conf.d/
│   └── deepfake.conf   # 站点配置文件
├── ssl/               # SSL证书目录（可选）
└── README.md          # 说明文档
```

## 主要功能

### 1. 静态资源处理
- 自动识别常见静态文件类型（CSS, JS, 图片等）
- 配置了30天的浏览器缓存
- 启用gzip压缩减少传输大小
- 可以根据需要配置静态文件根目录

### 2. 默认请求处理
- 其他所有请求默认代理到后端应用
- 可根据前端需求修改为静态文件服务

## 使用方法

### Docker方式（推荐）

1. 启动所有服务：
```bash
docker-compose up -d
```

2. 服务访问地址：
- 应用地址：http://localhost:80
- API接口：http://localhost:80/api/
- 文档界面：http://localhost:80/doc.html
- 直接访问后端：http://localhost:8888（开发调试用）

### 本地Nginx方式

1. 安装Nginx：
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install nginx

# CentOS/RHEL
sudo yum install nginx
```

2. 复制配置文件：
```bash
sudo cp nginx/nginx.conf /etc/nginx/nginx.conf
sudo cp nginx/conf.d/deepfake.conf /etc/nginx/conf.d/
```

3. 创建日志目录：
```bash
sudo mkdir -p /var/log/nginx
```

4. 启动Nginx：
```bash
sudo nginx -t  # 测试配置
sudo systemctl start nginx
sudo systemctl enable nginx
```

## 配置自定义

### 修改域名
编辑 `nginx/conf.d/deepfake.conf`：
```nginx
server_name your-domain.com;
```

### 配置前端静态文件
如果有前端构建产物，修改静态资源配置：
```nginx
location ~* \.(css|js|jpg|jpeg|png|gif|ico|svg|woff|woff2|ttf|eot)$ {
    root /path/to/your/frontend/build;
    expires 30d;
    add_header Cache-Control "public, immutable";
}
```

### HTTPS配置
1. 将SSL证书放入 `nginx/ssl/` 目录
2. 取消注释配置文件中的HTTPS部分
3. 修改证书路径

## 性能优化

### 连接数优化
- `worker_connections 1024`：每个工作进程的最大连接数
- `keepalive_timeout 65`：长连接超时时间

### 压缩配置
- `gzip_comp_level 6`：压缩级别（1-9，越高CPU消耗越大）
- `gzip_min_length 1024`：最小压缩文件大小

### 缓存配置
- 静态资源缓存30天
- API请求不缓存，确保数据实时性

## 监控和日志

### 日志文件位置
- 访问日志：`/var/log/nginx/deepfake_access.log`
- 错误日志：`/var/log/nginx/deepfake_error.log`
- Nginx主日志：`/var/log/nginx/error.log`

### Docker环境日志
```bash
# 查看Nginx日志
docker-compose logs nginx

# 实时查看日志
docker-compose logs -f nginx
```

## 故障排查

### 常见问题

1. **502 Bad Gateway**
   - 检查后端应用是否正常运行
   - 确认网络连接是否正常

2. **静态文件404**
   - 检查文件路径配置
   - 确认文件权限

3. **上传文件失败**
   - 检查文件大小限制配置
   - 确认磁盘空间

### 调试命令
```bash
# 测试Nginx配置
nginx -t

# 重新加载配置
nginx -s reload

# 查看Nginx进程
ps aux | grep nginx

# 查看端口占用
netstat -tlnp | grep :80
```

## 安全建议

1. 定期更新Nginx版本
2. 配置防火墙规则
3. 使用HTTPS加密传输
4. 隐藏Nginx版本信息（已配置`server_tokens off`）
5. 配置访问频率限制
6. 监控异常访问日志