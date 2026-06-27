# 项目文档索引

> 最后整理：2026-06-27  
> **产品规格以 [vegetable-wholesale-miniapp-prd.md](./vegetable-wholesale-miniapp-prd.md) v1.0 为准**；订单状态以 [order_status.md](./order_status.md) 为准。

## 快速导航

| 我想… | 看这里 |
|--------|--------|
| 本地跑起来 | [根目录 README](../README.md) · [backend/README](../backend/README.md) · [frontend/README](../frontend/README.md) |
| 理解订单怎么流转 | [order_status.md](./order_status.md) |
| 客户怎么进来（三种码） | [customer-onboarding.md](./customer-onboarding.md) |
| 本地清数据 / 联调脚本 | [testing.md](./testing.md) |
| 部署到腾讯云 | [腾讯云上线分步操作指南](./腾讯云上线分步操作指南.md) · [开发联调与生产部署指南](./开发联调与生产部署指南.md) |
| 查 API | 启动后端后打开 http://localhost:8080/doc.html |

## 文档清单

### 产品与需求

| 文件 | 说明 | 状态 |
|------|------|------|
| [vegetable-wholesale-miniapp-prd.md](./vegetable-wholesale-miniapp-prd.md) | **产品 PRD v1.0**（As-Built 功能规格） | **当前有效** |
| [pre-development-checklist.md](./pre-development-checklist.md) | 开工前决策清单 | 已归档，决策大多已落地 |
| [customer-onboarding.md](./customer-onboarding.md) | 档口码 / VIP 码 / 注册邀请 | **当前有效** |

### 技术与实现

| 文件 | 说明 | 状态 |
|------|------|------|
| [technical-architecture.md](./technical-architecture.md) | 技术选型与模块设计 v0.3 | 文首有「实现现状」对照；包结构/API 路径以代码为准 |
| [order_status.md](./order_status.md) | 六步业务状态 + 内部码映射 | **当前有效** |
| [testing.md](./testing.md) | 本地 SQL 脚本、dev-login、测试流程 | **当前有效** |
| [test.md](./test.md) | 指向 testing.md 的短索引 | 有效 |
| [smoke-test-report.md](./smoke-test-report.md) | API 冒烟结果快照 | 历史记录 |
| [todo-upload-security.md](./todo-upload-security.md) | 上传文件商户隔离 TODO | 待办 |

### 部署与运维

| 文件 | 说明 | 状态 |
|------|------|------|
| [开发联调与生产部署指南.md](./开发联调与生产部署指南.md) | `deploy-prod.sh`、SSH 隧道、本机/远程切换 | **日常联调推荐** |
| [腾讯云上线分步操作指南.md](./腾讯云上线分步操作指南.md) | 分步上线 checklist | 含进度跟踪，部分条目需人工核对 |
| [腾讯云上云部署指南.md](./腾讯云上云部署指南.md) | 资源、架构、systemd、Nginx | 架构参考；`application-prod.yml` 在服务器不在仓库 |

## 仓库目录（与文档对应）

```text
docs/                     本文档目录
backend/                  Java 后端（Spring Boot 3 + Flyway）
  docker/                 MySQL + Redis + RabbitMQ
  scripts/                联调 SQL、冒烟脚本
frontend/                 uni-app 小程序 + H5
scripts/                  仓库级：deploy-prod.sh、smoke-api.sh
vegetable-wholesale-app/  已废弃（见 DEPRECATED.md）
```

## 实现范围摘要（PRD v1.0 已覆盖）

完整功能清单见 **[vegetable-wholesale-miniapp-prd.md](./vegetable-wholesale-miniapp-prd.md)**。相对立项草案的主要增量包括：

- 六步订单状态（待确认 → 已确认 → 已拣单 → 已录价 → 已对账 → 已收款）
- 库存管理、采购开单/记账、供应商、报价单
- 销售/采购收款、客户对账、数据平台报表
- 客户注册审核、VIP 专属码、档口长期下单码
- 图片订单人工对照录入、文字订单前端解析
- 档口老板 / 档口经理 / 配送员角色
- RabbitMQ 异步通知与统计缓存刷新

未实现或部分实现见 PRD **§18 已知缺口**（微信支付、后端 AI、派单 UI、auto_confirm 等）。
