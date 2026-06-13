# 农产品溯源系统后端

本模块按照需求规格说明书实现：Spring Boot 3.5.13、JDK 17、MySQL 8、Spring Security Session 认证、JPA 业务 CRUD、MyBatis 统计查询。

## 启动前准备

1. 创建 MySQL 数据库并导入 `src/main/resources/schema.sql`。
2. 设置环境变量，或使用默认本地配置：
   - `MYSQL_URL=jdbc:mysql://localhost:3306/agriculture_trace?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`
   - `MYSQL_USERNAME=root`
   - `MYSQL_PASSWORD=123456`
3. 启动：`mvn spring-boot:run`

测试账号均为 `123456`：`admin`、`farmer`、`inspector`。
