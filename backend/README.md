# 农产品溯源系统后端

后端基于 Spring Boot 3.5.13、JDK 17、MySQL 8 实现，使用 Spring Security Session 认证、Spring Data JPA 处理业务 CRUD、MyBatis 处理统计查询。

## 启动前准备

1. 安装并启动 MySQL 8，确保当前 MySQL 用户有创建数据库和建表权限。
2. 设置环境变量，或使用默认本地配置。默认配置会在首次启动时自动创建 `agriculture_trace` 数据库，并执行 `src/main/resources/schema.sql` 补齐表和演示数据：
   - `MYSQL_URL=jdbc:mysql://localhost:3306/agriculture_trace?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true`
   - `MYSQL_USERNAME=root`
   - `MYSQL_PASSWORD=123456`
3. 启动：`mvn spring-boot:run`

测试账号均为 `123456`：`admin`、`farmer`、`inspector`。

## 区块链相关实现

本模块围绕当前项目表结构实现数据指纹、根哈希和审计日志链校验：

- `HashUtil.java`：SHA-256 哈希工具。
- `Product.dataHash`：产品数据指纹，对应数据库 `product.data_hash`。
- `Batch.dataHash`：批次数据指纹，对应数据库 `batch.data_hash`。
- `BlockchainLog.java`：审计日志实体，对应数据库 `blockchain_log`。
- `BlockchainLogRepository.java`：日志查询，包含 `findAllByOrderByTimestampAsc()` 和 `findLastLog()`。
- `IntegrityController.java`：数据指纹、根哈希、产品/批次单项验证接口。
- `BlockchainLogController.java`：审计日志分页列表和链条完整性验证接口。
- `BlockchainSchemaInitializer.java`：兼容已有数据库，启动时补齐哈希字段、日志表和初始化日志。

## 哈希规则

产品指纹由以下字段计算：

```text
id | name | category | origin | price | create_time
```

批次指纹由以下字段计算：

```text
id | batch_no | product_id | production_date | remark | create_time
```

审计日志哈希由以下内容计算：

```text
action_type + target_id + operator + timestamp + previous_hash + data_after
```

第一条审计日志的 `previous_hash` 为 `"0"`，后续每条日志的 `previous_hash` 必须等于上一条日志的 `data_hash`。

## 主要接口

```text
GET  /api/product/list
POST /api/product
PUT  /api/product
DELETE /api/product/{id}

GET  /api/batch/list
POST /api/batch
PUT  /api/batch
DELETE /api/batch/{id}

GET  /api/integrity/fingerprints
GET  /api/integrity/root-hash
GET  /api/integrity/verify/{id}
GET  /api/integrity/products/verify
GET  /api/integrity/batch/{id}/verify
GET  /api/integrity/batches/verify

GET  /api/blockchain/logs?page=1&pageSize=10
GET  /api/blockchain/logs/verify
DELETE /api/users/{id}
```

`/api/integrity/fingerprints`、`/api/integrity/products`、`/api/integrity/root-hash` 和 `/api/blockchain/**` 仅管理员、监管员可访问；产品/批次页面上的单条哈希验证接口登录后即可使用。

## 校验说明

- 正常通过系统新增/编辑产品或批次，会自动重新计算 `data_hash`，因此校验应为一致。
- 直接修改数据库业务字段但不更新 `data_hash`，会触发数据指纹异常。
- 只修改 `product` 或 `batch` 不会破坏日志链，因为日志链校验的是 `blockchain_log` 表自身。
- `/api/blockchain/logs/verify` 会先校验日志链，再联动检查当前产品/批次业务数据指纹。

## 运行测试

```bash
mvn test
```
