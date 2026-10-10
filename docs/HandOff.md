# HandOff — AAA / Velrix M0 平台底座

> 菜单树维护（M0-F-30～32）已完成，不要重做。登录、菜单可见性、角色授权、角色 CRUD、用户创建/编辑/分配角色也不要重做。**M0-F-13 接口代码由开发者写**；Flyway **V13**（`user:list` 菜单 2015）已由代理写好。包名是 `dto`，不要改回 `result`。  
> 更新：2026-10-07。`master` 与 `origin/master` 对齐，改动都还在工作区，未提交。需求全文不抄，看下面的引用。

主模式是 **learning-mentor v2.3**：蓝图 + 分步带做；**业务代码开发者写**（说「你来」才代写）；**Flyway 脚本、验收测试由代理写**，测试由代理跑。代理**不得**在用户只要求 sql 时顺带实现接口。技能：`~/.cursor/skills/learning-mentor/SKILL.md`。

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot **4.1.1**、Java **17**、Maven、MySQL 8、Flyway、MyBatis-Plus 3.5.17、Spring Security、JJWT 0.12.6、Lombok（`@Data` / `@RequiredArgsConstructor`）
- **目标**：M0 平台底座。课表 `docs/Velrix-SpringBoot重写需求文档.md` §3.2，范围 `docs/M0-平台底座需求.md`
- **未引入**：Redis、Validation 的实际使用、MapStruct。Redis 属 M0 v0.2

M0 v0.1 的 P0 共 20 项，都已落地。不在这个范围内的有：登出、用户列表（M0-F-13）、审计查询，这些是 P1；组织树可选；Redis 与全量审计属后续迭代。

---

## 当前进度

| 能力 | 落点 |
|------|------|
| 统一响应 / 业务码 | `shared/api` 的 `ApiResponse`、`ApiCodes` |
| 422 / 403 / 401 | `BizException` → 422；`ForbiddenException`（不继承 `BizException`）→ 403；`SecurityConfig` 入口 → 401 |
| 登录 / JWT | `application/auth/AuthService`；`infrastructure/security` 的 `JwtService`、`JwtAuthFilter` |
| 当前用户 | `GET /api/me`，`web/controller/me` |
| 菜单与权限判断 | `application/access/MenuAccessService`；返回值在 `access/dto`（`MenuNodeResponse`、`UserAccessResponse`） |
| 角色授权 | `PUT /api/roles/{id}/menus`，`application/role/RoleMenuService`；差值在 `role/dto/RoleMenuDiffResponse`。`@RequirePerm("role:grant-menus")` |
| 角色创建 / 编辑 / 列表 | `application/role/RoleService`；`POST` / `PUT /{id}` / `GET /api/roles` |
| 用户创建 / 编辑 / 分配角色 | `application/user/UserService`；`POST /api/users`、`PUT /api/users/{id}`、`PUT /api/users/{id}/roles` |
| M0-F-13 进行中 | 权限 **V13** `user:list`；待开发者补 `GET /api/users`、`GET /api/users/{id}`（详情建议含 `roleIds`，对标 `RoleService.list()`） |
| 菜单树维护 | `application/menu/MenuService`；`web/controller/menu/MenuController` 的 `POST/PUT/GET/DELETE /api/menus` |
| 权限注解与拦截 | `shared/web/RequirePerm`；`web/config/RequirePermInterceptor`，由同包 `WebMvcConfig` 注册 |
| 试权限接口 | `POST /api/purchase-orders/submit`。还不是采购业务 |

菜单维护的规则在 `MenuService.prepare`：创建传 `id = null`，编辑传正在改的 id。页面不写权限码，谁能进页面看角色有没有勾上这条菜单 id。按钮必须有唯一权限码，编辑查重用 `.ne` 排除自己。父级为空表示根，有值则父级必须存在且是页面。不能把自己或自己的子孙设为父级。下面还有子节点时不能改成按钮，也不能删除。删除先清 `sys_role_menu` 里该菜单的行，再 `deleteById`。`updateById` 会跳过 null，所以更新用 `LambdaUpdateWrapper.set`。`listTree` 是管理端整棵树，含按钮和隐藏项；`/api/me` 的侧栏树只留 `MENU`，不要混用。请求体是 `menu/dto/SaveMenuRequest`，服务直接收这个对象。管理树是 `menu/dto/MenuTreeResponse`。

`hasPerm` 只认菜单表里真实存在的按钮权限码。管理员不写 `sys_role_menu`。`sys_role_menu` 无实体，SQL 在 `SysMenuMapper`（`insertRoleMenu`、`deleteByRoleId`、`deleteByMenuId`，不要叫 `insert` / `delete`）。`sys_user_role` 无实体，SQL 在 `SysRoleMapper`。审计实体 `domain/audit/SysAuditLog`，`id` 与 `occurredAt` 插入时留空。用户分配角色、菜单增删改这一步不写审计。

### 已验证

- 库：Flyway 版本 **13**（含 `2015` / `user:list`）。`2011`～`2014` 为菜单维护按钮。不写 `sys_role_menu`
- `.\mvnw.cmd test -Dtest=MenuControllerTest`：3 个通过。写库用例带 `@Transactional`，跑完回滚
- 真实进程（8080）：管理员的菜单树里有这四个按钮；李四 `GET /api/menus` 为 403
- 角色、用户接口测试此前已通过。全量 `.\mvnw.cmd test` 前先问用户。`AuthServiceTest` 会改李四的 `enabled` 再改回

### 下一步

**下一步：开发者实现 M0-F-13**（Service + Controller；完成后由代理补测并跑 `UserControllerTest`）。之后 P1：登出、审计等，先确认再做。新 Flyway 从 **V14** 起。

---

## 包结构

课表 §3.2：先分层，再在层内按业务分子包。`record` 不是包名。传递数据的 record 放该业务下的 `dto`，类名以 `Request` 或 `Response` 结尾。只给本控制器用的请求体和响应仍放 `web/controller/{业务}`。服务要读的请求体放 `application/{业务}/dto`，避免服务反向依赖 web。Controller 必须在 `web/controller` 下，再按业务细分。

```
com.velrix
├── shared/                      # 不依赖 platform
│   ├── api/  exception/  web/   # RequirePerm、GlobalExceptionHandler
└── platform/
    ├── domain/                  # 只有表实体：user、role、menu、audit
    ├── application/
    │   ├── auth/                # AuthService
    │   ├── access/              # MenuAccessService
    │   │   └── dto/             # MenuNodeResponse、UserAccessResponse
    │   ├── role/                # RoleMenuService、RoleService
    │   │   └── dto/             # RoleMenuDiffResponse
    │   ├── menu/                # MenuService
    │   │   └── dto/             # SaveMenuRequest、MenuTreeResponse
    │   └── user/                # UserService
    ├── infrastructure/
    │   ├── persistence/         # user、role、menu、audit 的 Mapper
    │   └── security/            # SecurityConfig、Jwt*、AuthUser
    └── web/
        ├── config/              # WebMvcConfig、RequirePermInterceptor
        └── controller/
            ├── auth/  me/  role/  user/  menu/  purchase/
```

依赖方向：`web → application → infrastructure`。Controller 不调 Mapper。拦截器调用 `MenuAccessService`，所以和 `WebMvcConfig` 一起放 `web.config`。测试目录跟着主代码走。

---

## 实现约定

- 登录失败、停用账号：同一句「用户名或密码错误」
- 过滤器不抛异常。没登录进不了控制器
- 管理员不写 `sys_role_menu`。祖先补齐只加父行，侧栏树在 `loadAccess` 里挂 `children`，`BUTTON` 不进这棵树
- 角色菜单授权：先算增减（给审计），落库仍先删后插。`menuIds` 为 null 或空表示收回全部菜单。审计与替换同一事务
- 布尔列不要用 `is` 前缀：`administrator` → `is_administrator`，`hidden` → `is_hidden`
- `AuthUser` 在 `infrastructure.security`，不是表实体。拦截器和控制器仍直接读 `SecurityContextHolder`。IP 用 `getRemoteAddr()`
- 改包后 IDEA 里已启动的进程要重启才吃到新类
- 角色：名称 `trim` 后必填且唯一（编辑用 `ne` 排除自己）；描述可空，更新用 `LambdaUpdateWrapper.set` 才能写成 null；`administrator` 缺省 false 用 `Boolean.TRUE.equals`；列表按 `seq` 升序。`id` 留空走雪花，`seq` 与时间留空
- 用户：登录名 `trim` 后写入 `username`，`Locale.ROOT` 小写写入 `usernameNorm` 并据此查重；密码只存 `PasswordEncoder.encode` 的结果。创建时 `enabled` 用 `!Boolean.FALSE.equals`，缺省 true。编辑不改登录名；`enabled` 不传则保持原值；密码空白则不改哈希。分配角色是全量覆盖，`roleIds` 为 null 或空则清空。接口用 `UserResponse`，不含 `passwordHash`
- 菜单：名称 `trim` 后必填。类型只有 `MENU` / `BUTTON`。空白路由存 null。`sort` 缺省 0，`hidden` 只有传入 true 才为 true。页面权限码存 null

---

## 踩过的坑

- Spring Boot 4 的 `ObjectMapper` 在 `tools.jackson.databind`。注解仍是 `com.fasterxml.jackson.annotation`
- 只有 `flyway-core` 时启动不会迁移。已用 `spring-boot-starter-flyway`，并保留 `flyway-mysql`
- `SecurityConfig` 的过滤链方法必须有 `@Bean`
- 已执行的 Flyway 脚本不要改。本机库已到 version **12**。新数据用 V13 起
- Windows 终端中文是 GBK 乱码；HTTP body 实际是 UTF-8。核对中文用 `mysql --default-character-set=utf8mb4`
- `pom.xml` 的 `java.version` 是 17。不要加 `--enable-preview` 去配源码 21
- `DELETE` 语句不写 `*`
- `updateById` 会跳过 null 字段。要清空的列用 `LambdaUpdateWrapper.set`。用户编辑要保留原密码时，不要把哈希 `set` 成 null
- 测试里不要把助手方法命名为 `delete`，会和 `MockMvcRequestBuilders.delete` 的静态导入撞车

---

## 运行

- 菜单接口：`.\mvnw.cmd test -Dtest=MenuControllerTest`
- 角色接口：`.\mvnw.cmd test -Dtest=RoleControllerTest`
- 用户接口：`.\mvnw.cmd test -Dtest=UserControllerTest`
- 授权与侧栏：`.\mvnw.cmd test "-Dtest=RoleMenuControllerTest,MenuAccessServiceTest"`
- 全量：`.\mvnw.cmd test`（会写库，先问用户）
- 启动：IDEA 跑 `com.velrix.VelrixApplication`，或 `.\mvnw.cmd spring-boot:run`。长命令拆开跑，不要把打包、启动、轮询串成一条

演示数据在 `V7__seed_m0_platform.sql` 到 `V12__seed_menu_crud_menus.sql`。本文不记密码。

固定 id：角色 1/2/3，用户 1001 `admin`、1002 `lisi`、1003 `wangwu`，菜单 2000～2014、2100～2102。`sys_org` 还没做。

---

## 开发者

第一次写 Java。会拿分层文档纠正包结构，以课表为准。**Flyway 脚本与功能测试由代理写并跑测试**；**业务接口由开发者写**（代理只写 sql 时不碰 Java）。数据库的增删改必须先得到同意。

---

## 参考

- `docs/Velrix-SpringBoot重写需求文档.md`（§3.2 分层、§3.4 接口、M0 章）
- `docs/M0-平台底座需求.md`（下一功能从 M0-F-13 起）
- 原项目 `D:\gitProject\VelrixWorkHub`：只参考业务规则，不抄代码和形状

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `learning-mentor` | 引导写用户列表（下一会话的主模式） |
| `codebase-design` | 再动包或模块边界时 |
| `clean-code` | 分包、命名 |
| `tdd` | 补验收测试 |
| `handoff` | 再交接时覆盖本文 |
