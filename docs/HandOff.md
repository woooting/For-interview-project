# HandOff — AAA / Velrix M0 平台底座

> 供下一会话接手的摘要。详细需求见引用文档，此处不重复全文。  
> 更新：2026-10-03

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot **4.1.1**、Java **17**、Maven、MySQL 8、Flyway；`webmvc` + **JDBC**（HikariCP 默认池）
- **目标**：M0 平台底座（JWT、RBAC、菜单/`perm_code`）；课表见 `docs/Velrix-SpringBoot重写需求文档.md`，产品范围见 `docs/M0-平台底座需求.md`

**pom 已引入**：MyBatis-Plus 3.5.17（`spring-boot4-starter` + `jsqlparser`）、Spring Security、Validation、`spring-boot-starter-aspectj`、JJWT 0.12.6、`spring-security-test`。  
**尚未引入**：Redis、业务 Java（除 `AaaApplication`）；统一响应/Security 配置等由开发者自写。

---

## 已拍板的设计决策（摘要）

- 认证规划：**JWT 无状态**；请求只带 Token，**不传 `perm_code`**
- `sys_user`：`username_norm` 唯一；登录不索引 `password_hash`
- `sys_role`：无 `enabled`；`seq` 自增唯一；`name` 唯一；`is_administrator`
- `sys_menu`：根 `parent_id NULL`；`is_hidden`；`perm_code` 与 `path` 分离；`MENU` / `BUTTON`
- 多对多：`sys_user_role`、`sys_role_menu`（联合主键）
- 权限：v0.1 可每次查库；v0.2 Redis `aaa:auth:perm:{userId}`（未实现）
- M0 §4 定稿 D1～D8：未全部写入 `M0-平台底座需求.md`，接手可问用户

---

## Flyway（`src/main/resources/db/migration/`）

| 版本 | 内容 | 状态 |
|------|------|------|
| V1 | `sys_user` | 脚本已提交 |
| V2 | `sys_role` | 脚本已提交 |
| V3 | `sys_menu` | 脚本已提交 |
| V4 | `sys_user_role` | 脚本已提交 |
| V5 | `sys_role_menu` | 脚本已提交 |
| V6 | `sys_audit_log` | 脚本已提交 |
| **V7** | **`V7__seed_m0_platform.sql` 种子数据** | **脚本已提交** |
| — | `sys_org`（可选，D7） | 未做 |

**M0 v0.1 必需表（含审计）在脚本层面已齐**；组织表可选。本地库需 `flyway_schema_history` 跑到 **V7** 才有种子（未跑则启动/migrate 补跑）。

配置：`application.properties` + 可选 **`application-local.properties`**（密码勿提交）。

---

## 种子数据（V7，仅开发演示）

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `admin123` | 系统管理员（`is_administrator=1`，不写 `sys_role_menu`） |
| `lisi` | `admin123` | 采购员（已授权菜单 2100/2101/2102，含 `purchase-order:submit`） |

固定 id：角色 1/2，用户 1001/1002，菜单 2000～2102。详见 `V7__seed_m0_platform.sql`。

---

## 建议的下一步

1. **M0 接口**：`POST /api/auth/login`（BCrypt）→ JWT → `GET /api/me`（菜单树 + permissions + 祖先补齐）→ `@RequirePerm` 示例
3. **`PUT /api/roles/{id}/menus`**：覆盖 `sys_role_menu` + **INSERT `sys_audit_log`**（AC-M0-08）
4. Spring Security 过滤器链；Redis 权限缓存 → M0 v0.2

---

## 运行

IDEA：Maven 项目 + JDK 17 → Run `com.example.aaa.AaaApplication`  
CLI：`.\mvnw.cmd spring-boot:run`

---

## 参考

- `docs/M0-平台底座需求.md`
- `docs/Velrix-SpringBoot重写需求文档.md`
- 源对照：`D:\gitProject\VelrixWorkHub`（勿直接抄 GPL 源码）

---

## 注意

- `application.properties` 若含明文 DB 密码，宜迁到 `application-local.properties`
- BUTTON 必须唯一 `perm_code`；超级管理员在代码中绕过 `sys_role_menu`
- V7 重复执行会主键冲突；仅 Flyway 首次 migrate 安全

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `tdd` | 登录/RBAC 验收 |
| `spec-driven-development` | 接口与表对齐 |
| `clean-code` | 分包 `domain/application/web` |
| `handoff` | 再交接时覆盖本文 |
