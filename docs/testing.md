# 本地测试与数据脚本

本文档汇总 `backend/scripts/` 下的测试/运维脚本及常用命令，便于本地联调时快速重置数据或修复环境。

相关文档：[订单状态流转](./order_status.md) · [后端启动说明](../backend/README.md)

---

## 前置条件

1. MySQL 容器已启动：

```bash
cd backend/docker && docker compose up -d
```

2. 容器与数据库信息（见 `backend/docker/docker-compose.yml`）：

| 项 | 值 |
|----|-----|
| 容器名 | `vwholesale-mysql` |
| 数据库 | `vwholesale` |
| 用户 / 密码 | `vwholesale` / `vwholesale` |
| 端口 | `3306` |

3. 在项目根目录执行 SQL 的通用写法：

```bash
docker exec -i vwholesale-mysql mysql -u vwholesale -pvwholesale vwholesale \
  < backend/scripts/<脚本名>.sql
```

> 密码紧跟 `-p` 后，中间无空格。

---

## 测试账号（开发登录）

后端未配置微信时，使用开发登录接口：

```bash
curl -X POST http://localhost:8080/api/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"openid":"dev-owner-001","nickname":"老板","role":"OWNER_ADMIN"}'
```

| 角色 | openid 示例 | 说明 |
|------|-------------|------|
| 老板 | `dev-owner-001` | 订单/录价/采购等老板端 |
| 客户 | 绑定 `customer_id=4` 的用户 | 原测试客户「张记」已迁至 id=4 |

小程序登录页使用对应 openid 或已绑定客户账号即可。

---

## 脚本速查

| 脚本 | 用途 | 影响范围 | 常用场景 |
|------|------|----------|----------|
| [clear-all-orders.sql](../backend/scripts/clear-all-orders.sql) | 清空订单并重置库存为 100 | 订单及关联表；**保留**商品/客户/用户 | 重复测下单→确认→拣单流程 |
| [seed-random-stock.sql](../backend/scripts/seed-random-stock.sql) | 随机库存 0~50 | 仅 `products.stock_qty` | 测库存不足提示 |
| [backfill-pick-stock.sql](../backend/scripts/backfill-pick-stock.sql) | 补扣历史拣单未同步的库存 | `products` + `order_items.stock_applied_qty` | 升级库存逻辑后一次性修复 |
| [reset-products-from-catalog.sql](../backend/scripts/reset-products-from-catalog.sql) | 从品名目录重建分类与商品 | **删除**订单、价格、商品、分类后重建 | 全量重置商品目录 |
| [import-products-from-xls.py](../backend/scripts/import-products-from-xls.py) | 从 `品名.xls` 生成上述 SQL | 生成文件，不直接改库 | 商品目录变更时重新生成 SQL |
| [migrate-customer-1-to-4.sql](../backend/scripts/migrate-customer-1-to-4.sql) | 客户 id 1 → 4 | 客户及关联外键 | 与 Flyway V19 等效的手动脚本（一般仅执行一次） |
| [fix-flyway-v13.sql](../backend/scripts/fix-flyway-v13.sql) | 修正 V13 checksum | `flyway_schema_history` | 手动改过 V13 后 Flyway 启动失败 |
| [fix-flyway-v14.sql](../backend/scripts/fix-flyway-v14.sql) | 修正 V14 checksum | `flyway_schema_history` | 手动改过 V14 后 Flyway 启动失败 |
| [dev-env.sh](../backend/scripts/dev-env.sh) | 设置 JDK 21 环境变量 | 无 | `source backend/scripts/dev-env.sh` 后启动 Maven |

---

## 常用测试流程

### 1. 清空订单，保留商品与客户（最常用）

清空所有订单、明细、收款、分拣日志，并将**所有在售商品库存设为 100**：

```bash
docker exec -i vwholesale-mysql mysql -u vwholesale -pvwholesale vwholesale \
  < backend/scripts/clear-all-orders.sql
```

脚本末尾会输出各表行数及库存 min/max，便于确认。

**保留：** 商品、分类、客户、用户、供应商等主数据。  
**删除：** `orders`、`order_items`、`payments`、`dispatch_logs`、`purchase_payments`。

---

### 2. 模拟随机库存（测缺货）

```bash
docker exec -i vwholesale-mysql mysql -u vwholesale -pvwholesale vwholesale \
  < backend/scripts/seed-random-stock.sql
```

每个在售商品 `stock_qty` 为 0~50 的随机值（2 位小数）。

---

### 3. 全量重置商品目录（慎用）

会**删除全部订单与商品数据**，再从 `品名.xls` 导出的目录重建 3 大类约 136 个商品：

```bash
docker exec -i vwholesale-mysql mysql -u vwholesale -pvwholesale vwholesale \
  < backend/scripts/reset-products-from-catalog.sql
```

若 Excel 源文件有更新，先重新生成 SQL：

```bash
cd backend/scripts
python3 import-products-from-xls.py /path/to/品名.xls
# 默认输出 reset-products-from-catalog.sql
```

再执行上面的 `docker exec` 导入。

---

### 4. 补扣历史拣单库存

适用于早期分拣未扣库存、或 `stock_applied_qty` 未回填的数据：

```bash
docker exec -i vwholesale-mysql mysql -u vwholesale -pvwholesale vwholesale \
  < backend/scripts/backfill-pick-stock.sql
```

---

## Flyway checksum 修复

若曾**手动执行**过与 Flyway 迁移相同的 DDL，启动时可能出现 checksum 不一致。按报错版本执行对应脚本：

```bash
# V13
docker exec -i vwholesale-mysql mysql -u vwholesale -pvwholesale vwholesale \
  < backend/scripts/fix-flyway-v13.sql

# V14
docker exec -i vwholesale-mysql mysql -u vwholesale -pvwholesale vwholesale \
  < backend/scripts/fix-flyway-v14.sql
```

其他版本需在 `flyway_schema_history` 中对照 JAR 内 migration 文件重新计算 checksum，或避免手动改已发布 migration 文件内容。

---

## 推荐联调顺序（订单全流程）

1. `clear-all-orders.sql` — 干净订单 + 库存 100  
2. 客户端下单 → 老板端「待确认」  
3. 详情页「预览订单」— 确认商品与数量（未录价）  
4. 「确认 / 拣单」— 扣库存  
5. 录价 → 发送对账单 → 标记收款  

状态说明见 [order_status.md](./order_status.md)。

---

## 老板端 API 冒烟测试（近期功能）

客户/供应商/报表/采购等接口一键验证：

```bash
cd backend
BASE_URL=http://localhost:8080 ./scripts/smoke-test-boss-apis.sh
```

用例与结果见 [smoke-test-report.md](./smoke-test-report.md)。**改后端代码后须重启 Spring Boot**，否则新接口（供应商报表、库存报表）会 404/500。

---

## 注意事项

- 以上脚本**仅用于本地/测试环境**，不要在生产库直接执行 `DELETE` / 全量重置类脚本。
- 执行 SQL 前建议确认容器名：`docker ps | grep vwholesale-mysql`。
- 修改数据库后，小程序端下拉刷新或重新进入页面即可；无需重启后端（Flyway 修复除外）。
- `reset-products-from-catalog.sql` 与 `clear-all-orders.sql` 会清空订单，执行前确认无需要保留的测试数据。
