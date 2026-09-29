# Velrix 工作台 · Spring Boot + React 重写需求文档（后端练习版）

> 来源：从 `prroject/VelrixWorkHub`（.NET + Blazor + FreeSql + PostgreSQL 的模块化企业工作台）中提炼。
> 目的：不是 1:1 复刻，而是挑出**后端含金量最高**的业务，让你用 Spring Boot 手写一遍，把 CRUD 之外的状态机、事务、并发、幂等、账本、流程引擎、异步投递这些真功夫练扎实。
> 配套：模块顺序和 `plan.md` 的学习阶段（MySQL → Redis → MQ → ES → Docker/Nginx）一一对应，每个阶段都有可落地的业务场景来练。

---

## 0. 怎么用这份文档

1. 按第 2 节的里程碑顺序做，每个里程碑都是可以独立验收的一块。
2. 每个模块都有四部分：**业务规则（R-编号）**、**接口清单**、**验收用例（AC-编号）**、**练到的后端知识点**。先写规则对应的单元测试，再写接口，最后做前端页面。
3. 验收用例里标了 🔒 的是并发/一致性用例，必须写成**多线程集成测试**（Testcontainers + 真 MySQL），不能只在单元测试里 mock。
4. 前端只求能操作、能看状态，重点在后端，不要在 UI 上耗太多时间。

---

## 1. 选题思路：为什么选这些、砍掉哪些

原项目有 OA、CRM、ERP、PMS、MOM（制造）、LMS（许可证）、Workflow、Admin 八大块、上百个实体。按"对后端能力的锻炼价值"筛选：

| 模块 | 后端含金量 | 取舍 |
| --- | --- | --- |
| Workflow 流程引擎 | ★★★★★ 图校验、版本快照、并行汇聚、会签策略、乐观锁、幂等待办 | **必做，项目核心** |
| ERP 库存账本 | ★★★★★ 不可变流水、余额、批次 FIFO、序列号、防超卖、调拨事务 | **必做** |
| 采购/销售订单 | ★★★★ 状态机、部分收货/拒收/退货、跨模块联动库存 | **必做** |
| 应收应付核销 | ★★★★ 金额占用、审批中金额冻结、撤销 | **必做** |
| 额度占用（请假额度 / 付款预算） | ★★★★ Reserve → Consume / Release 三态模型，与审批联动 | **必做** |
| 通知中心 + Outbox | ★★★★ 去重幂等、事务提交后投递、失败重试退避、租约抢占 | **必做** |
| 平台底座（RBAC） | ★★★ 认证、菜单/按钮权限、审计 | **必做，但别陷进去** |
| 定时任务 + 库存预警 | ★★★ Cron、节假日跳过、分布式防重 | 必做 |
| 客户 360 / 全文搜索 | ★★★ 跨模块聚合、ES | 必做（放到 ES 阶段） |
| MRP 物料需求计算 | ★★★★ BOM 多层展开、净需求算法 | 选做（算法挑战） |
| CRM 客户/联系人/跟进 | ★★ 基本 CRUD | 只做客户主数据，其余砍掉 |
| MOM 制造全链、LMS、PMS、OA 资产/车辆/招聘… | ★★ 大量是重复的"单据 + 状态"模式 | **砍掉**，学会上面的模式后都是同一套路 |

**一句话总结：核心就是"单据状态机 + 流水账本 + 流程引擎 + 可靠通知"四件事，其它模块都是这四件事的排列组合。**

---

## 2. 总体范围与里程碑

```
M0 平台底座 ─► M1 主数据 ─► M2 库存账本 ─► M3 采购/销售订单 ─► M4 核销
                                                  │
M5 流程引擎 ◄─────────────────────────────────────┘（M3/M4 的审批接入 M5）
   │
   ├─► M6 额度占用（请假额度 / 付款预算）
   ├─► M7 通知中心 + Outbox
   └─► M8 定时任务 + 预警
M9 客户 360 / 全文搜索（ES 阶段）
选做：X1 MRP 物料需求计算、X2 多租户
```

| 里程碑 | 内容 | 对应 plan.md 阶段 | 重点练习 |
| --- | --- | --- | --- |
| M0 | 登录、JWT、RBAC、审计日志 | 阶段一 MySQL | 表设计、多对多、树形菜单查询 |
| M1 | 商品、仓库/库位、客户、供应商 | 阶段一 MySQL | 唯一索引、停用代替删除、引用影响查询 |
| M2 | 库存流水账本 | 阶段一 MySQL（事务与锁） | 行锁、死锁、索引、大分页 |
| M3 | 采购/销售订单 | 阶段一 MySQL | 状态机、跨聚合事务 |
| M4 | 应收应付核销 | 阶段一 MySQL | 金额计算、并发超额核销 |
| M5 | 流程引擎 | 阶段一 + 阶段二 Redis | 乐观锁、幂等、图算法、分布式锁 |
| M6 | 额度占用 | 阶段一 | 余额模型、补偿 |
| M7 | 通知 + Outbox | 阶段三 MQ | 事务消息、重试、死信、幂等消费 |
| M8 | 定时任务 + 预警 | 阶段二 Redis | 分布式调度防重 |
| M9 | 客户 360 / 搜索 | 阶段四 ES | Canal/MQ 同步、DSL |
| 部署 | Compose 一键起 | 阶段五 Docker/Nginx | 多阶段镜像、反向代理、限流 |

---

## 3. 技术栈与工程约定

### 3.1 技术栈建议

- 后端：Java 21、Spring Boot 3.x、Spring Security、MyBatis-Plus（复杂查询手写 XML SQL，方便练 SQL 和 EXPLAIN）、Flyway（数据库版本迁移）、Bean Validation、MapStruct。
- 数据库：MySQL 8（原项目用 PostgreSQL，你的学习计划是 MySQL，按 MySQL 做）。
- 中间件（按阶段逐步引入）：Redis、RabbitMQ 或 Kafka、Elasticsearch、Nginx。
- 测试：JUnit 5、AssertJ、Testcontainers（MySQL/Redis/MQ 真容器）、`CountDownLatch` 并发测试。
- 前端：React 18 + TypeScript + Vite + Ant Design + TanStack Query（接口缓存）+ React Router。流程图设计器可以用 React Flow。

### 3.2 分层约定（沿用原项目的思路）

原项目是模块化单体，每个模块分四层。Spring Boot 里建议按模块分包：

```
com.velrix
 ├─ platform      # 用户、角色、菜单、审计、字典、单号
 ├─ masterdata    # 商品、仓库、客户、供应商
 ├─ inventory     # 库存账本
 ├─ trade         # 采购单、销售单
 ├─ settlement    # 核销
 ├─ workflow      # 流程引擎
 ├─ quota         # 请假额度、付款预算
 ├─ notification  # 站内通知、Outbox
 └─ job           # 定时任务
每个模块内：domain（实体 + 领域规则）/ application（用例、事务）/ infrastructure（Mapper）/ web（Controller、DTO）
```

硬性规则（原项目的 AGENTS.md 里总结得很好，直接照搬）：

- **业务规则写在领域对象里**，例如 `purchaseOrder.receive(qty)` 自己校验状态和数量，Service 只负责编排和事务。不要把 if-else 都堆在 Service。
- **跨模块不直接读写对方的表**，只能调用对方的 Application 服务。例如销售发货要扣库存，只能调 `InventoryService.outbound(...)`，不能在销售模块里直接 `insert inventory_txn`。
- **流程引擎不碰业务表**。审批通过后怎么改业务状态，由业务模块注册回调（见 M5）。

### 3.3 数据约定

| 约定 | 要求 | 为什么 |
| --- | --- | --- |
| 金额、数量 | Java 用 `BigDecimal`，MySQL 用 `DECIMAL(18,2)` / 数量 `DECIMAL(18,6)` | 浮点误差；对应 plan.md Day5 |
| 枚举 | 数据库存**名称字符串**（`VARCHAR(50)`），不存数字 | 调整枚举顺序不会改变历史数据含义（原项目的硬规则） |
| 主键 | 选一种并写清理由：`BIGINT` 雪花 ID，或有序 UUID（UUIDv7） | 对应 plan.md 里自增 vs UUID 页分裂的讨论。原项目用 UUIDv7 |
| 单号 | 服务端生成，例如 `PO20260929-0001`，业务唯一索引 | 练 Redis `INCR` 或数据库号段 |
| 编码类字段 | 流程编码、计划号等统一 `trim + 大写` 后存 | 避免大小写不同产生两条数据 |
| 用户名比较 | 审批人、通知接收人一律**大小写无关** | 原项目多处踩过这个坑 |
| JSON | 全局一个 `ObjectMapper`，枚举序列化为名称，中文不转义 | 流程定义、快照都依赖 JSON |
| 时间 | `DATETIME(3)`，业务日期用 `DATE` | |

### 3.4 接口约定

- 统一响应：`{ "code": "OK", "message": "", "data": ... }`；业务错误用 `code = "BIZ_xxx"`，HTTP 409（状态冲突）或 422（校验失败）。
- 全局异常处理：领域规则抛 `BizException("采购订单已锁定，不能推进或取消订单")`，前端直接展示 message。
- 并发冲突统一返回 409 + `"数据已变化，请刷新后重试"`。
- 列表接口：普通列表用页码分页；**流水类大表用游标分页**（`?afterId=xxx&size=50`），练 plan.md Day12 的大分页优化。
- 所有写接口都记审计日志（谁、什么时间、改了哪个对象、前后值摘要）。

---

## 4. 模块需求

### M0 平台底座：认证、RBAC、审计

**目标**：能登录，不同角色看到不同菜单和按钮，所有写操作可追溯。

**核心表**

| 表 | 关键字段 |
| --- | --- |
| `sys_user` | id, username（唯一，大小写无关）, password_hash, display_name, org_id, enabled |
| `sys_role` | id, name, is_administrator |
| `sys_menu` | id, parent_id, label, path, type（`MENU` / `BUTTON`）, sort, hidden |
| `sys_user_role` / `sys_role_menu` | 多对多关联 |
| `sys_org` | id, parent_id, name（审批人按组织解析时要用） |
| `sys_audit_log` | id, actor, action, target_type, target_id, summary, occurred_at, ip |

**业务规则**

- R0-1 密码 BCrypt 存储；登录失败 5 次锁定 15 分钟（Redis 计数）。
- R0-2 Access Token 短期（30 分钟）+ Refresh Token 长期；退出登录后 Token 进黑名单（Redis，TTL = 剩余有效期）。
- R0-3 管理员角色拥有全部菜单和按钮；普通角色的菜单树要**自动补齐祖先节点**（只授权了子菜单，也要把父菜单带出来，否则前端树断掉）。
- R0-4 按钮权限用 `path` 作为权限码（如 `purchase-order:submit`），后端用注解 `@RequirePerm("purchase-order:submit")` 拦截，**前端隐藏按钮不算权限控制**。
- R0-5 角色授权变更要写权限审计（谁给哪个角色加/减了哪些菜单）。
- R0-6 用户权限缓存到 Redis，角色授权变更时主动失效。

**接口**

```
POST /api/auth/login            POST /api/auth/refresh        POST /api/auth/logout
GET  /api/me                    # 当前用户 + 菜单树 + 按钮权限码
CRUD /api/users  /api/roles  /api/menus(tree)  /api/orgs(tree)
PUT  /api/roles/{id}/menus      # 覆盖式授权
GET  /api/audit-logs?actor=&targetType=&from=&to=
```

**验收用例**

- AC0-1 只授权了"采购订单"子菜单的角色，`/api/me` 返回的树包含其父菜单"ERP"。
- AC0-2 没有 `purchase-order:submit` 权限的用户直接调提交接口，返回 403。
- AC0-3 退出登录后用旧 Token 访问，返回 401。

**练到**：Spring Security 过滤器链、JWT、树形结构查询（递归 CTE `WITH RECURSIVE`）、Redis 缓存与失效。

---

### M1 主数据：商品、仓库/库位、客户、供应商

**目标**：热身模块，但有两个 CRUD 之外的点：**停用代替删除**和**引用影响查询**。

**核心表**

| 表 | 关键字段 |
| --- | --- |
| `md_product` | id, code（唯一）, name, unit, status（`ACTIVE`/`INACTIVE`）, max_inventory_qty（超储阈值，可空）, batch_managed, serial_managed |
| `md_warehouse` | id, code（唯一）, name, address, status |
| `md_location` | id, warehouse_id, code（仓内唯一）, name |
| `md_location_capacity` | location_id, product_id, max_qty（某库位对某商品的容量上限） |
| `md_customer` / `md_supplier` | id, code, name, status, contact... |

**业务规则**

- R1-1 编码唯一；编辑时也要校验唯一。
- R1-2 **有历史引用的主数据禁止物理删除，只能停用**。停用后不能用于新单据（新建订单、登记流水时校验），但历史单据照常展示。
- R1-3 删除前调用"影响查询"，返回每类引用的数量，例如商品：采购单 3 张、销售单 5 张、库存流水 20 条、当前在库 12.00。只要有引用就拒绝删除，并提示"请停用并保留历史数据"。
- R1-4 库位必须属于仓库；库位容量 `max_qty > 0`。

**接口**

```
CRUD  /api/products  /api/warehouses  /api/customers  /api/suppliers
PATCH /api/products/{id}/status          # 启用/停用
GET   /api/products/{id}/impact          # 引用影响
POST  /api/warehouses/{id}/locations
PUT   /api/locations/{id}/capacities     # [{productId, maxQty}]
```

**验收用例**

- AC1-1 有库存流水的商品调删除，返回 409，提示里列出引用数量。
- AC1-2 停用的仓库不能登记入库。

**练到**：唯一索引与业务校验的双保险、`COUNT` 聚合、软删除 vs 停用的区别。

---

### M2 库存账本（重点模块之一）

**目标**：做一个**不可变流水账本**。库存不是一个随便改的数字，而是所有流水的累加结果。

**原项目的做法和你要改进的地方**

原项目每次查余额都是把**全部流水读到内存里 `GroupBy + Sum`**，演示数据量下没问题，但数据量一大就崩。重写时要求：

1. `inv_txn`（流水表）只插入不修改，是**唯一事实来源**。
2. 另建 `inv_balance`（余额表）作为物化结果，与流水**在同一个事务里**更新。
3. 写一个对账任务（M8），定期校验 `SUM(流水) == 余额`，不一致就告警。

这正好练 plan.md 阶段一的事务、行锁、死锁和索引。

**核心表**

```sql
CREATE TABLE inv_txn (
  id            BIGINT PRIMARY KEY,
  source_no     VARCHAR(80)  NOT NULL,          -- 流水单号，全局唯一（幂等键）
  kind          VARCHAR(50)  NOT NULL,          -- INBOUND / OUTBOUND / ADJUSTMENT
  product_id    BIGINT       NOT NULL,
  warehouse_id  BIGINT       NOT NULL,
  location_id   BIGINT       NULL,
  batch_no      VARCHAR(50)  NULL,
  expiry_date   DATE         NULL,
  serial_no     VARCHAR(80)  NULL,
  quantity      DECIMAL(18,6) NOT NULL,         -- 入/出库为正；调整可正可负
  signed_qty    DECIMAL(18,6) NOT NULL,         -- 入库 +q，出库 -q，调整 = q
  occurred_on   DATE         NOT NULL,
  notes         VARCHAR(500) NULL,
  created_by    VARCHAR(100) NOT NULL,
  created_at    DATETIME(3)  NOT NULL,
  UNIQUE KEY uk_source_no (source_no),
  KEY idx_product_wh (product_id, warehouse_id, occurred_on)
);

CREATE TABLE inv_balance (
  product_id    BIGINT NOT NULL,
  warehouse_id  BIGINT NOT NULL,
  location_id   BIGINT NOT NULL DEFAULT 0,       -- 0 表示不区分库位
  batch_no      VARCHAR(50) NOT NULL DEFAULT '',
  serial_no     VARCHAR(80) NOT NULL DEFAULT '',
  expiry_date   DATE NULL,
  quantity      DECIMAL(18,6) NOT NULL,
  last_moved_on DATE NOT NULL,                   -- 呆滞预警要用
  version       BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (product_id, warehouse_id, location_id, batch_no, serial_no)
);
```

> 思考题：`inv_balance` 的主键为什么这样设计？"仓库级余额"是在查询时 `SUM` 库位级余额，还是再建一张表？各自的代价是什么？

**业务规则**

- R2-1 流水类型：入库、出库、调整。入库/出库数量必须 > 0；调整数量不能为 0，可以为负。
- R2-2 `source_no` 全局唯一，**重复提交同一单号直接拒绝**（这就是幂等键）。
- R2-3 商品、仓库必须是启用状态；库位必须属于所选仓库。
- R2-4 **出库前校验可用量**，不足时报错："库存不足，当前可用库存为 8.00"。按粒度校验：指定序列号 → 序列号余额；指定批次 → 批次余额；指定库位 → 库位余额；否则 → 仓库余额。
- R2-5 **序列号**：带序列号的流水数量必须为 1；同一商品的序列号在全系统只能有一处在库（不能重复入库）；出库时序列号必须在所选仓库/库位。
- R2-6 **批次保质期**不能早于流水日期。
- R2-7 **库位容量**：入库（或正向调整）后，某库位某商品的余额不能超过配置的 `max_qty`。报错需带上容量、当前账面和本次数量。
- R2-8 **FIFO 出库**：不指定批次时，按"到期日升序（无到期日排最后）→ 批次号升序"依次扣减，一次请求可能拆成多条流水，子单号为 `{单号}-B01`、`{单号}-B02`……。总量不够时整体失败，一条都不写。
- R2-9 **调拨** = 一条出库 `{单号}-OUT` + 一条入库 `{单号}-IN`，必须在**同一事务**内完成；来源与目标（仓库 + 库位）不能完全相同；两边仓库都必须启用；目标库位要过容量校验。
- R2-10 **盘点**：输入实盘数，差异 = 实盘 - 账面；差异为 0 时拒绝（"无需生成调整流水"）；否则生成一条调整流水，备注写"盘点调整：账面 X，实盘 Y"。
- R2-11 **防超卖**：两个请求同时扣最后的库存，只能成功一个。

**接口**

```
POST /api/inventory/txns                  # 单条入库/出库/调整
GET  /api/inventory/txns?productId=&warehouseId=&kind=&batchNo=&serialNo=&afterId=&size=
GET  /api/inventory/balances?dim=WAREHOUSE|LOCATION|BATCH|SERIAL&productId=&warehouseId=
POST /api/inventory/outbound/fifo         # {productId, warehouseId, locationId, qty, sourceNo}
POST /api/inventory/transfers             # {productId, from{wh,loc}, to{wh,loc}, qty, batchNo?, serialNo?}
POST /api/inventory/stocktakes            # {productId, warehouseId, locationId?, batchNo?, actualQty}
```

**验收用例**

- AC2-1 入库 100 → 出库 30 → 仓库余额 70；流水两条，余额表一行。
- AC2-2 批次 A（到期 10-01）20 件、批次 B（到期 11-01）50 件，FIFO 出库 30 → 生成 `-B01` 扣 A 20、`-B02` 扣 B 10。
- AC2-3 FIFO 出库 80（总共只有 70）→ 失败，流水表**没有任何新记录**。
- AC2-4 序列号 SN001 已在库，再次入库 SN001 → 失败。
- AC2-5 调拨时模拟入库失败（例如目标库位超容量）→ 出库那条也不能留下。
- AC2-6 🔒 余额 10，20 个线程同时各出库 1 → 恰好 10 个成功、10 个失败，最终余额 0，不能为负。
- AC2-7 🔒 线程 1 从 A 调拨到 B，线程 2 同时从 B 调拨到 A → 不能死锁（或死锁后能自动重试成功）。
- AC2-8 同一 `source_no` 并发提交两次 → 只有一条流水。

**练到**

- 防超卖的三种写法都实现一遍并对比：① `SELECT ... FOR UPDATE` 锁余额行；② `UPDATE inv_balance SET quantity = quantity - ? WHERE ... AND quantity >= ?` 看影响行数；③ `version` 乐观锁 + 重试。
- 调拨锁两行时**按主键固定顺序加锁**避免死锁（对应 plan.md Day15），并用 `SHOW ENGINE INNODB STATUS` 看死锁现场。
- 百万级流水表的游标分页、联合索引设计、`EXPLAIN` 分析（对应 Day7~Day12）。
- 进阶（Redis 阶段）：热点商品库存用 Redis Lua 预扣，异步落库，思考与数据库不一致时怎么对账。

---

### M3 采购订单与销售订单

**目标**：练**严格的状态机**和**跨模块联动**（收货 → 入库，发货 → 出库）。

**采购订单 `po_order`**

关键字段：order_no, supplier_id, product_id, order_date, due_date（付款到期日，默认下单日 + 30 天）, quantity, received_qty, rejected_qty, unit_price, amount（= 数量 × 单价，保留 2 位）, status, source_kind（手工/请购/合同/计划…）, source_doc_no, is_locked, version。

> 原项目是单行订单（一张单一个商品）。你可以先照做，做完后再升级成"订单头 + 订单行"，这是一次很好的表结构重构练习。

**状态机**

```
            submit              全部收货且无拒收
  DRAFT ───────────► SUBMITTED ──────────────────► RECEIVED
    │                  │   ▲                          │  ▲
    │ cancel           │   │ 退货（回到待收货）        │  │
    ▼                  │   └──────────────────────────┘  │
 CANCELLED ◄───────────┤                                 │
    ▲   全部拒收且没收过货 │  收货+拒收覆盖全部数量且有拒收   │ close / reopen
    └──────────────────┴──────────────────────► CLOSED ◄─┘
```

**业务规则**

- R3-1 待收数量 = 数量 - 已收 - 已拒收。
- R3-2 **收货**：只有 SUBMITTED 可以收货；本次数量 > 0 且 ≤ 待收数量；收货在同一事务中调用库存入库；待收降为 0 时，没有拒收 → RECEIVED，有拒收 → CLOSED。
- R3-3 **未入库拒收**：只扣减待收数量，**不产生库存流水**；待收降为 0 时，从未收过货 → CANCELLED，收过一部分 → CLOSED。
- R3-4 **退货**：SUBMITTED / RECEIVED / CLOSED 可以退货；数量 ≤ 累计已收；同一事务调库存出库；RECEIVED/CLOSED 退货后回到 SUBMITTED。
- R3-5 **锁定**：只有 DRAFT / SUBMITTED 可以锁定；锁定后不能推进或取消（典型场景：审批中锁单）。
- R3-6 非法状态跳转一律报错，报错信息写清楚"不能从 X 变更为 Y"。
- R3-7 🔒 同一张单两人同时收货，总收货数量不能超过订单数量（用 `version` 乐观锁）。

**销售订单 `so_order`**

- 字段：order_no, customer_id, product_id, contract_id?, project_id?, order_date, due_date（收款到期日）, quantity, unit_price, amount, status。
- 状态：DRAFT → SUBMITTED → SHIPPED；DRAFT/SUBMITTED 可以 CANCELLED。
- R3-8 **发货**：只有 SUBMITTED 可以发货；同一事务调库存出库（可选 FIFO）；库存不足时整单失败，订单状态不变。
- R3-9 客户必须启用。

**接口**

```
CRUD /api/purchase-orders
POST /api/purchase-orders/{id}/submit | cancel | close | lock | unlock
POST /api/purchase-orders/{id}/receipts     {qty, warehouseId, locationId?, batchNo?, expiryDate?}
POST /api/purchase-orders/{id}/rejections   {qty, reason}
POST /api/purchase-orders/{id}/returns      {qty, warehouseId, ...}
CRUD /api/sales-orders
POST /api/sales-orders/{id}/submit | cancel
POST /api/sales-orders/{id}/ship            {warehouseId, locationId?, fifo: true}
```

**验收用例**

- AC3-1 数量 100：收货 60 → SUBMITTED，待收 40；拒收 40 → CLOSED，库存只增加 60。
- AC3-2 数量 100：拒收 100 → CANCELLED，库存不变。
- AC3-3 RECEIVED 状态退货 10 → 回到 SUBMITTED，待收 10，库存减 10。
- AC3-4 锁定的订单调取消 → 409。
- AC3-5 发货时库存不足 → 订单仍是 SUBMITTED，库存流水没有新增。

**练到**：状态机建模（枚举 + 迁移表，或 Spring StateMachine，建议先手写）、跨服务事务传播（`@Transactional` 的 `REQUIRED`）、乐观锁。

---

### M4 应收应付核销

**目标**：练**金额占用**。一张订单可以分多次收/付款，任何时刻都不能超额。

**核心表 `stl_settlement`**

reference_no（唯一）, order_id, party_id（供应商或客户，由订单自动带出）, kind（`PAYABLE` 应付 / `RECEIVABLE` 应收）, amount, occurred_on, notes, status（`ACTIVE` / `PENDING_APPROVAL` / `REJECTED` / `VOIDED`）, void_reason。

**业务规则**

- R4-1 订单余额视图：
  - 已核销 = 该订单所有 ACTIVE 核销之和
  - 审批中 = 所有 PENDING_APPROVAL 核销之和
  - 剩余 = 订单金额 - 已核销
  - **可用 = 剩余 - 审批中**（审批中的金额要先冻结，否则两张待审批的核销加起来会超额）
- R4-2 新建核销：金额 > 0 且 ≤ 可用金额；已取消的订单不能核销。
- R4-3 需要审批的核销以 PENDING_APPROVAL 创建，通过后变 ACTIVE，拒绝后变 REJECTED（记录拒绝原因）。
- R4-4 REJECTED 可以重新提交，重新提交时**再次校验可用金额**（期间可能有别的核销占用了额度）。
- R4-5 撤销：必须填原因；审批中的不能直接撤销；已撤销的不能重复撤销。撤销后金额释放回可用。
- R4-6 🔒 订单金额 1000，两个请求同时各核销 600 → 只能成功一个。

**接口**

```
GET  /api/settlements/order-balances?kind=RECEIVABLE     # 只返回剩余 > 0 的订单
GET  /api/settlements?kind=&status=&partyId=&keyword=
POST /api/settlements                                     {kind, orderId, amount, referenceNo, needApproval}
POST /api/settlements/{id}/void                           {reason}
POST /api/settlements/{id}/resubmit
GET  /api/parties/{id}/statement                          # 往来对账单：订单、核销、余额、逾期
```

**验收用例**

- AC4-1 订单 1000：核销 300（生效）+ 500（审批中）→ 可用 200；再核销 300 → 失败。
- AC4-2 撤销那笔 300 → 可用 500。
- AC4-3 🔒 并发超额核销只成功一个（提示：对订单行 `FOR UPDATE`，或维护一张"订单核销汇总"表用条件更新）。

**练到**：`BigDecimal` 精度与舍入、聚合查询、并发下的"查询-判断-写入"竞态（最经典的面试题形态）。

---

### M5 流程引擎（全项目含金量最高）

**目标**：自研一个轻量流程引擎，不用 Activiti/Flowable。采购单、销售单、核销、请假、付款申请都复用它，而不是每个业务各写一套审批表。

**建议分四步做，每步都能独立验收：**

| 步骤 | 范围 |
| --- | --- |
| 5A | 流程定义：节点/连线、图校验、发布、版本 |
| 5B | 线性审批：开始 → 审批 → 结束，同意/拒绝，业务回调 |
| 5C | 协作动作：转交、退回、撤回、重提、操作时间线 |
| 5D | 高级：条件分支、会签策略、并行拆分/汇聚、受控循环 |

#### 5A 流程定义

**表**

| 表 | 关键字段 |
| --- | --- |
| `wf_definition` | id, code（大写）, name, version_no, status（`DRAFT`/`PUBLISHED`/`ARCHIVED`）, published_at；唯一键 (code, version_no) |
| `wf_node` | id, definition_id, type, name, x, y, config_json |
| `wf_connection` | definition_id, source_node_id, target_node_id, condition_key（可空） |

**节点类型**：`START`、`APPROVAL`（审批）、`CONDITION`（条件）、`NOTIFICATION`（通知）、`BUSINESS_ACTION`（自动业务动作）、`PARALLEL_SPLIT`（并行拆分）、`PARALLEL_JOIN`（并行汇聚）、`LOOP`（受控循环）、`END`。

**审批节点 config_json 示例**

```json
{
  "approvers": ["zhangsan"],
  "approverRoles": ["财务经理"],
  "approverOrgs": ["采购部"],
  "approverBusinessFields": ["projectManager"],
  "approvalMode": "Quorum",
  "requiredApprovals": 2,
  "returnTargets": ["<某个前序审批节点 id>"]
}
```

审批人来源可以组合，`$initiator` 代表发起人本人。

**业务规则（发布时的图校验，全部通过才能发布）**

- R5-1 有且只有一个开始节点，开始节点没有入边、只有一条无条件出边。
- R5-2 至少一个结束节点，结束节点没有出边。
- R5-3 所有节点从开始节点**可达**，且都**能到达**某个结束节点（两次 BFS：正向一次、反向一次）。
- R5-4 **禁止无控制的环**：图中的环必须经过 LOOP 节点的 `repeat` 分支（DFS 判环时，把 `repeat` 边排除掉）。
- R5-5 只有条件节点的出边可以带 `condition_key`；条件节点每条出边都必须有分支键且不能重复，至少两个分支；配置里的分支和连线要一一对应。
- R5-6 并行拆分至少两条指向不同目标的无条件出边，且不能直接连到汇聚节点；并行汇聚至少两条入边、只有一条出边。
- R5-7 审批节点必须至少配置一种审批人来源；`returnTargets` 只能指向其他审批节点，且不能指向自己。
- R5-8 `approvalMode` 只能是 `All` / `Any` / `Majority` / `Quorum`；Quorum 必须带 `requiredApprovals ≥ 1`。
- R5-9 **只有草稿可以修改**。已发布的要改，只能"复制为新版本"（版本号 +1）；已发布可以归档。
- R5-10 校验失败时一次性返回**全部**错误信息，而不是遇到第一个就返回（方便设计器标红）。

#### 5B 流程实例与线性审批

**表**

```sql
CREATE TABLE wf_instance (
  id                   BIGINT PRIMARY KEY,
  definition_id        BIGINT NOT NULL,
  definition_code      VARCHAR(50) NOT NULL,
  definition_version   INT NOT NULL,
  definition_snapshot  JSON NOT NULL,        -- 启动时整张图的快照
  business_type        VARCHAR(100) NOT NULL, -- 如 PURCHASE_ORDER
  business_id          BIGINT NOT NULL,
  started_by           VARCHAR(100) NOT NULL,
  previous_instance_id BIGINT NULL,           -- 重提时指向上一次实例
  status               VARCHAR(50) NOT NULL,  -- RUNNING/COMPLETED/REJECTED/CANCELLED
  active_node_ids      JSON NOT NULL,         -- 当前激活节点集合（并行时多个）
  join_arrivals        JSON NOT NULL,         -- 并行汇聚已到达的分支
  loop_iterations      JSON NOT NULL,         -- 每个 Loop 节点已执行次数
  approver_snapshot    JSON NOT NULL,         -- 每个审批节点首次进入时解析出的审批人
  revision             BIGINT NOT NULL,       -- 乐观锁
  started_at DATETIME(3) NOT NULL, completed_at DATETIME(3) NULL,
  KEY idx_business (business_type, business_id)
);

CREATE TABLE wf_task (
  id              BIGINT PRIMARY KEY,
  instance_id     BIGINT NOT NULL,
  node_id         BIGINT NOT NULL,
  node_name       VARCHAR(200) NOT NULL,
  business_type   VARCHAR(100) NOT NULL,
  business_id     BIGINT NOT NULL,
  assignee        VARCHAR(100) NOT NULL,     -- 统一存小写
  round           INT NOT NULL,              -- 退回后再次进入同一节点时 +1
  status          VARCHAR(50) NOT NULL,      -- PENDING/APPROVED/REJECTED/CANCELLED/TRANSFERRED/RETURNED
  transfer_target VARCHAR(100) NULL,
  decision_actor  VARCHAR(100) NULL,
  decision_comment VARCHAR(2000) NULL,
  revision        BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL, completed_at DATETIME(3) NULL,
  UNIQUE KEY uk_task (instance_id, node_id, round, assignee),   -- 待办幂等的关键
  KEY idx_my_todo (assignee, status, created_at)
);
```

**业务规则**

- R5-11 只有已发布且校验通过的定义可以启动实例。
- R5-12 **启动时保存定义快照**，实例后续只按快照运行。定义后来发布了新版本，也不影响正在跑的实例。
- R5-13 进入审批节点时解析审批人并**写入 `approver_snapshot`**，之后组织架构怎么变，这个节点的审批人都不变；解析结果为空时启动/推进失败（"审批节点未解析到可用审批人，流程不能进入无人待办状态"）。
- R5-14 **待办幂等**：同一实例 + 节点 + 轮次 + 审批人只能有一条待办。用唯一键 + `INSERT IGNORE`（或 `ON DUPLICATE KEY`）实现；重复创建时返回已有那条，不能报错也不能产生第二条通知。原项目是用这四个字段做哈希生成确定性 ID，思路一样。
- R5-15 **只有待办的审批人本人可以处理**，操作人取**当前登录用户**，绝不能从请求参数里拿。查询参数里的 `assignee` 只能用来筛选列表（这是典型的越权漏洞点）。
- R5-16 已处理的待办不能重复处理；审批意见最长 2000 字。
- R5-17 **乐观锁**：处理待办时 `UPDATE wf_task SET ..., revision = revision + 1 WHERE id = ? AND revision = ? AND status = 'PENDING'`，影响行数为 0 就返回"审批待办状态已变化，请刷新后重试"。推进实例同理用 `wf_instance.revision`。
- R5-18 同意：推进到下一个节点，自动节点（通知、业务动作、条件）连续执行，直到遇到下一个审批节点或结束节点。
- R5-19 拒绝：实例变 REJECTED，同实例其他待办全部 CANCELLED。
- R5-20 **业务回调**：流程引擎不改业务表。业务模块实现接口并按 `businessType` 注册：

  ```java
  public interface WorkflowBusinessHandler {
      String businessType();                          // 如 "PURCHASE_ORDER"
      Map<String, Object> fields(long businessId);    // 提供条件分支和审批人解析所需字段
      void onApproved(long businessId, String actor); // 审批完成：例如采购单 SUBMITTED 并解锁
      void onRejected(long businessId, String actor); // 驳回：例如释放预算占用
      void onWithdrawn(long businessId, String actor);
  }
  ```

  回调和流程推进在**同一个数据库事务**里，回调抛异常整个审批动作回滚。
- R5-21 **业务状态门禁仍由业务模块负责**：审批通过不等于可以绕过业务规则，比如核销审批通过时如果可用金额已不足，回调要拒绝并回滚。

#### 5C 协作动作与时间线

- R5-22 **转交**：当前待办变 TRANSFERRED 并记录目标；给目标人新建待办；目标不能是自己。
- R5-23 **退回**：只能退回到当前节点 `returnTargets` 里声明的审批节点；同节点其他待办取消；目标节点的轮次 +1，重新生成待办（历史轮次的待办保留，不覆盖）。
- R5-24 **撤回**：只有发起人可以撤回，只有运行中的实例可以撤回；实例变 CANCELLED，所有待办取消，触发业务 `onWithdrawn`。
- R5-25 **重提**：被驳回/撤回的业务单可以重新发起，新实例的 `previous_instance_id` 指向旧实例，形成链路。
- R5-26 **操作时间线 `wf_operation`**：不可变记录，类型包括启动、分派、同意、拒绝、取消、退回、转交、撤回、重提、节点进入/完成/执行/失败、重试。每条带 `dedupe_key`（唯一），重复写入忽略。
- R5-27 自动节点（业务动作）执行失败时，实例停在该节点并记录 NodeFailed，管理员可以"重试"。

#### 5D 高级：条件、会签、并行、循环

- R5-28 **条件节点**：配置 `branches: [{key, expression}]` + 可选 `defaultKey`，按顺序取第一个命中的分支，都不命中走默认分支，没有默认分支就报错。表达式支持：`==`、`!=`、`>`、`>=`、`<`、`<=`、`contains`、`startswith`、`endswith`，用 `&&` 和 `||` 组合（`&&` 优先）。字段值来自业务回调的 `fields()`。例如 `amount > 10000 && department == "采购部"`。
  - 要求**自己写一个小的表达式解析器**（先按 `||` 再按 `&&` 切分，再用正则解析比较式），不要直接用 SpEL，否则练不到东西；写完再对比 SpEL 的安全风险。
- R5-29 **会签策略**（同一审批节点有多个审批人时）：
  - `All`：所有人同意才通过（默认）。
  - `Any`：任意一人同意即通过。
  - `Majority`：同意人数 > 审批人数 / 2。
  - `Quorum`：同意人数 ≥ `requiredApprovals`，且 `requiredApprovals` 不能超过审批人数。
  - 达到门槛后，同节点剩余的待办自动 CANCELLED（意见写"同节点审批已达到通过门槛"）。
  - 任何一人拒绝 → 整个实例拒绝。
- R5-30 **并行拆分**：一次激活所有无条件出边的目标，`active_node_ids` 变成多个。
- R5-31 **并行汇聚**：每个分支到达时记到 `join_arrivals`；所有入边来源都到齐后才激活汇聚节点继续往下走。并行分支没有汇聚前不能直接到结束节点。
- R5-32 **受控循环**：Loop 节点每执行一次计数 +1，未达 `maxIterations` 走 `repeat`，达到后走 `exit`，计数持久化，重启不丢。

**接口**

```
# 定义
POST /api/wf/definitions                 # 新建草稿
PUT  /api/wf/definitions/{id}/graph      # 保存节点和连线
POST /api/wf/definitions/{id}/validate   # 返回全部错误
POST /api/wf/definitions/{id}/publish | archive | new-version
# 实例
POST /api/wf/instances                   {definitionCode, businessType, businessId}
GET  /api/wf/instances/{id}              # 状态 + 当前节点 + 时间线
GET  /api/wf/instances?businessType=&businessId=   # 业务单查看审批记录
POST /api/wf/instances/{id}/withdraw | retry
# 待办
GET  /api/wf/tasks/mine?status=PENDING&page=
GET  /api/wf/tasks/mine/count            # 首页角标
POST /api/wf/tasks/{id}/approve | reject {comment}
POST /api/wf/tasks/{id}/transfer         {target, comment}
POST /api/wf/tasks/{id}/return           {targetNodeId, comment}
```

**验收用例**

- AC5-1 定义里有一个从开始节点不可达的审批节点 → 发布失败，错误列表里包含该节点名。
- AC5-2 定义 V1 启动实例后发布 V2（多一个审批节点），V1 的实例仍按两节点走完。
- AC5-3 张三的待办，李四调同意接口 → 403；在请求参数里伪造 `assignee=zhangsan` 也没用。
- AC5-4 采购单审批通过 → 回调把采购单推进到 SUBMITTED；回调抛异常 → 待办仍是 PENDING，实例不前进。
- AC5-5 金额 > 10000 走"总经理审批"分支，否则走"部门经理审批"。
- AC5-6 3 人 Majority：第 2 人同意后节点通过，第 3 人待办变 CANCELLED。
- AC5-7 退回到第一个审批节点 → 第一个节点出现 round = 2 的新待办，round = 1 的历史待办还在。
- AC5-8 并行拆分为"财务会签"和"法务会签"，只有财务同意时实例停在汇聚前；法务也同意后才进入下一步。
- AC5-9 🔒 Any 模式 2 个审批人**同时**点同意 → 实例只推进一次，下一节点的待办只生成一份，通知只发一次。
- AC5-10 🔒 同一个人双击同意（两个并发请求）→ 一个成功，一个返回 409。
- AC5-11 🔒 审批人点同意的同时发起人点撤回 → 只有一个成功，最终状态自洽（不能出现实例已撤回但下一节点待办已生成）。

**练到**

- 图算法：BFS 可达性、反向 BFS、DFS 判环。
- 状态快照 + JSON 列（MySQL `JSON` 类型）。
- 乐观锁 vs 悲观锁：待办用 revision 做 CAS，实例推进时用 `SELECT ... FOR UPDATE` 锁实例行，思考两者怎么配合。
- 幂等：唯一键 + `INSERT IGNORE`、去重键。
- 策略模式（按 `businessType` 分发回调）、解释器模式（条件表达式）。
- 进阶（Redis 阶段）：审批接口加分布式锁 `lock:wf:instance:{id}`，对比数据库锁的优缺点；防重复提交令牌。

---

### M6 额度占用：请假额度 + 付款预算

**目标**：掌握企业系统里最常见的一种模式：**提交时占用、通过时核销、驳回/撤回时释放**。请假额度和付款预算是同一个模型，做完一个另一个基本可以照抄。

**通用模型**

```
可用 = 总额度 - 占用中 - 已使用

提交申请   ──► reserve(x)  ：可用 ≥ x，否则拒绝；占用中 += x
审批通过   ──► consume(x)  ：占用中 -= x；已使用 += x
驳回/撤回  ──► release(x)  ：占用中 -= x
重新提交   ──► 占用记录从 RELEASED 重新变回 RESERVED（金额可能已修改）
```

每次占用都有一条**占用记录**（`RESERVED` / `CONSUMED` / `RELEASED`），挂在具体申请单上，保证能追溯"这笔占用是谁的"，也保证释放和核销是幂等的。

**请假额度**

| 表 | 关键字段 |
| --- | --- |
| `oa_leave_balance` | user_id, year, leave_type（只有 `ANNUAL` 年假、`COMPENSATORY` 调休有额度）, entitled_hours, reserved_hours, used_hours, version；唯一键 (user_id, year, leave_type) |
| `oa_leave_reservation` | balance_id, request_id（唯一）, hours, status, released_at |
| `oa_leave_request` | user_id, leave_type, start_at, end_at, hours, reason, status |

- R6-1 请假时长按**工作日历**计算（跳过周末和 `sys_holiday` 中的节假日，调休上班日要算工作日）。
- R6-2 提交时占用额度，额度不足拒绝提交："请假额度不足，不能提交申请"。
- R6-3 管理员调整总额度时，新额度不能低于"占用中 + 已使用"。
- R6-4 加班审批通过后可以折算为调休额度（`grant`）。
- R6-5 已使用的额度不能再释放。

**付款预算**

| 表 | 关键字段 |
| --- | --- |
| `oa_payment_budget` | budget_no, legal_entity, department, currency, total_amount, reserved_amount, consumed_amount, status（`ACTIVE`/`CLOSED`） |
| `oa_payment_budget_reservation` | budget_id, payment_request_id（唯一）, amount, status |
| `oa_payment_request` | 申请人, 部门, 收款方, 金额, 预算, 状态 |

- R6-6 预算关闭后不能再占用；**仍有占用中金额时不能关闭**。
- R6-7 付款申请走审批：提交占用 → 审批通过核销 → 驳回/撤回释放。
- R6-8 🔒 预算剩 1000，两个 600 的付款申请同时提交 → 只能成功一个。

**选做扩展：借款与报销冲销**
借款单审批通过后，员工后续报销可以冲销借款余额，也可以直接还款；借款状态 `APPROVED → PARTIALLY_SETTLED → SETTLED`，冲销金额不能超过借款余额。

**接口**

```
GET  /api/leave/balances/mine?year=         POST /api/leave/balances/{id}/adjust
CRUD /api/leave/requests                    POST /api/leave/requests/{id}/submit | withdraw
CRUD /api/payment-budgets                   POST /api/payment-budgets/{id}/close
CRUD /api/payment-requests                  POST /api/payment-requests/{id}/submit | withdraw
```

**验收用例**

- AC6-1 年假 40 小时：提交 16 小时 → 可用 24；被驳回 → 可用 40；重新提交 → 可用 24；通过 → 已使用 16。
- AC6-2 请假跨国庆节假日，时长只算工作日。
- AC6-3 审批回调失败（例如模拟核销时报错）→ 审批动作整体回滚，额度仍在"占用中"。

**练到**：余额 + 明细的双表设计、幂等状态迁移、和流程引擎回调的事务边界、工作日计算。

---

### M7 通知中心 + Outbox 可靠投递

**目标**：站内通知**不重复**；站外通知（邮件/企业微信，先用日志模拟）**不丢失、失败可重试**；**通知失败不能影响主业务**。

**站内通知 `ntf_notification`**

recipient（小写）, kind（`APPROVAL`/`REMINDER`/`ASSIGNMENT`/`SYSTEM`）, title, content, href（点击跳转的前端路由）, dedupe_key, created_at, read_at；唯一键 (recipient, dedupe_key)。

- R7-1 按"接收人 + 去重键"幂等，接收人大小写无关。例如待办通知的去重键为 `workflow-task:{taskId}`。
- R7-2 **待办创建时发通知；待办处理后（包括被取消）把对应通知标为已读**。
- R7-3 **通知在事务提交之后才发**：用 Spring 的 `@TransactionalEventListener(phase = AFTER_COMMIT)`。否则主事务回滚了，用户却已经收到"待审批"通知，点进去什么都没有。
- R7-4 标已读幂等（已读的再标一次不改时间）。

**Outbox 外部通知 `ntf_outbox`**

message_id, channel（`EMAIL`/`WECOM`…）, address, payload, dedupe_key（唯一）, status（`PENDING`/`DELIVERED`）, retry_count, last_error, last_attempt_at, next_attempt_at, lease_until。

- R7-5 **入队和发送分离**：业务事务里只往 outbox 表插一行（与业务数据同一事务），由后台任务负责真正发送。
- R7-6 **抢占租约**：投递任务先执行 `UPDATE ntf_outbox SET lease_until = now()+5min WHERE id=? AND (lease_until IS NULL OR lease_until < now())`，影响行数为 1 才发送，防止多个实例重复发送。
- R7-7 失败后按退避时间重试：5 分钟、15 分钟、30 分钟、1、2、4、8、12 小时；错误信息截断到 2000 字；单条失败不影响同批其他消息。
- R7-8 同一接收人同一渠道的地址去重（邮箱大小写无关）。
- R7-9 提供运维视图：待投递数、延迟中数、有失败记录数、最大重试次数，按渠道分组。

**MQ 阶段改造**（对应 plan.md 阶段三）
把"后台轮询 outbox"升级为：outbox 行 → 投递任务发到 MQ → 消费者调用渠道发送。要求：

- 生产者确认 + 消费者手动 ack；
- 消费端按 `dedupe_key` 幂等；
- 超过最大重试次数进入**死信队列**，运维页面可以手动重放；
- 用**延时队列**实现"待办超过 24 小时未处理自动催办"。

**接口**

```
GET  /api/notifications/mine?unreadOnly=&page=
GET  /api/notifications/mine/unread-count
POST /api/notifications/{id}/read       POST /api/notifications/read-all
GET  /api/admin/outbox/summary          POST /api/admin/outbox/deliver     # 手动触发一次投递
```

**验收用例**

- AC7-1 同一待办重复触发创建 → 审批人只收到一条通知。
- AC7-2 审批通过后，该审批人的对应通知自动变为已读。
- AC7-3 主事务回滚 → 没有站内通知，outbox 也没有记录。
- AC7-4 渠道模拟连续失败 3 次 → `retry_count = 3`，`next_attempt_at` 符合退避表；第 4 次成功 → DELIVERED。
- AC7-5 🔒 起两个投递进程同时跑 → 每条消息只发送一次。

**练到**：事务同步回调、Outbox 模式（分布式事务的最实用方案）、租约锁、指数退避、MQ 可靠性全套。

---

### M8 定时任务 + 库存预警 + 对账

**目标**：后台任务可配置、可查看、多实例部署时不重复执行。

**任务清单**

| 任务 | Cron 示例 | 说明 |
| --- | --- | --- |
| Outbox 投递 | 每分钟 | M7 |
| 临期预警 | 每天 08:00 | 批次到期日在 N 天内（N 取 0~365，默认 30）的在库批次；已过期的排前面 |
| 呆滞预警 | 每周一 08:00 | 在库且最后一次出入库距今 ≥ N 天（1~3650，默认 180）的批次 |
| 超储预警 | 每天 08:00 | 商品全仓余额 > `max_inventory_qty` 的，按超出量倒序 |
| 库存对账 | 每天 02:00 | 校验 `SUM(inv_txn.signed_qty)` 与 `inv_balance` 一致，不一致发系统通知 |
| 待办催办 | 每小时 | 超时未处理的待办（MQ 阶段改为延时队列） |

**业务规则**

- R8-1 每个任务可以启用/停用，可以配置"跳过节假日"（读 `sys_holiday`）。
- R8-2 管理页面展示任务名、Cron、下次执行时间、上次执行结果和耗时。
- R8-3 支持手动触发一次。
- R8-4 🔒 部署两个实例时，同一任务同一时刻只执行一次（Redis 分布式锁或 ShedLock）。
- R8-5 预警结果推送给配置的接收人（走 M7 通知，去重键带日期，保证一天只提醒一次）。

**接口**

```
GET   /api/admin/jobs              PATCH /api/admin/jobs/{name}      # 启停、改 cron
POST  /api/admin/jobs/{name}/trigger
GET   /api/inventory/alerts/expiry?withinDays=30
GET   /api/inventory/alerts/stagnant?inactiveDays=180
GET   /api/inventory/alerts/overstock
CRUD  /api/holidays
```

**练到**：`@Scheduled` / Quartz / XXL-JOB 对比，分布式锁（`SET NX PX` + Lua 释放 + 看门狗续期，对应 plan.md 阶段二的学习目标）。

---

### M9 客户 360 视图 + 跨模块全文搜索

**目标**：做读多写少的聚合查询，放到 ES 阶段完成。

**客户 360**（原项目的"从 CRM 客户出发"）

从一个客户出发，一页看到：销售订单（状态、金额）、发货记录（库存出库流水）、应收余额（已收、审批中、剩余、逾期）、相关审批（进行中的流程）。

- R9-1 各模块数据**只能通过对方的 Application 服务取**，不能在聚合服务里直接 join 其他模块的表（先这样做，感受一下性能问题，再引出下面的读模型）。
- R9-2 客户停用或删除前，先展示影响：订单数、核销数、审批中的流程数（复用 M1 的影响查询）。

**全文搜索**

- R9-3 一个搜索框，搜客户、销售单、采购单、库存流水单号、核销流水号，返回统一结构：`{objectType, id, code, title, status, summary, href}`，并按对象类型分面计数。
- R9-4 第一版用 MySQL `LIKE` 实现（感受慢在哪里，用 `EXPLAIN` 看）。
- R9-5 第二版改为 ES：业务表变更 → Canal 监听 binlog（或业务侧发 MQ 事件）→ 消费者写 ES；中文用 IK 分词；支持高亮、`search_after` 深分页。
- R9-6 数据权限：搜索结果只返回当前用户有菜单权限的对象类型。

**接口**

```
GET /api/customers/{id}/overview
GET /api/search?q=&types=CUSTOMER,SALES_ORDER&size=20&searchAfter=
```

**练到**：聚合查询、读模型（CQRS 入门）、binlog 同步、ES Mapping 与 DSL。

---

### X1（选做）MRP 物料需求计算

**目标**：算法挑战。给定需求（销售订单、生产工单），按 BOM 多层展开，算出每种物料缺多少，给出"采购"或"生产"建议。

- BOM 表：`parent_product_id, child_product_id, qty_per`（每生产 1 个父件需要多少子件），可以多层。
- 计算：
  1. 汇总每个成品的毛需求；
  2. 逐层展开：子件需求 = 父件净需求 × `qty_per`（**先按层级排序**，同一物料出现在多层时要在最低层汇总后再展开，否则会重复计算）；
  3. 供给 = 在库 + 采购在途（已提交未收完的采购单待收数量）+ 未完工生产工单；
  4. 缺口 = max(0, 需求 - 供给)；有 BOM 的建议"生产"，没有的建议"采购"。
- 每次计算生成一个**计划批次**（状态 `SIMULATED` → `CONFIRMED`，或 `CANCELLED`），明细里冻结当时的供需数字和需求来源（哪张销售单贡献了多少）。
- 重算不覆盖旧批次，旧批次标为 `SUPERSEDED`，新批次记录"由哪个批次重算而来"。
- BOM 必须检测循环引用（A 用 B、B 又用 A）。
- 确认后的采购建议可以一键生成采购单草稿（来源类型 = 计划，来源单号 = 计划号）。

**练到**：拓扑排序（低层码计算）、递归 SQL、快照设计、大批量计算的分批与事务拆分。

### X2（选做）多租户

原项目支持按域名识别租户、每个租户可以用不同数据库。你可以做简化版：所有表加 `tenant_id`，用 MyBatis-Plus 租户插件自动拼条件，登录时从域名或请求头识别租户。重点练"怎么保证任何 SQL 都不会漏掉 tenant_id 条件"。

---

## 5. 端到端演示场景（全部做完后用来整体验收）

**场景 A：采购到入库再到付款**

1. 采购员新建采购单（数量 100，单价 50）→ 提交 → 触发审批，采购单锁定。
2. 金额 5000 ≤ 10000，条件节点走"部门经理审批"；部门经理收到站内通知 → 同意 → 回调把采购单推进为 SUBMITTED 并解锁；通知自动变已读。
3. 仓管收货 60（批次 A，到期 10-15）→ 库存 +60；拒收 40 → 采购单 CLOSED。
4. 财务发起应付核销 3000（需审批）→ 可用金额从 3000 变为 0（有 3000 在审批中）。
5. 财务经理驳回 → 核销变 REJECTED，可用恢复 3000 → 财务修改后重新提交 → 通过 → 生效。

**场景 B：销售到发货再到收款**

1. 销售单（客户 C，数量 30）提交 → 发货，FIFO 自动扣批次 A 的 30 件。
2. 分两次收款核销，第二次超额被拒。
3. 打开客户 C 的 360 视图，能看到订单、发货流水、应收余额。

**场景 C：请假会签与退回**

1. 员工请年假 16 小时 → 额度占用。
2. 节点一"部门经理"通过；节点二"HR 会签"（3 人，Majority）→ 第一个 HR 退回到节点一 → 节点一出现 round = 2 的待办。
3. 部门经理再次同意 → HR 两人同意 → 第三人待办自动取消 → 额度转为已使用。

**场景 D：并发压测**（Redis/MQ 阶段后）

用 JMeter/wrk 对"出库"和"审批同意"接口压测，记录 TPS、P99，确认没有超卖、没有重复待办、没有重复通知，并对比加 Redis 锁前后的差异。

---

## 6. 非功能需求

- **测试**：每个 R-规则至少一个单元测试；每个 🔒 用例都有 Testcontainers 并发集成测试。建议覆盖率 ≥ 70%（领域层 ≥ 90%）。
- **性能基线**：库存流水 100 万条、待办 10 万条的数据量下，"我的待办"和"流水游标分页"查询 < 100ms（用 `EXPLAIN` 证明走了索引）。
- **可观测性**：请求日志带 traceId；慢 SQL（> 200ms）打日志；关键业务动作记审计日志。
- **部署**：`docker compose up` 一键启动 MySQL、Redis、MQ、ES、后端、Nginx（托管前端静态资源并反向代理 `/api`，配置限流）。后端 Dockerfile 用多阶段构建。
- **数据库迁移**：所有表结构变更都用 Flyway 脚本，禁止手动改库。

---

## 7. 前端页面清单（够用即可）

| 页面 | 要点 |
| --- | --- |
| 登录 / 布局 | 按 `/api/me` 渲染菜单树；按钮按权限码显示 |
| 用户、角色、菜单 | 角色授权用树形勾选 |
| 商品、仓库（含库位、容量）、客户、供应商 | 列表 + 表单 + 启停；删除前弹出影响清单 |
| 库存流水 / 余额 / 预警 | 余额支持切换维度；流水无限滚动（游标分页） |
| 采购单 / 销售单 | 详情页按状态显示可用操作；详情页嵌入"审批记录"时间线 |
| 核销 | 订单余额列表（剩余、审批中、可用）；往来对账单 |
| 流程设计器 | React Flow 拖拽节点、连线，右侧编辑节点配置 JSON；点"校验"把错误节点标红 |
| 我的待办 / 我发起的 | 同意、拒绝、转交、退回；点击跳转回业务原单 |
| 通知中心 | 顶部铃铛显示未读数；列表点击跳 `href` |
| 请假 / 付款申请 / 预算 | 显示总额度、占用中、已使用、可用 |
| 运维：定时任务、Outbox、审计日志 | |
| 全局搜索 | 顶部搜索框 + 分面结果页 |

---

## 8. 附录：原项目对照（写不下去时去看原实现）

路径均相对 `prroject/VelrixWorkHub/src/`。

| 主题 | 原项目文件 |
| --- | --- |
| 流程定义与图校验 | `VelrixWorkHub.Domain/WorkflowDefinition.cs`、`WorkflowDefinitionValidator.cs` |
| 流程实例（快照、并行、循环） | `VelrixWorkHub.Domain/WorkflowInstance.cs` |
| 待办（幂等 ID、状态） | `VelrixWorkHub.Domain/WorkflowTask.cs` |
| 审批动作、会签策略、乐观锁 | `VelrixWorkHub.Application/Workflow/WorkflowTaskService.cs` |
| 条件表达式 | `VelrixWorkHub.Domain/WorkflowConditionEvaluator.cs` |
| 审批人解析 | `VelrixWorkHub.Application/Workflow/IWorkflowApproverResolver.cs` |
| 操作时间线 | `VelrixWorkHub.Domain/WorkflowOperation.cs` |
| 待办 CAS 与幂等写入 | `VelrixWorkHub.Infrastructure/**/FreeSqlWorkflowTaskRepository.cs` |
| 库存流水与余额、FIFO、调拨、盘点、预警 | `VelrixWorkHub.Domain/InventoryTransaction.cs`、`VelrixWorkHub.Application/Inventory/InventoryService.cs` |
| 仓库、库位容量 | `VelrixWorkHub.Domain/Warehouse.cs` |
| 采购单状态机 | `VelrixWorkHub.Domain/PurchaseOrder.cs` |
| 销售单 | `VelrixWorkHub.Domain/SalesOrder.cs` |
| 核销 | `VelrixWorkHub.Domain/ErpSettlement.cs`、`VelrixWorkHub.Application/Settlements/SettlementService.cs` |
| 请假额度 | `VelrixWorkHub.Domain/OaLeaveBalance.cs` |
| 付款预算 | `VelrixWorkHub.Domain/OaPaymentBudget.cs` |
| 借款冲销 | `VelrixWorkHub.Domain/OaCashAdvance.cs` |
| 站内通知 | `VelrixWorkHub.Domain/WorkNotification.cs` |
| Outbox 外部通知 | `VelrixWorkHub.Application/Notifications/ExternalNotificationOutboxService.cs` |
| 主数据引用影响 | `VelrixWorkHub.Application/MasterData/MasterDataImpactService.cs` |
| 跨模块搜索 | `VelrixWorkHub.Application/Reports/CrossModuleSearchService.cs` |
| 权限与菜单树 | `VelrixWorkHub.Infrastructure/**/AdminAuthorizationService.cs` |
| 定时任务 | `VelrixWorkHub.Infrastructure/**/CronSchedulerService.cs` |
| MRP | `VelrixWorkHub.Domain/MomMaterialPlanning.cs`、`VelrixWorkHub.Application/Mom/MomMaterialPlanningService.cs` |

> 提醒：原项目采用 GPL-3.0 / 商业双许可。自己看着需求手写用于学习没有问题；如果以后要把代码公开发布或商用，不要直接复制原项目的源码。
