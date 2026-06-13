# 农产品溯源系统

农产品溯源系统包含 Spring Boot 后端和 Vue 3 前端，支持产品管理、批次管理、生产记录、质检记录、物流轨迹、统计分析、Session/Cookie 登录和头像上传。

## 项目结构

```text
class01/
  backend/   Spring Boot 后端服务
  frontend/  Vue 3 + Vite 前端项目
```

## 环境要求

- JDK 17 或更高版本（JDK 21 可直接运行）
- Maven 3.8+
- Node.js 18+
- MySQL 8+

## 数据库初始化

1. 创建并导入数据库脚本：

```bash
mysql -uroot -p < backend/src/main/resources/schema.sql
```

2. 默认数据库配置在 `backend/src/main/resources/application.yml`：

```text
数据库：agriculture_trace
用户名：root
密码：123456
端口：3306
```

也可以通过环境变量覆盖：

```bash
MYSQL_URL=jdbc:mysql://localhost:3306/agriculture_trace?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=123456
```

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 测试账号

密码均为：`123456`

```text
admin      管理员
farmer     农户
inspector  监管员
```

## 提交说明

仓库已经配置 `.gitignore`，会忽略本地 IDE 配置、构建产物、`node_modules`、`target`、运行日志和上传头像文件。`backend/pom.xml`、`frontend/package.json`、`frontend/package-lock.json` 等运行必需文件会保留上传。
