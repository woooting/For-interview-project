# HandOff — AAA / Velrix M0 平台底座

> M0-F-13 用户 list/detail、**审计查询**（`GET /api/audit-logs`）已完成，不要重做。登录、RBAC、菜单维护、用户/角色 CRUD、角色授权写审计也不要重做。**登出**与 Redis 黑名单留 **M0 v0.2** 一起做。包名是 `dto`。  
> 更新：2026-10-10。`master` 与 `origin/master` 对齐情况以 `git status` 为准；改动多在本地工作区。需求全文见 `docs/M0-平台底座需求.md`、`docs/Velrix-SpringBoot重写需求文档.md`。

主模式 **learning-mentor v2.3**：蓝图 + 分步；**业务代码开发者写**；**Flyway 脚本与验收测试代理写并跑**（用户只说要 sql 时不顺带写 Java）。技能：`~/.cursor/skills/learning-mentor/SKILL.md`。

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot **4.1.1**、Java **17**、Maven、MySQL 8、Flyway、MyBatis-Plus 3.5.17、Spring Security、JJWT 0.12.6、Lombok
- **目标**：M0 平台底座（课表 §3.2 / `docs/M0-平台底座需求.md`）
- **未引入**：Redis、Bean Validation 实际使用、MapStruct（Redis 计划 v0.2：登出黑名单、登录锁定、权限缓存）

---

## 当前进度

| 能力 | 落点 |
|------|------|
| 统一响应 / 业务码 | `shared/api` 的 `ApiResponse`、`ApiCodes` |
| 422 / 403 / 401 | `BizException` → 422；`ForbiddenException` → 403；`SecurityConfig` → 401 |
| 登录 / JWT | `application/auth/AuthService`；`infrastructure/security` 的 `JwtService`、`JwtAuthFilter` |
| 当前用户 | `GET /api/me` |
| 菜单与权限 | `application/access/MenuAccessService`；`access/dto` |
| 角色 CRUD / 列表 | `RoleService`；`GET/POST/PUT /api/roles` |
| 角色菜单授权 + **写审计** | `RoleMenuService`；`PUT /api/roles/{id}/menus` → `sys_audit_log`（`ROLE_MENU_UPDATE`，`target_type=ROLE`） |
| 用户 CRUD + list/detail | `UserService` / `UserController`；详情 `user/dto/UserDetailResponse`（含 `roleIds`）；列表 `UserResponse` 脱敏 |
| 菜单树维护 | `MenuService` / `MenuController` |
| **审计查询** | `application/audit/AuditLogService.getLog(...)`；`web/controller/audit/AuditLogController` **`GET /api/audit-logs`**，`@RequirePerm("audit:list")`；可选 query：`actor`、`targetType`、`from`、`to`（ISO 日期时间） |
| 权限拦截 | `shared/web/RequirePerm`；`web/config/RequirePermInterceptor` |

**审计语义（重要）**：v0.1 仅 **角色菜单授权变更** 写入并可查；`target_type` 表示对象种类（现多为 `ROLE`），`target_id` 为角色 id。用户分配角色、菜单 CRUD **尚未**写审计。表字段见 `domain/audit/SysAuditLog`。

菜单维护、`hasPerm`、管理员不写 `sys_role_menu`、Mapper 命名等约定仍有效；细节见历史段落或代码 `MenuService` / HandOff 旧版同类说明。

### 已验证

- Flyway **14**（`2015` `user:list`；`2016`/`2017` 审计页 + `audit:list`）
- `.\mvnw.cmd test -Dtest=UserControllerTest`：4 通过
- `.\mvnw.cmd test -Dtest=AuditLogControllerTest`：2 通过（授权后可查 log；lisi 403）
- `.\mvnw.cmd test -Dtest=MenuControllerTest`：3 通过
- 全量 `.\mvnw.cmd test` 前先问用户

### 下一步

1. **M0 v0.2**：Redis + `POST /api/auth/logout` + Token 黑名单（与开发者确认后做）。  
2. **可选**：组织树 `sys_org`（D7）。  
3. **后续**：用户/菜单变更写审计（M0-F-51 级）、`GET /api/audit-logs` 分页。  
4. 新 Flyway 从 **V15** 起。

---

## 包结构（增量）

```
application/audit/     AuditLogService
web/controller/audit/  AuditLogController
```

其余分层同课表 §3.2：`web → application → infrastructure`；Controller 不调 Mapper。

---

## 实现约定（摘录）

- REST：`GET /api/audit-logs` 用 **HTTP GET + `@GetMapping`**（仅 `@RequestMapping` 不会暴露 GET）。
- 审计查询：Service 内 **平级 if** 拼 `LambdaQueryWrapper`；四参数均可空 = 查全表（当前数据量可接受）。
- 角色授权与写审计同一事务；summary 最长 512。
- 用户列表禁止返回 `SysUser`（含 `passwordHash`）。

---

## 踩过的坑

- Flyway 已执行脚本勿改；本机库参考版本 **14**，新脚本 **V15+**。
- Spring Boot 4：`ObjectMapper` 在 `tools.jackson.databind`。
- `updateById` 跳过 null → 清空用 `LambdaUpdateWrapper.set`。
- 测试助手勿命名 `delete`（与 MockMvc 静态导入冲突）。
- Windows 终端 GBK；MySQL 中文用 `utf8mb4`。

---

## 运行

- 用户：`.\mvnw.cmd test -Dtest=UserControllerTest`
- 审计：`.\mvnw.cmd test -Dtest=AuditLogControllerTest`
- 角色授权 + 写审计：`.\mvnw.cmd test -Dtest=RoleMenuControllerTest`
- 菜单：`.\mvnw.cmd test -Dtest=MenuControllerTest`
- 启动：`com.velrix.VelrixApplication` 或 `.\mvnw.cmd spring-boot:run`

固定 id：角色 1/2/3；用户 1001 admin、1002 lisi、1003 wangwu；系统菜单约 2000～**2017**、采购 2100～2102。演示种子见 `V7`～`V14`。密码不在本文。

---

## 开发者

第一次写 Java；**业务自己写**，测试/SQL 常由代理执行。DB 迁移执行、全量 test、破坏性 SQL 须先确认。

---

## 参考

- `docs/M0-平台底座需求.md`（AC-M0-08 审计可查）
- `docs/Velrix-SpringBoot重写需求文档.md`（§3.4、`GET /api/audit-logs`）

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `learning-mentor` | v0.2 登出/Redis 或新 P1 功能（先蓝图再写代码） |
| `codebase-design` | 扩展审计 target_type（USER/MENU）时定边界 |
| `tdd` | 补集成测试 |
| `handoff` | 再交接时覆盖本文 |
