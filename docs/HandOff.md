# HandOff — AAA / Velrix M0 平台底座

> **M0 v0.2** 认证 + Redis 与 **R0-6 用户权限缓存/失效** 已完成。组织 **M0-F-60/61** 仍有效。  
> 更新：**2026-10-17**。需求：`docs/M0-平台底座需求.md` §6、`docs/Velrix-SpringBoot重写需求文档.md` R0。

协作习惯：**learning-mentor** + **rubber-duck** 引导实现；Flyway / 针对性测试常由代理补跑。技能见文末。

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot **4.1.1**、Java **17**、Maven、MySQL 8、Flyway、MyBatis-Plus、Spring Security、JJWT、**Redis**
- **配置**：`application.properties` + 可选 `application-local.properties`；JWT / 登录锁定见 `VelrixJwtProperties`、`VelrixAuthProperties`

---

## 当前进度

| 能力 | 落点 |
|------|------|
| 登录双 token / Refresh 轮换 / 登出黑名单 / 登录锁定 | 见 v0.2 Auth（`AuthService`、`JwtService`、Redis auth 前缀） |
| **用户权限缓存（R0-6）** | `MenuAccessService#loadAccess` → `UserAccessCacheStore`；`hasPerm` 走同一 `loadAccess` |
| **缓存失效** | `UserAccessCacheInvalidator` + `application.access.event.*`；**`TransactionPhase.AFTER_COMMIT`** |
| RBAC、菜单、用户/角色/组织等 | 与 v0.1 相同 |

### R0-6 失效触发（提交成功后才 evict）

| 场景 | 入口 | 机制 |
|------|------|------|
| 用户换角色 | `UserService#replaceRoles` | `UserAccessEvictEvent` |
| 角色换菜单 | `RoleMenuService#replace` | `RoleMenuReplacedEvent` → `invalidateByRoleId` |
| 改菜单 | `MenuService#update` | `MenuChangedEvent(id, Optional.empty())` → `invalidateByMenuId` |
| 删菜单 | `MenuService#delete` | 删 **前** `resolveUserIdsByMenuId` 快照 → `MenuChangedEvent(id, Optional.of(userIds))` → `evictAll`（勿在删光 `sys_role_menu` 后再按 menuId 反查） |
| 管理员 | `invalidateByMenuId` / `resolveUserIdsByMenuId` | `SysRoleMapper#selectUserIdsWithAdministratorRole` 与 role-menu 用户 **合并去重** |

核心类：`UserAccessCacheInvalidator`、`UserAccessCacheEventListener`；Mapper 补充 `SysMenuMapper#selectRoleIdsByMenuid`、`SysRoleMapper#selectUserIdsByRoleIds`、`selectUserIdsWithAdministratorRole`。

### Auth API（逻辑）

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/api/auth/login` | 匿名 | `LoginRequest` |
| POST | `/api/auth/refresh` | 匿名 | 旧 refresh revoke 后发新一对 |
| POST | `/api/auth/logout` | access | access 黑名单 + refresh revoke |
| GET | `/api/me` | access | `MenuAccessService#loadAccess`（可命中 Redis） |

### Redis key（勿随意改前缀）

| 前缀 | 用途 |
|------|------|
| `velrix:auth:blacklist:{jti}` | 登出 access |
| `velrix:auth:login-fail:{usernameNorm}` | 登录失败锁定 |
| `velrix:auth:refresh-token:{jti}` | refresh 会话 |
| **`velrix:access:user:{userId}`** | **`UserAccessResponse` JSON（菜单树 + permCodes）** |

### 组织 / 用户（v0.1，仍有效）

- 组织：`/api/orgs`；Flyway 当前 **V16**；**新迁移从 V17 起**
- 种子：V7（admin/lisi、采购菜单）、V8（wangwu 仅子菜单 2101）

### 已验证 / 测试

- **R0-6**：`UserAccessCacheInvalidatorTest`、`UserAccessCacheEventListenerTest`（Mockito）；`UserAccessCacheIntegrationTest`（MySQL Flyway 种子 + Redis，无 Redis 时跳过 2 条）
- 测试配置：`src/test/resources/application.properties`（不 import local，避免空 `test` profile）；`application-test.properties` 兜底
- 命令：`.\mvnw.cmd test "-Dtest=UserAccessCacheInvalidatorTest,UserAccessCacheEventListenerTest,UserAccessCacheIntegrationTest"`
- 全量 `.\mvnw.cmd test` **先问用户**；集成测需本机 **MySQL + Redis 6379**（测试默认密码见 test `application.properties`，勿提交真实密码）

---

## 下一步（建议顺序）

1. **Postman 验收**：改角色菜单 / 换用户角色 / 改删菜单 → `/api/me` 与 `@RequirePerm` 与 DB 一致  
2. **M1 主数据**（见 `docs/M0-平台底座需求.md` / 主需求文档）  
3. 可选：`AuthControllerTest`（login → refresh → logout 链）

---

## 实现约定（摘录）

- 组织无 `perm_code`；鉴权靠菜单按钮 `@RequirePerm`  
- 用户 API 勿返回 `passwordHash`  
- 权限缓存失效宜 **afterCommit**，避免 Redis 已删、DB 回滚  
- 数据库写操作需用户确认（项目规则）

---

## 运行

- 启动：`com.velrix.VelrixApplication`；**Redis 6379**
- 针对性测试：见上 R0-6 命令

---

## 参考

- `docs/M0-平台底座需求.md` — §6 v0.2 / §7  
- `docs/Velrix-SpringBoot重写需求文档.md` — R0-1～R0-2、R0-6

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `learning-mentor` | M1 或新模块分步实现 |
| `rubber-duck` | 先想清楚再写（可与 learning-mentor 合并使用） |
| `tdd` | 补 Controller 链或 M1 用例 |
| `handoff` | 下一阶段再覆盖本文 |
| `documentation-and-adrs` | 缓存/失效策略要留 ADR 时 |
