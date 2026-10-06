# HandOff — AAA / Velrix M0 平台底座

> 下一会话做菜单树维护（M0-F-30～32）。用户的创建、编辑、分配角色（M0-F-10/11/12）已完成。不要重做登录、角色、用户，也不要再改已定的包结构，除非开发者明确要求。  
> 更新：2026-10-06。工作区改动还没提交。需求全文不抄，看下面的引用。

主模式是带着开发者写：先讲数据流，再给填空，不要一次把类写完。开发者说「你来」时，只写卡住的那一段并解释。技能文件：`~/.cursor/skills/learning-mentor/SKILL.md`。

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot **4.1.1**、Java **17**、Maven、MySQL 8、Flyway、MyBatis-Plus 3.5.17、Spring Security、JJWT 0.12.6、Lombok（`@Data` / `@RequiredArgsConstructor`）
- **目标**：M0 平台底座。课表 `docs/Velrix-SpringBoot重写需求文档.md` §3.2，范围 `docs/M0-平台底座需求.md`
- **未引入**：Redis、Validation 的实际使用、MapStruct。Redis 属 M0 v0.2

---

## 当前进度

登录、`GET /api/me`、`@RequirePerm`、`PUT /api/roles/{id}/menus` 已可用。包已按业务拆完。

角色 CRUD（M0-F-20）已完成。`RoleService` 的 `create`、`update`、`list` 在 `application/role/RoleService`：名称去空格后必填且唯一（编辑用 `ne` 排除自己），描述可空，管理员标记用 `Boolean.TRUE.equals`，列表按 `seq` 升序。`web/controller/role/RoleController` 与 `SaveRoleRequest` 提供 `POST` / `PUT /{id}` / `GET /api/roles`。V10 已执行：菜单 `2005`～`2007` 为 `role:create`、`role:update`、`role:list`，不写 `sys_role_menu`。

| 能力 | 落点 |
|------|------|
| 统一响应 / 业务码 | `shared/api` 的 `ApiResponse`、`ApiCodes` |
| 422 / 403 / 401 | `BizException` → 422；`ForbiddenException`（不继承 `BizException`）→ 403；`SecurityConfig` 入口 → 401 |
| 登录 / JWT | `application/auth/AuthService`；`infrastructure/security` 的 `JwtService`、`JwtAuthFilter` |
| 菜单与权限判断 | `application/access/MenuAccessService`；返回值在 `access/result`（`MenuNode`、`UserAccess`） |
| 角色授权 | `application/role/RoleMenuService`；差值在 `role/result/RoleMenuDiff` |
| 角色创建 / 编辑 / 列表 | `application/role/RoleService`；`web/controller/role/RoleController` |
| 用户创建 / 编辑 / 分配角色 | `application/user/UserService`；`web/controller/user/UserController`。返回 `UserResponse`，不含密码哈希。`sys_user_role` 无实体，SQL 在 `SysRoleMapper`（`insertUserRole`，不要叫 `insert`） |
| 授权接口 | `PUT /api/roles/{id}/menus`，`web/controller/role/RoleMenuController`，`@RequirePerm("role:grant-menus")` |
| 权限注解 | `shared/web/RequirePerm` |
| 拦截器 | `web/config/RequirePermInterceptor`，由同包 `WebMvcConfig` 注册 |
| 试权限接口 | `POST /api/purchase-orders/submit`，`web/controller/purchase`。还不是采购业务 |

`hasPerm` 只认菜单表里真实存在的按钮权限码。管理员不写 `sys_role_menu`。`sys_role_menu` 无实体，SQL 在 `infrastructure/persistence/menu/SysMenuMapper`（`insertRoleMenu`，不要叫 `insert`）。审计实体 `domain/audit/SysAuditLog`，`id` 与 `occurredAt` 插入时留空。

### 已验证

- `MenuAccessServiceTest`、`PurchaseOrderControllerTest`、`RoleMenuControllerTest` 在分包后仍通过。授权测试带 `@Transactional`，跑完回滚
- 库：Flyway 版本 11。菜单 `2004` 为「保存授权」；`2005`～`2007` 为角色新建/编辑/列表；`2008`～`2010` 为用户新建/编辑/分配角色。采购员仍是角色 2，菜单 `2100/2101/2102`。测试插入的「验收角色甲」「验收用户甲」已回滚
- 真实进程（8080）已核对 401 / `/api/me` / 伪造 token / 王五提交 403，以及管理员把采购员改成只留 `2101` 后再改回。细节不重复记
- `AuthServiceTest` 会改李四的 `enabled` 再改回。全量 `.\mvnw.cmd test` 前先问用户
- `.\mvnw.cmd test -Dtest=RoleControllerTest`：3 个用例通过。写库的用例带 `@Transactional`，跑完回滚
- `.\mvnw.cmd test -Dtest=UserControllerTest`：3 个用例通过。写库的用例带 `@Transactional`，跑完回滚。李四仍只绑角色 2

### 下一步

菜单树维护（M0-F-30～32）：父级、名称、前端路由、排序、是否隐藏；类型为页面或按钮；按钮携带权限码。一次只引导一步。会写库的脚本和测试先得到用户同意。

用户列表（M0-F-13）是 P1，这一步不做。用户/角色/菜单变更的全量审计、`GET /api/audit-logs`、Redis 都不是这一步。

---

## 包结构

课表 §3.2：先分层，再在层内按业务分子包。`record` 不是包名；用例返回值放该业务下的 `result`。Controller 必须在 `web/controller` 下，再按业务细分。开发者已纠正过「控制器直接放在 `web/auth` 这种业务包」的做法。

```
com.velrix
├── shared/                      # 不依赖 platform
│   ├── api/  exception/  web/   # RequirePerm、GlobalExceptionHandler
└── platform/
    ├── domain/                  # 只有表实体：user、role、menu、audit
    ├── application/
    │   ├── auth/                # AuthService
    │   ├── access/              # MenuAccessService
    │   │   └── result/          # MenuNode、UserAccess
    │   ├── role/                # RoleMenuService、RoleService
    │   │   └── result/          # RoleMenuDiff
    │   └── user/                # UserService
    ├── infrastructure/
    │   ├── persistence/         # user、role、menu、audit 的 Mapper
    │   └── security/            # SecurityConfig、Jwt*、AuthUser
    └── web/
        ├── config/              # WebMvcConfig、RequirePermInterceptor
        └── controller/
            ├── auth/  me/  role/  user/  purchase/
```

依赖方向：`web → application → infrastructure`。Controller 不调 Mapper。拦截器调用 `MenuAccessService`，所以和 `WebMvcConfig` 一起放 `web.config`，不放 `infrastructure`，也不放 `shared`。请求体 record 与对应 Controller 放同一个 `controller/{业务}` 包，不再单开 `dto`。测试目录跟着主代码走。

---

## 实现约定

- 登录失败、停用账号：同一句「用户名或密码错误」
- 过滤器不抛异常。没登录进不了控制器
- 管理员不写 `sys_role_menu`。祖先补齐只加父行，树在 `loadAccess` 里挂 `children`，`BUTTON` 不进树
- 覆盖授权：先算增减，再删旧插新。`menuIds` 为 null 或空表示收回全部菜单。审计与替换同一事务
- 布尔列不要用 `is` 前缀：`administrator` → `is_administrator`，`hidden` → `is_hidden`
- `AuthUser` 在 `infrastructure.security`，不是表实体。拦截器和控制器仍直接读 `SecurityContextHolder`。IP 用 `getRemoteAddr()`
- 改包后 IDEA 里已启动的进程要重启才吃到新类
- 角色创建：名称 `trim` 后必填且唯一；`administrator` 缺省 false 用 `Boolean.TRUE.equals`；`id` 留空走雪花，`seq` 与时间留空
- 用户：登录名 `trim` 后写入 `username`，小写写入 `usernameNorm` 并据此查重；密码只存哈希；`enabled` 创建时用 `!Boolean.FALSE.equals` 缺省 true。编辑不改登录名，`enabled` 不传则保持原值，密码空白则不改哈希。分配角色是全量覆盖，`roleIds` 空则清空。不写这次审计。接口返回不含 `passwordHash`

---

## 踩过的坑

- Spring Boot 4 的 `ObjectMapper` 在 `tools.jackson.databind`。注解仍是 `com.fasterxml.jackson.annotation`
- 只有 `flyway-core` 时启动不会迁移。已用 `spring-boot-starter-flyway`，并保留 `flyway-mysql`
- `SecurityConfig` 的过滤链方法必须有 `@Bean`
- 已执行的 Flyway 脚本不要改。本机库已到 version 11。新数据用下一个版本号
- Windows 终端中文是 GBK 乱码；HTTP body 实际是 UTF-8。核对中文用 `mysql --default-character-set=utf8mb4`
- `pom.xml` 的 `java.version` 是 17。不要加 `--enable-preview` 去配源码 21
- `DELETE` 语句不写 `*`

---

## 运行

- 授权与菜单：`.\mvnw.cmd test "-Dtest=RoleMenuControllerTest,MenuAccessServiceTest"`
- 单类：`.\mvnw.cmd test -Dtest=PurchaseOrderControllerTest`
- 全量：`.\mvnw.cmd test`（会写库，先问用户）
- 启动：IDEA 跑 `com.velrix.VelrixApplication`，或 `.\mvnw.cmd spring-boot:run`。长命令拆开跑，不要把打包、启动、轮询串成一条

演示数据在 `V7__seed_m0_platform.sql`、`V8__seed_child_menu_only.sql`、`V9__seed_role_grant_menu.sql`、`V10__seed_role_crud_menus.sql`、`V11__seed_user_crud_menus.sql`。本文不记密码。

固定 id：角色 1/2/3，用户 1001 `admin`、1002 `lisi`、1003 `wangwu`，菜单 2000～2010、2100～2102。`sys_org` 还没做。

角色接口测试：`.\mvnw.cmd test -Dtest=RoleControllerTest`  
用户接口测试：`.\mvnw.cmd test -Dtest=UserControllerTest`

---

## 开发者

第一次写 Java。会拿分层文档纠正包结构，以课表为准。终端里长命令会干等。数据库的增删改必须先得到同意。

---

## 参考

- `docs/Velrix-SpringBoot重写需求文档.md`（§3.2 分层、§3.4 接口、M0 章）
- `docs/M0-平台底座需求.md`（下一功能从 M0-F-30 起）
- 原项目 `D:\gitProject\VelrixWorkHub`：只参考业务规则，不抄代码和形状

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `learning-mentor` | 引导写菜单树维护（当前主模式） |
| `codebase-design` | 再动包或模块边界时 |
| `clean-code` | 分包、命名 |
| `tdd` | 补验收测试 |
| `handoff` | 再交接时覆盖本文 |
