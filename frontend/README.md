# 农产品溯源系统前端

前端基于 Vue 3、Vite、Element Plus、Vue Router、Axios、ECharts 实现。

## 启动

```bash
npm install
npm run dev
```

开发环境默认地址：`http://localhost:5173`

接口地址配置在 `.env.development`：

```text
VITE_API_BASE_URL=http://localhost:8080
```

## 构建

```bash
npm run build
```

## 请求封装

项目统一使用 `src/utils/request.js` 封装 Axios：

- `baseURL` 为 `${VITE_API_BASE_URL}/api`
- `withCredentials: true`，用于携带 `JSESSIONID`
- 响应拦截器会把后端 `Result` 拆包后返回给页面
- 统一处理 `401` 和网络异常

因此页面中通常这样使用：

```js
const res = await getProductFingerprints()
if (res.code === 200) {
  records.value = res.data.records
}
```

不是直接读取原生 Axios 的 `response.data.code`。

## 主要页面

```text
src/views/Dashboard.vue                  数据概览
src/views/ProductList.vue                产品管理
src/views/BatchList.vue                  批次管理
src/views/IntegrityReport.vue            数据指纹
src/views/blockchain/AuditLog.vue        审计日志
src/views/Statistics.vue                 统计分析
src/views/UserManagement.vue             用户管理
src/views/TraceDetail.vue                溯源详情
```

## 区块链相关页面

### 数据指纹

路由：`/integrity`

文件：`src/views/IntegrityReport.vue`

功能：

- 展示所有产品的存储指纹和当前指纹。
- 展示全局根哈希。
- 标记一致/异常状态。
- 支持单个产品哈希验证。

### 审计日志

路由：`/blockchain/audit-log`

文件：`src/views/blockchain/AuditLog.vue`

功能：

- 分页展示审计日志。
- 展示操作时间、操作人、操作类型、对象、上一哈希、本条哈希。
- 查看操作前/操作后 JSON 详情。
- 点击“验证链条完整性”会校验日志链，并联动检查业务数据指纹异常。

## 区块链接口封装

```text
src/api/integrity.js
  getProductFingerprints()
  getRootHash()
  verifyProductHash(id)
  verifyBatchHash(id)

src/api/blockchain.js
  getAuditLogs(params)
  verifyAuditLogChain()
```

## 模拟篡改验证

通过系统页面正常编辑产品/批次时会自动重算哈希，因此会保持一致。要验证异常效果，需要直接改数据库业务字段但不更新 `data_hash`：

```sql
UPDATE product
SET name = '篡改'
WHERE id = 'prod_1';
```

刷新“数据指纹”页面后，该产品应显示“异常”。再进入“审计日志”点击“验证链条完整性”，会提示日志链完整但业务数据存在指纹异常。
