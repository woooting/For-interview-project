# HandOff — AAA / Velrix M0 平台底座

> **M0 v0.2 认证 + Redis** 主线已完成（登出黑名单、登录锁定、Access/Refresh 轮换）。组织 **M0-F-60/61** 仍有效。  
> 更新：**2026-10-12**。需求：`docs/M0-平台底座需求.md` §6、`docs/Velrix-SpringBoot重写需求文档.md` R0。

协作习惯：**learning-mentor** 引导实现；Flyway / 针对性测试常由代理补跑。技能见文末。

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot **4.1.1**、Java **17**、Maven、MySQL 8、Flyway、MyBatis-Plus、Spring Security、JJWT、**Redis**（`spring-boot-starter-data-redis`）
- **配置**：`application.properties` + 可选 `application-local.properties`；JWT / 登录锁定见 `VelrixJwtProperties`、`VelrixAuthProperties`（`@EnableConfigurationProperties` 在 `VelrixApplication`）

---

## 当前进度

| 能力 | 落点 |
|------|------|
| 登录双 token | `AuthService#issueTokenPair` → `LoginTokens`；`LoginResponse(accessToken, refreshToken)` |
| Refresh（**方案 B 轮换**） | `POST /api/auth/refresh` + `RefreshRequest`；旧 refresh jti revoke 后发新一对 |
| 登出 | `POST /api/auth/logout`：Header Bearer **access** + body `LogoutRequest(refreshToken)`；access → `TokenBlacklistStore`，refresh → `RefreshTokenStore#revoke(jti)` |
| JWT | `JwtService`：`issueAccess` / `issueRefresh`；claim `typ`（access/refresh）、标准 `jti`；Filter 仅 **access** 建登录态，并查黑名单 |
| 登录失败锁定 | `LoginAttemptStore` + `velrix.auth.max-login-failures` / `login-lock-minutes` |
| RBAC、菜单、用户/角色/组织等 | 与 v0.1 相同，见既有 Controller / Service |
| **未做（v0.2 剩余）** | **R0-6** 用户权限 Redis 缓存 + 角色授权变更失效 |

### Auth API（逻辑）

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/api/auth/login` | 匿名 | body：`LoginRequest` |
| POST | `/api/auth/refresh` | 匿名 | body：`{ "refreshToken" }`；失败 → `UNAUTHORIZED`，前端重新 login |
| POST | `/api/auth/logout` | 需 access | Header + `{ "refreshToken" }` |
| GET | `/api/me` | access | 不变 |

`SecurityConfig`：`/api/auth/login`、`/api/auth/refresh` 为 `permitAll`。

### Redis key（勿改前缀除非迁移）

| 前缀 | 用途 |
|------|------|
| `velrix:auth:blacklist:{jti}` | 登出后的 access jti，TTL = token 剩余秒数 |
| `velrix:auth:login-fail:{usernameNorm}` | 失败次数；首次失败设 lock 窗口 TTL |
| `velrix:auth:refresh-token:{jti}` | refresh 会话；value = userId；login/refresh 登记，logout/revoke/轮换删除 |

**不**在 Redis 存整段 JWT；access 平时无状态，仅黑名单与 refresh jti 登记。

### 组织 / 用户（v0.1，仍有效）

- 组织：`/api/orgs`，权限 `org:*`（V16 菜单 **2018～2022**）
- 用户可选 `orgId`；Flyway 当前 **V16**；新迁移从 **V17** 起

### 已验证 / 测试

- 单元：`JwtServiceTest`、`JwtAuthFilterTest`（含黑名单 Mock）
- 集成：`AuthServiceTest`、`AuthServiceLoginLockTest` 等依赖 **本机 MySQL + Redis**
- 全量 `.\mvnw.cmd test` **先问用户**
- **可选补**：`AuthControllerTest`（login → refresh → logout 链）

---

## 下一步（建议顺序）

1. **验收**：Postman 走通 login → me → refresh（旧 refresh 失效）→ logout → me/refresh 401  
2. **R0-6 权限缓存**（`MenuAccessService` + `RoleMenuService` 失效）或 **M1 主数据**（见 `docs/M0-平台底座需求.md` / 主需求文档）  
3. 需要时再补 auth 集成测试并更新本文

---

## 实现约定（摘录）

- 组织无 `perm_code`；鉴权靠菜单按钮 `@RequirePerm`  
- 用户 API 勿返回 `passwordHash`  
- refresh 失败、锁定、未登录等对外宜统一 **`UNAUTHORIZED`**（与 `GlobalExceptionHandler` 一致）；业务校验仍可用 `BIZ_*`  
- 数据库写操作需用户确认（项目规则）

---

## 运行

- 启动：`com.velrix.VelrixApplication`；**Redis 6379**（与 `spring.data.redis.*` 一致）  
- 针对性测试：`.\mvnw.cmd test "-Dtest=AuthServiceTest,JwtServiceTest"` 等

---

## 参考

- `docs/M0-平台底座需求.md` — §6 v0.2 / §7 接口  
- `docs/Velrix-SpringBoot重写需求文档.md` — R0-1～R0-2、R0-6

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `learning-mentor` | 引导 R0-6 权限缓存或 M1 |
| `tdd` | 补 `AuthControllerTest` / 锁定与 refresh 用例 |
| `handoff` | 下一阶段再交接时覆盖本文 |
| `documentation-and-adrs` | 若要写 ADR 记录 refresh 轮换 B 的前端约定 |
