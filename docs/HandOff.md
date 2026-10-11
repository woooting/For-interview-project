# HandOff — AAA / Velrix M0 平台底座

> **M0-F-60 / M0-F-61** 已完成：组织树 + 用户可选 `orgId`（创建/更新/列表/详情）。登出 + Redis → **v0.2**。  
> 更新：2026-10-11。需求见 `docs/M0-平台底座需求.md` §3.7。

主模式 **learning-mentor v2.3**：蓝图 + 分步；Flyway / 测试常由代理写并跑。技能：`~/.cursor/skills/learning-mentor/SKILL.md`。

---

## 项目是什么

- **路径**：`D:\gitProject\AAA`
- **栈**：Spring Boot **4.1.1**、Java **17**、Maven、MySQL 8、Flyway、MyBatis-Plus、Spring Security、JJWT
- **未引入**：Redis（v0.2：登出黑名单、登录锁定、权限缓存）

---

## 当前进度

| 能力 | 落点 |
|------|------|
| 登录 / JWT / `/api/me` | `AuthService`、`Jwt*`、`MeController` |
| RBAC、菜单维护、用户/角色 CRUD | 见既有 Controller / Service |
| 角色授权 + 写审计 | `RoleMenuService` → `sys_audit_log` |
| 审计查询 | `GET /api/audit-logs`，`audit:list`（V14） |
| **组织树** | `OrgService`、`OrgController` → `/api/orgs`；权限 `org:*`（V16 **2018～2022**） |
| **用户归属组织** | `CreateUserRequest` / `UpdateUserRequest` 可选 `orgId`；更新时 `orgId==null` **不修改**原部门；`UserService.requireOrgExists` |

### 组织 API（已实现）

- 列表：`GET /api/orgs` → `List<OrgTreeResponse>`（`org:list`）
- 创建/更新 body：`SaveOrgRequest`（`parentId`, `name`, `type`, `sort`）
- 删除：无子部门且无用户 `org_id` 引用（`OrgService.delete`）

### 已验证

- Flyway **16**（V15 `sys_org`，V16 组织菜单种子）
- `OrgControllerTest` 3、`UserControllerTest` **5**、`AuditLogControllerTest` 2、`MenuControllerTest` 3
- 全量 `.\mvnw.cmd test` 前先问用户

### 下一步

1. **v0.2**：Redis + 登出黑名单等。  
2. 或 **M1 主数据**。  

新 Flyway 从 **V17** 起。

---

## 包结构（组织）

```
domain/org/SysOrg
infrastructure/persistence/org/SysOrgMapper
application/org/OrgService
application/org/dto/SaveOrgRequest, OrgTreeResponse
web/controller/org/OrgController
```

---

## 实现约定（摘录）

- 组织表 **无** `perm_code`；接口鉴权靠 **sys_menu 按钮**（`org:list` 等），组织不参与菜单 id 授权树。  
- 用户列表仍用 `UserResponse`，勿返回 `passwordHash`。  
- 组织变更 **未** 写审计。

---

## 运行

- 组织：`OrgControllerTest`；启动 `com.velrix.VelrixApplication`；admin 需 V16 后重新登录以带上 `org:*`。

菜单 id：**2018** 组织管理页 `/system/orgs`；**2019～2022** 为 org 按钮。

---

## 参考

- `docs/M0-平台底座需求.md` — M0-F-60/61、D7  
- `docs/Velrix-SpringBoot重写需求文档.md` — M0 表 `sys_org`

---

## Suggested skills

| Skill | 何时用 |
|-------|--------|
| `learning-mentor` | 引导实现 F-61 用户 `orgId` |
| `tdd` | 扩展 `UserControllerTest` |
| `handoff` | 再交接时覆盖本文 |
