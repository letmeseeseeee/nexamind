# NexaMind 部署指南

NexaMind 提供统一的 Docker 部署解决方案，支持一键启动多种环境配置。

## 🚀 快速开始

### 1. 一键启动（推荐）

```bash
cd deploy
./start.sh
```

脚本会自动引导您选择部署模式并完成环境配置。

### 2. 直接指定模式启动

```bash
# 本地开发环境
./start.sh local

# 生产环境
./start.sh production

# 外部数据库模式
./start.sh external

# 开发环境+管理工具
./start.sh dev
```

## 📦 部署模式说明

### 🔧 local - 本地开发环境
- **特点**: 内置 PostgreSQL，支持热重载
- **适用**: 日常开发调试
- **配置**: 基于 `.env.local.example`
- **服务**: 前端 + 后端 + 数据库 + 消息队列

### 🏭 production - 生产环境
- **特点**: 内置 PostgreSQL，生产优化配置
- **适用**: 中小型生产部署
- **配置**: 基于 `.env.production.example`
- **服务**: 前端 + 后端 + 数据库 + 消息队列

### 🔗 external - 外部数据库
- **特点**: 连接外部 PostgreSQL 数据库
- **适用**: 大型生产环境，数据库分离部署
- **配置**: 基于 `.env.external.example`
- **服务**: 前端 + 后端 + 消息队列

### 🛠 dev - 开发环境增强版
- **特点**: 本地开发 + 数据库管理工具
- **适用**: 需要数据库管理界面的开发场景
- **配置**: 基于 `.env.local.example`
- **服务**: 前端 + 后端 + 数据库 + 消息队列 + Adminer

## 🌐 服务访问地址

启动完成后，您可以通过以下地址访问服务：

- **前端应用**: http://localhost:3000
- **后端API**: http://localhost:8080
- **API文档**: http://localhost:8080/swagger-ui.html
- **健康检查**: http://localhost:8080/api/health
- **数据库管理** (仅dev模式): http://localhost:8081

## 👤 默认账号

### 管理员账号
- **邮箱**: admin@nexamind.local
- **密码**: admin123

### 测试账号 (仅开发环境)
- **邮箱**: test@nexamind.local
- **密码**: test123

## ⚙️ 配置文件说明

### 环境变量模板
- `.env.local.example` - 本地开发环境配置
- `.env.production.example` - 生产环境配置
- `.env.external.example` - 外部数据库配置

### 自定义配置
1. 复制对应的模板文件为 `.env`
2. 根据需要修改配置参数
3. 重新启动服务生效

## 🛠 常用操作

### 查看服务状态
```bash
docker compose ps
```

### 查看实时日志
```bash
# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f nexamind-backend
docker compose logs -f nexamind-frontend
```

### 重启服务
```bash
# 重启所有服务
docker compose restart

# 重启特定服务
docker compose restart nexamind-backend
```

### 停止服务
```bash
docker compose down
```

### 完全重建
```bash
docker compose down
docker compose up -d --build
```

## 📊 数据管理

### 数据持久化
数据存储在 Docker volumes 中：
- `postgres-data` - 数据库数据
- `rabbitmq-data` - 消息队列数据
- `storage-data` - 应用文件存储

### 数据备份
```bash
# 备份数据库
docker exec nexamind-postgres pg_dump -U nexamind_user nexamind > backup.sql

# 恢复数据库
docker exec -i nexamind-postgres psql -U nexamind_user nexamind < backup.sql
```

## 🔧 故障排查

### 常见问题

#### 1. 端口冲突
检查端口占用情况：
```bash
# 检查端口占用
lsof -i :3000  # 前端
lsof -i :8080  # 后端
lsof -i :5432  # 数据库
```

修改 `.env` 文件中的端口配置：
```env
FRONTEND_PORT=3001
BACKEND_PORT=8081
POSTGRES_PORT=5433
```

#### 2. 数据库连接失败
检查数据库服务状态：
```bash
docker compose logs postgres
```

验证数据库连接：
```bash
docker exec nexamind-postgres pg_isready -U nexamind_user
```

#### 3. 服务启动失败
查看详细错误日志：
```bash
docker compose logs nexamind-backend
```

检查配置文件语法：
```bash
docker compose config
```

### 调试模式

开发环境支持远程调试：
- **调试端口**: 5005
- **连接方式**: Remote JVM Debug

## 📈 性能优化

### 生产环境建议
1. **内存配置**: 根据服务器配置调整 JVM 参数
2. **数据库优化**: 配置合适的连接池大小
3. **日志级别**: 生产环境使用 `warn` 或 `error` 级别
4. **安全配置**: 修改默认密码和密钥

### 监控建议
- 启用健康检查监控
- 配置日志收集
- 监控容器资源使用情况

## 🔒 安全注意事项

### 生产环境必须修改的配置
- 数据库密码：`DB_PASSWORD`
- JWT密钥：`JWT_SECRET`
- 管理员密码：`NEXAMIND_ADMIN_PASSWORD`
- 消息队列密码：`RABBITMQ_PASSWORD`

### 网络安全
- 使用 HTTPS（通过反向代理）
- 限制数据库端口对外暴露
- 配置防火墙规则

## 📞 技术支持

如遇到问题，请检查：
1. [故障排查文档](../docs/deployment/TROUBLESHOOTING.md)
2. [项目README](../README.md)
3. [GitHub Issues](https://github.com/letmeseeseeee/nexamind/issues)

---

*更新时间: 2025-01-08*
