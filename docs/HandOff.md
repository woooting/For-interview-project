# HandOff — AAA / Velrix M0 平台底座

> 供下一会话接手的摘要。详细需求见引用文档，此处不重复全文。  
> 更新：2026-10-04（`web` 拆成 controller / dto，7d 代码已写、端到端未验）

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot **4.1.1**、Java **17**、Maven、MySQL 8、Flyway、MyBatis-Plus 3.5.17、Spring Security、JJWT 0.12.6、**Lombok（后加，在用 @Data/@RequiredArgsConstructor）**
- **目标**：M0 平台底座（JWT、RBAC、菜单/`perm_code`）；课表见 `docs/Velrix-SpringBoot重写需求文档.md`，产品范围见 `docs/M0-平台底座需求.md`
- **未引入**：Redis、Validation 实际使用、MapStruct；Redis 属 M0 v0.2

---

## 当前进度

### 已完成：登录闭环（第 1～6 步 + 7a～7c）

| 文件 | 位置 | 职责 |
|------|------|------|
| `ApiResponse` / `ApiCodes` | `shared/api` | 统一响应壳（record）；code：OK / BIZ_ERROR / BIZ_LOGIN_FAILED / UNAUTHORIZED |
| `BizException` | `shared/exception` | code + message，继承 RuntimeException |
| `GlobalExceptionHandler` | `shared/web` | BizException → HTTP 422 + `ApiResponse.fail` |
| `SysUser` | `platform/domain` | `sys_user` 实体（Lombok @Data，MyBatis-Plus @TableName/@TableId） |
| `AuthUser` | `platform/domain` | record(id, username)，JWT 解析后的认证主体 |
| `AuthService` | `platform/application` | `login()`：规范化用户名→查库→BCrypt→签发 JWT；`getById()`：按 id 查用户 |
| `SysUserMapper` | `platform/infrastructure/persistence` | `@Mapper extends BaseMapper<SysUser>` |
| `SecurityConfig` | `platform/infrastructure/security` | PasswordEncoder Bean；FilterChain：STATELESS、login 白名单、其余需认证、JwtAuthFilter、401 统一 JSON |
| `JwtService` | `platform/infrastructure/security` | `issue(SysUser)` / `parse(token)`，HMAC-SHA256 |
| `JwtAuthFilter` | `platform/infrastructure/security` | `OncePerRequestFilter`：Bearer token → Claims → AuthUser → SecurityContext（不拒绝，只识别） |
| `AuthController` | `platform/web/controller` | `POST /api/auth/login` 已 curl 验证通过（OK + token；错密码 422 + BIZ_LOGIN_FAILED） |
| `LoginRequest` / `LoginResponse` / `MeResponse` | `platform/web/dto` | HTTP 边界 record。`MeResponse`：id / username / displayName |
| `MeController` | `platform/web/controller` | `GET /api/me`：SecurityContext 取 AuthUser → `authService.getById` → `MeResponse`。代码已写，端到端未验 |

### 测试：16 个全绿（`.\mvnw.cmd test`）

`AuthServiceTest`(5)、`SysUserMapperTest`(2)、`JwtServiceTest`(2)、`JwtAuthFilterTest`(3)、`ApiResponseTest`(2)、`GlobalExceptionHandlerTest`(1)、`VelrixApplicationTests`(1)

### 待做：第 7d 及之后

1. **端到端验证**（7d 代码已写，这一步未做）：打包 → `java -jar target/aaa-0.0.1-SNAPSHOT.jar --server.port=18080` 后台启动 → sleep 检查端口 → curl 三场景（无 token 401 / 真 token 200 / 伪造 401）→ 杀进程。**分多条命令，勿串长命令干等（用户明确要求）**
2. **M0 RBAC**：`SysRole`/`SysMenu` 实体 + Mapper；`/api/me` 扩展菜单树（祖先补齐 R0-3）+ 权限码；`@RequirePerm` 注解 + 拦截器（R0-4，403 统一响应）
3. **`PUT /api/roles/{id}/menus`** 覆盖式授权 + `sys_audit_log`（AC-M0-08）
4. 用户/角色/菜单 CRUD；v0.2 Redis（权限缓存、refresh token、登录锁定）

---

## 包结构

```
com.velrix
├── shared/                    # 全项目技术公共设施（HandOff 拍板，文档 §3.2 未画）
│   ├── api/  exception/  web/
└── platform/                  # M0 模块（文档 §3.2：用户/角色/菜单/审计都归此模块）
    ├── domain/                # 实体 + 领域概念；不依赖任何层
    ├── application/           # 用例/事务（AuthService …）
    ├── infrastructure/
    │   ├── persistence/       # Mapper
    │   └── security/          # SecurityConfig、JwtService、JwtAuthFilter
    └── web/
        ├── controller/        # AuthController、MeController
        └── dto/               # HTTP 边界 record：LoginRequest、LoginResponse、MeResponse
```

- 依赖方向：`web → application → infrastructure`；`domain` 谁都不依赖。Controller 不许直接调 Mapper
- DTO 归属：只在 HTTP 边界用的 record 放 `web/dto`（请求和响应放一起）；领域值对象（AuthUser）放 `domain`；全项目通用（ApiResponse）放 `shared`
- 拆包时机：包里混了不同职责、读起来乱时再拆。已拆过两次：infrastructure → persistence/security；web → controller/dto

---

## 关键实现决策

- 登录：`usernameNorm = trim + toLowerCase(Locale.ROOT)` 查 `username_norm`；BCrypt `matches`
- 失败统一 `BIZ_LOGIN_FAILED` + "用户名或密码错误"（不泄露账号是否存在；停用同此）
- 业务错误 HTTP 422；未认证 401 + `{"code":"UNAUTHORIZED",...}`（EntryPoint 用 ObjectMapper 序列化）
- JWT：subject=userId，claim username，30 分钟；密钥配置 `velrix.jwt.secret`（≥32 字节）/`velrix.jwt.access-token-minutes`
- Security 链：STATELESS + `.requestMatchers("/api/auth/login").permitAll()` + `.anyRequest().authenticated()` + `addFilterBefore(JwtAuthFilter, UsernamePasswordAuthenticationFilter)`
- 过滤器策略：token 无效**不抛不拒**，只不设置认证；拒绝交给授权规则

---

## 踩过的坑（重要）

- **Spring Boot 4 用 Jackson 3**：`ObjectMapper` 在 `tools.jackson.databind`（`com.fasterxml.jackson.databind` 不在 classpath）；注解包 `com.fasterxml.jackson.annotation` 不变。网上老教程全是 Jackson 2 写法，注意
- `SecurityConfig` 的 FilterChain 方法漏 `@Bean` → 配置不生效（已修）
- Lombok 是开发者自己加进 pom 的；@Data/@RequiredArgsConstructor 已在使用
- 脚手架注释（"填空1/2…"）残留是常态，每次检查时顺手清理
- Windows 终端显示中文乱码（GBK）属正常，Maven 输出/HTTP 响应 body 实际为 UTF-8

---

## 运行与验证

- 全量测试：`.\mvnw.cmd test`；单类：`.\mvnw.cmd test -Dtest=AuthServiceTest`
- IDEA：Run `com.velrix.VelrixApplication`；CLI：`.\mvnw.cmd spring-boot:run`
- 登录 curl 示例（端口自定）：

```powershell
Invoke-RestMethod -Uri "http://localhost:18080/api/auth/login" -Method Post `
  -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
```

---

## 种子与表（Flyway V1–V7 已全部落库）

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `admin123` | 系统管理员（`is_administrator=1`，不写 `sys_role_menu`） |
| `lisi` | `admin123` | 采购员（菜单 2100/2101/2102，含 `purchase-order:submit`） |

固定 id：角色 1/2，用户 1001/1002，菜单 2000～2102。`sys_org` 未做（D7 可选）。V7 重复执行会主键冲突，仅 Flyway 首次 migrate 安全。

---

## 开发者特点（新手，重要）

- 第一次写 Java：先讲概念/数据流，再给骨架填空；一次别抛太多新 API
- **不写测试**：检查 + 补测试 + 跑验证由接手方做（改代码后必须编译 + 跑相关测试）
- 会主动对照文档质疑架构（做过两次有价值的纠偏）：一切以 `docs/Velrix-SpringBoot重写需求文档.md` 为准，发现自己的建议与文档冲突就承认并改正
- 终端体验敏感：长命令（打包+启动+轮询）会干等，要拆段执行

---

## 参考

- `docs/Velrix-SpringBoot重写需求文档.md`（§3.2 分层、§3.4 接口、M0 章为验收基准）
- `docs/M0-平台底座需求.md`
- 原项目：`D:\gitProject\VelrixWorkHub`（C#/Blazor/FreeSql，**仅业务规则参考，勿抄代码/形状**）

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `learning-mentor` | 引导开发者写下一步（当前主模式） |
| `clean-code` | 分包/重构审查 |
| `tdd` | RBAC 验收用例 |
| `handoff` | 再交接时覆盖本文 |
