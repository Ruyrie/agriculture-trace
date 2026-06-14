# 农产品溯源系统

本项目是一个前后端分离的农产品溯源系统，包含 Spring Boot 后端和 Vue 3 前端。系统支持产品管理、批次管理、生产/质检/物流溯源、统计分析、用户权限、Session/Cookie 登录、头像上传、数据指纹和审计日志链校验。

## 核心功能

- 数据概览：统计产品、批次、溯源记录和近期趋势。
- 产品管理：产品增删改查、分页搜索、产品数据指纹 `data_hash`、单个/批量产品哈希验证。
- 批次管理：批次增删改查、按产品筛选、批次数据指纹、单个/批量哈希验证。
- 溯源详情：展示生产记录、质检记录、物流记录和二维码访问入口。
- 数据指纹：展示所有产品的存储指纹、当前指纹、全局根哈希和异常状态，并支持复制根哈希。
- 审计日志：记录产品/批次 CREATE、UPDATE、DELETE 操作，形成 `previous_hash -> data_hash` 的链式日志，并支持分页展示和完整性验证。
- 用户管理：管理员维护用户、角色、状态和密码。

## 区块链增强说明

本项目结合现有数据库结构实现文档中的区块链模拟功能。文档示例中的 `t_product`、`t_blockchain_log` 在当前项目中对应为：

```text
t_product          -> product
t_blockchain_log   -> blockchain_log
```

实现点：

- `product.data_hash`：产品数据 SHA-256 指纹。
- `batch.data_hash`：批次数据 SHA-256 指纹。
- `blockchain_log.previous_hash`：上一条审计日志哈希，第一条日志为 `"0"`。
- `blockchain_log.data_hash`：当前日志哈希。
- 全局根哈希：由当前产品指纹组合后计算，用于模拟整体数据完整性校验。

注意：

- “数据指纹”验证的是当前业务表数据是否和存储指纹一致。
- “审计日志链”验证的是日志表本身是否连续、是否被篡改。
- 当前审计日志验证已同时返回业务数据指纹状态：如果日志链完整但产品/批次被直接改库，会提示业务数据指纹异常。
- 通过系统正常新增/编辑产品或批次时会自动重算指纹，所以要模拟篡改，需要直接修改数据库业务字段但不更新 `data_hash`。

示例：

```sql
UPDATE product
SET price = 100
WHERE name = '有机苹果';
```

然后在“数据指纹”页刷新，或在“审计日志”页点击“验证链条完整性”，即可看到异常提示。

## 项目结构

```text
class01/
  backend/                 Spring Boot 后端服务
    src/main/java/...      控制器、服务、实体、仓库、配置
    src/main/resources/    application.yml、schema.sql、MyBatis mapper
  frontend/                Vue 3 + Vite 前端项目
    src/api/               前端接口封装
    src/views/             页面
    src/views/blockchain/  区块链审计日志页面
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.8+
- Node.js 18+
- MySQL 8+

## 数据库初始化

创建并导入数据库脚本：

```bash
mysql -uroot -p < backend/src/main/resources/schema.sql
```

默认数据库配置位于 `backend/src/main/resources/application.yml`：

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

当前项目 `ddl-auto` 为 `none`。为兼容已有数据库，后端启动时会检查并补齐 `product.data_hash`、`batch.data_hash` 和 `blockchain_log` 表，并在审计日志为空时为已有产品/批次生成初始化日志。

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

前端开发环境接口地址配置在 `frontend/.env.development`：

```text
VITE_API_BASE_URL=http://localhost:8080
```

## 测试账号

密码均为：`123456`

```text
admin      管理员
farmer     农户
inspector  监管员
```

## 角色菜单

```text
farmer     数据概览、产品管理、批次管理
inspector  数据概览、产品管理、批次管理、数据指纹、审计日志、统计分析
admin      数据概览、产品管理、批次管理、数据指纹、审计日志、统计分析、用户管理
```

## 主要页面

```text
/dashboard                  数据概览
/products                   产品管理
/batches                    批次管理
/integrity                  数据指纹
/blockchain/audit-log       审计日志
/statistics                 统计分析
/users                      用户管理
/trace/:id                  产品溯源详情
/trace/batch/:batchId       批次溯源详情
```

## 主要接口

```text
GET  /api/integrity/fingerprints       产品指纹列表和全局根哈希
GET  /api/integrity/root-hash          全局根哈希
GET  /api/integrity/verify/{id}        单个产品哈希验证
GET  /api/integrity/products/verify    全部产品哈希验证，返回异常明细
GET  /api/integrity/batch/{id}/verify  单个批次哈希验证
GET  /api/integrity/batches/verify     全部批次哈希验证，返回异常明细
GET  /api/blockchain/logs              审计日志分页列表
GET  /api/blockchain/logs/verify       审计日志链和业务数据指纹验证
DELETE /api/users/{id}                 管理员删除用户，不能删除当前登录账号
```

`/api/integrity/fingerprints`、`/api/integrity/products`、`/api/integrity/root-hash` 和 `/api/blockchain/**` 仅管理员和监管员可访问；产品/批次页面上的单条哈希验证接口登录后即可使用。

## 验证命令

后端：

```bash
cd backend
mvn test
```

前端：

```bash
cd frontend
npm run build
```

## 提交说明

仓库已经配置 `.gitignore`，会忽略本地 IDE 配置、构建产物、`node_modules`、`target`、运行日志和上传头像文件。`backend/pom.xml`、`frontend/package.json`、`frontend/package-lock.json` 等运行必需文件会保留上传。
