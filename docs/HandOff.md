# HandOff — AAA / Velrix（M0 + M1 仓库线）

> **M0 v0.2** 仍有效（JWT、Redis 权限缓存 R0-6）。  
> **M1 v0.1**：已完成 **T0 + 仓库/库位/容量 T2**（Flyway **V17–V20**）。  
> 更新：**2026-10-17**。规格：`docs/M1-主数据需求.md`。

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot 4.1.1、Java 17、MySQL 8、Flyway、MyBatis-Plus、Redis、JWT
- **包**：`com.velrix.platform`（M0）、`com.velrix.masterdata`（M1）

---

## M1 已落地（仓库线 T2）

| 项 | 说明 |
|----|------|
| DDL | V17 `md_product`；V18 `md_warehouse` / `md_location`；V19 `md_location_capacity`（FK **RESTRICT**）；V20 ERP 菜单 2200+ |
| 商品 API | `GET/POST/PUT /api/products`（支撑容量配置） |
| 仓库 API | `GET/POST/PUT /api/warehouses`，`PATCH …/status`，`GET …/locations`，`POST …/locations` 批量 |
| 库位 API | `GET/PUT/PATCH/DELETE /api/locations/{id}`，`PUT …/capacities` 覆盖保存 |
| 规则 | `WarehouseReadinessPolicy` + `WarehouseGuard#requireActiveForInbound`；仓停用 **不改** 库位 status，**冻结** 子资源写操作 |
| 列表 | `inboundReady` / `warehouseFrozen`；库位 `storageLabel`（M2 `LocationOccupancyQuery` 接口 + **Stub** 空实现） |
| 审计 | `MasterdataAuditWriter` → `sys_audit_log` |
| ID | `com.velrix.shared.id.IdGenerator`（雪花，与 MP 一致） |

**设计定稿（与 rubber-duck 一致）**：编码 `trim+大写` → `code_norm`；库位 **description**；容量 **联合主键**；批量/容量 **整单失败 + 多错误拼接**；v0.1 **无仓库类型**。

---

## M1 未做（建议 T3→T4）

- `MasterDataImpactService` / Contributor、商品/客户/供应商完整 CRUD + impact、`PATCH` 商品启停
- `LocationGuard`、客户/供应商 Guard
- M2 真实 `LocationOccupancyQuery` 实现
- 集成测试、前端 T6

---

## M0 要点（摘录）

- Flyway 曾至 V16；M1 从 **V17** 起
- 鉴权：`@RequirePerm`；管理员 `is_administrator=1` 拥有全部 perm
- 测试：改库/全量 `mvn test` 前先问用户；集成需 MySQL + Redis

---

## 运行

- 启动：`VelrixApplication`（Flyway 自动跑 V17–V20）
- 编译：`.\mvnw.cmd compile -DskipTests`

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `tdd` | T3 impact/Guard 单测 |
| `learning-mentor` / `rubber-duck` | 客户/供应商线或 impact 设计 |
| `handoff` | 下一阶段再覆盖本文 |
