# HandOff — AAA / Velrix（M0 + M1 主数据线）

> **M0 v0.2** 仍有效（JWT、Redis 权限缓存 R0-6）。  
> **M1**：T0 + 仓库/库位/容量 T2（V17–V20）+ T3 商品启停/删除 + 引用影响 SPI（V21）+ **T4 客户/供应商 CRUD + impact**（V22/V23）已完成。  
> 更新：**2026-10-25**。规格：`docs/M1-主数据需求.md`；链路教学导览：`docs/M1-链路导览.md`。

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot 4.1.1、Java 17、MySQL 8、Flyway、MyBatis-Plus、Redis、JWT
- **包**：`com.velrix.platform`（M0）、`com.velrix.masterdata`（M1）、`com.velrix.shared`

---

## M1 已落地

### T2（仓库线，V17–V20）

| 项 | 说明 |
|----|------|
| DDL | V17 `md_product`；V18 `md_warehouse` / `md_location`；V19 `md_location_capacity`（FK **RESTRICT**）；V20 ERP 菜单 2200+ |
| 商品 API | `GET/POST/PUT /api/products` |
| 仓库 API | `GET/POST/PUT /api/warehouses`，`PATCH …/status`，`GET …/locations`，`POST …/locations` 批量 |
| 库位 API | `GET/PUT/PATCH/DELETE /api/locations/{id}`，`PUT …/capacities` 覆盖保存 |
| 规则 | `WarehouseReadinessPolicy` + `WarehouseGuard#requireActiveForInbound`；仓停用**冻结**子资源写操作 |
| 审计 | `MasterdataAuditWriter` → `sys_audit_log` |

### T3（商品启停/删除 + impact，V21，本会话完成）

| 项 | 说明 |
|----|------|
| impact SPI | `application/impact/`：`MasterDataKind`（PRODUCT/WAREHOUSE/CUSTOMER/SUPPLIER/LOCATION）、`ImpactItem`、`ImpactSummary`、`MasterDataImpactContributor`、`MasterDataImpactService`（EnumMap 分组聚合，GET impact 与 DELETE 409 共用）；Contributor 不返回 count=0 项 → `deletable = items.isEmpty()` |
| 本地 Contributor | `ProductImpactContributor`（库位容量配置）、`WarehouseImpactContributor`（下挂库位） |
| 409 通道 | `ApiCodes.BIZ_CONFLICT` + `ConflictException` + `GlobalExceptionHandler` → HTTP 409 |
| 商品 API 新增 | `PATCH /api/products/{id}/status`（`product:status`，审计 action `MD_PRODUCT_STATUS`）、`GET …/impact`（`product:list`）、`DELETE …/{id}`（`product:delete`） |
| 仓库 API 新增 | `GET /api/warehouses/{id}/impact`（`warehouse:list`） |
| 删除语义（D2 定稿） | DELETE 有引用→409+分类型明细；**无引用也 409**（"请使用停用"，主数据不物理删） |
| DTO 校验（后补） | 全部写 DTO 加 Bean Validation（`@NotBlank`/`@NotEmpty`/`@NotNull`+`@DecimalMin`，嵌套 `@Valid`），Controller `@Valid` 触发 → 400 `PARAM_INVALID` 字段级消息（`ApiCodes.PARAM_INVALID` + `MethodArgumentNotValidException`/`HttpMessageNotReadableException` handler）。**service 层与 DTO 重复的空值检查已砍**（Product/Warehouse prepare、Location update/createBatch/replaceCapacities，各留注释）；保留：规范化后空值、批内重复、DB 查重、requireActive、存在性。service 契约变为"调用方保证非空"。注：`spring-boot-starter-validation` 依赖 pom 里早就有，此前未用 |
| Guard | `ProductGuard.requireActive`（从 ProductService 抽出，LocationService 已改引用）；`LocationGuard.requireBelongsToWarehouse` |
| 种子 | V21：2214 `product:status`、2215 `product:delete`（接 V20 的 2210 商品管理段） |
| 单测 | **39 个纯 Mockito 测试全绿**（不起 Spring、不需库）：T3 20 个 + T4 19 个（`CustomerServiceTest`(8)、`SupplierServiceTest`(5)、`CustomerGuardTest`(3)、`SupplierGuardTest`(3)） |
| 跑法 | `.\mvnw.cmd test -Dtest="ProductServiceTest,ProductGuardTest,LocationGuardTest,WarehouseGuardTest,MasterDataImpactServiceTest,CustomerServiceTest,SupplierServiceTest,CustomerGuardTest,SupplierGuardTest"` |

**设计定稿（历史）**：编码 `trim+大写`→`code_norm`；库位 description；容量联合主键；批量/容量整单失败+多错误拼接；v0.1 无仓库类型。

---

### T4（客户/供应商 CRUD + impact，V22/V23，本会话完成）

| 项 | 说明 |
|----|------|
| DDL | V22 `md_customer` / `md_supplier`：主数据骨架（id/code/code_norm UK/name/status/审计列）+ `contact_name`/`contact_phone`/`address`（NULL 可选，DTO 不加 @NotBlank，service 空白归 null） |
| API | `GET/POST/PUT /api/customers`、`PATCH …/status`、`GET …/impact`、`DELETE …/{id}`（409）；`/api/suppliers` 同构 |
| 权限 | V23：2220 客户段 / 2230 供应商段（十位区分模块），`customer:*`/`supplier:*` 五权限（status 与 update 分离：敏感度不同） |
| Guard | `CustomerGuard`/`SupplierGuard.requireActive`（对标 ProductGuard，M1-F-23 接口先交付，M3 下单门禁调用） |
| impact | `CustomerImpactContributor`/`SupplierImpactContributor` 空实现（M1 无本地引用，M3 订单注册即生效）；Service 走 `MasterDataImpactService.summarize` 聚合 |
| 删除 | D2 定稿照搬：有引用 409 带明细；无引用也 409「请使用停用」（防孤儿数据/可回滚/可溯源） |
| 分层 | 用户手写实体/DTO/Service/Controller（导师模式引导），代理写 SQL/测试 |
| 单测 | 19 个纯 Mockito 测试全绿：`CustomerServiceTest`(8)、`SupplierServiceTest`(5)、`CustomerGuardTest`(3)、`SupplierGuardTest`(3)；mock insert 需手动回填雪花 id（`write(..., long, ...)` 拆箱） |
| 冒烟 | 13 发全过：codeNorm 大写、大小写查重、双层校验（DTO 拦空/空格，service 拦规范化后空值）、启停、impact 空 items、DELETE 409、菜单/权限入 me |

**T4 设计讨论定稿（导师会话推导）**：客户/供应商=被单据引用的主数据档案（联系=未来订单，不建中间表）；价格不入档案（成交快照归流水）。

---

## M1 未做（建议 T5→T6）

- **T5**：集成测试（需 MySQL + Redis）
- **T6**：前端
- M2 真实 `LocationOccupancyQuery` 实现（现为 Stub）；M3 订单线（将消费 CustomerGuard/SupplierGuard + 注册 impact Contributor）
- 未提交 git（用户未要求 commit）；`feature/jwt-device-claim` 未合回 master

---

## M0 要点（摘录）

- Flyway 至 V21；鉴权 `@RequirePerm`；管理员 `is_administrator=1` 拥有全部 perm
- 测试：改库/全量 `mvn test` 前先问用户；集成需 MySQL + Redis

---

## 运行

- 启动：`VelrixApplication`（Flyway 自动跑 V17–V21）
- 编译：`.\mvnw.cmd compile -DskipTests`

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `tdd` | T5 集成测试、M3 订单线 |
| `learning-mentor` / `rubber-duck` | M2 占用查询设计、M3 订单模块设计（T4 即此模式完成） |
| `handoff` | 下一阶段再覆盖本文 |
