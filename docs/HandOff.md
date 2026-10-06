# HandOff — AAA / Velrix M0 平台底座

> 下一会话接着做。需求全文不抄，看下面的引用。  
> 更新：2026-10-05。工作区改动还没提交。

主模式是带着开发者写：先讲数据流，再给填空，不要一次把类写完。技能文件：`~/.cursor/skills/learning-mentor/SKILL.md`。

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot **4.1.1**、Java **17**、Maven、MySQL 8、Flyway、MyBatis-Plus 3.5.17、Spring Security、JJWT 0.12.6、Lombok（`@Data` / `@RequiredArgsConstructor` / `@Getter`）
- **目标**：M0 平台底座。课表 `docs/Velrix-SpringBoot重写需求文档.md`，范围 `docs/M0-平台底座需求.md`
- **未引入**：Redis、Validation 的实际使用、MapStruct。Redis 属 M0 v0.2（登录锁定、refresh、权限缓存）

---

## 当前进度

登录、`GET /api/me`（用户 + 菜单树 + 权限码）、`@RequirePerm` 拦截都已经能用。

| 能力 | 落点 |
|------|------|
| 统一响应 / 业务码 | `shared/api` 的 `ApiResponse`、`ApiCodes`（`OK` / `BIZ_ERROR` / `BIZ_LOGIN_FAILED` / `UNAUTHORIZED` / `FORBIDDEN`） |
| 422 | `BizException` → `GlobalExceptionHandler` |
| 403 | `ForbiddenException` **不继承** `BizException`，单独处理方法，HTTP 403 + `code=FORBIDDEN` |
| 401 | `SecurityConfig` 的 `authenticationEntryPoint`，JSON `UNAUTHORIZED` |
| 登录 | `AuthService.login`：`trim` + `toLowerCase(Locale.ROOT)` 查 `username_norm`，BCrypt，失败一律 `BIZ_LOGIN_FAILED` |
| JWT | `JwtService`：subject=userId，claim `username`，30 分钟。配置键 `velrix.jwt.secret`、`velrix.jwt.access-token-minutes` |
| 过滤器 | `JwtAuthFilter` 只识别 token，无效则不设置认证；拒绝交给 `.anyRequest().authenticated()` |
| 菜单 | `MenuAccessService`：`listGrantedMenus` → `listVisibleMenus`（按 `parentId` 补祖先）→ `loadAccess`（`MENU` 拼树，`BUTTON` 的 `permCode` 进列表）→ `hasPerm` |
| 树的类型 | `MenuNode`、`UserAccess` 在 `application`。HTTP 外形 `MeResponse` 在 `web/dto`，可以引用 `MenuNode` |
| 权限注解 | `shared/web/RequirePerm`（`@Target(METHOD)`，`RUNTIME`，属性名 `value`） |
| 拦截器 | `platform/web/RequirePermInterceptor`，由 `WebMvcConfig.addInterceptors` 注册。无注解放行；无权限抛 `ForbiddenException` |
| 试权限的接口 | `POST /api/purchase-orders/submit`，注解 `purchase-order:submit`。还不是采购业务 |

布尔列不要用 `is` 前缀：`SysRole.administrator` 配 `@TableField("is_administrator")`，`SysMenu.hidden` 配 `@TableField("is_hidden")`。关联表没有实体，角色 id / 菜单 id 用 Mapper 上的 `@Select` 查。

### 已验证

- `MenuAccessServiceTest`（3）：管理员全树、李四只有采购、王五只授权子菜单 `2101` 时树里仍有父菜单 `2100`
- `PurchaseOrderControllerTest`（3）：管理员与李四提交 200；王五 403，`message` 为「没有权限」
- 更早的登录 / JWT / Mapper 测试未在本轮全量重跑。`AuthServiceTest` 会改李四的 `enabled` 再改回，重跑全量前先问用户

### 下一步

1. `PUT /api/roles/{id}/menus`：覆盖式授权，并写 `sys_audit_log`（课表 R0-5，需求 AC-M0-08）。表已在 Flyway V6，代码还没有
2. HTTP 端到端还没做：打包后单独起进程，再分开 curl（无 token 401、登录后 `/api/me`、伪造 token 401、王五调 submit 403）。不要把打包、启动、轮询串成一条长命令
3. 之后才是用户/角色/菜单 CRUD，以及 v0.2 的 Redis

---

## 包结构

```
com.velrix
├── shared/                 # 技术公共设施，不依赖 platform
│   ├── api/  exception/  web/     # RequirePerm、GlobalExceptionHandler 在 web
└── platform/
    ├── domain/             # 实体 + AuthUser。不依赖别的层
    ├── application/        # AuthService、MenuAccessService、MenuNode、UserAccess
    ├── infrastructure/
    │   ├── persistence/
    │   └── security/       # SecurityConfig、JwtService、JwtAuthFilter
    └── web/
        ├── controller/     # Auth、Me、PurchaseOrder
        ├── dto/
        ├── RequirePermInterceptor.java
        └── WebMvcConfig.java
```

依赖方向：`web → application → infrastructure`。Controller 不调 Mapper。拦截器要调 `MenuAccessService`，所以放 `platform/web`，不放 `shared`，也不放 `infrastructure`。

---

## 实现约定

- 登录失败、停用账号：同一句「用户名或密码错误」，不暴露账号是否存在
- 过滤器不抛异常。没登录进不了控制器；`/api/me` 和拦截器里不再为 `authentication == null` 写防空
- 管理员不写 `sys_role_menu`。`listGrantedMenus` 看到 `administrator == true` 就 `selectList(null)`，所以 `hasPerm` 不用再判一次管理员
- 祖先补齐只往列表里加缺的父行，不改 `path`，也不在这一步排序或做树。树在 `loadAccess` 里按 `sort` 挂 `children`，`BUTTON` 不进树
- `CurrentAuthUser` 还没抽。拦截器和 `MeController` 仍直接读 `SecurityContextHolder`

---

## 踩过的坑

- Spring Boot 4 的 `ObjectMapper` 在 `tools.jackson.databind`。注解仍是 `com.fasterxml.jackson.annotation`
- 只有 `flyway-core` 时启动不会迁移。已用 `spring-boot-starter-flyway`，并保留 `flyway-mysql`
- `SecurityConfig` 的过滤链方法必须有 `@Bean`
- 已执行过的 Flyway 脚本不要改。新数据用下一个版本号。V8 已在本机库执行到 version 8
- Windows 终端中文是 GBK 乱码；Maven / HTTP body 实际是 UTF-8
- `pom.xml` 的 `java.version` 是 17。不要再加 `--enable-preview` 配源码 21，和 17 冲突，编译会失败

---

## 运行

- 单类：`.\mvnw.cmd test -Dtest=PurchaseOrderControllerTest`
- 全量：`.\mvnw.cmd test`（会写库，先问用户）
- 启动：IDEA 跑 `com.velrix.VelrixApplication`，或 `.\mvnw.cmd spring-boot:run`
- 演示账号和授权写在 `src/main/resources/db/migration/V7__seed_m0_platform.sql`、`V8__seed_child_menu_only.sql`。交接文档不记密码

固定 id：角色 1/2/3，用户 1001 `admin`、1002 `lisi`、1003 `wangwu`，菜单 2000～2102。`sys_org` 还没做。

---

## 开发者

第一次写 Java。会拿文档纠正分层，以课表为准。终端里长命令会干等，要拆开跑。数据库的增删改必须先得到用户同意。

---

## 参考

- `docs/Velrix-SpringBoot重写需求文档.md`（§3.2 分层、§3.4 接口、M0 章）
- `docs/M0-平台底座需求.md`
- 原项目 `D:\gitProject\VelrixWorkHub`：只参考业务规则，不抄代码和形状

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `learning-mentor` | 引导写下一步（当前主模式） |
| `clean-code` | 分包、命名 |
| `tdd` | 接手方补验收测试 |
| `handoff` | 再交接时覆盖本文 |
