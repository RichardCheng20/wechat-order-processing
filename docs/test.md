# 测试文档

完整测试脚本与命令说明见：**[testing.md](./testing.md)**  
文档索引：[README.md](./README.md)

常用一键清空订单：

```bash
docker exec -i vwholesale-mysql mysql -u vwholesale -pvwholesale vwholesale \
  < backend/scripts/clear-all-orders.sql
```
