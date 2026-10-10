-- 审计日志查询：页面 + audit:list 按钮。管理员拥有全部菜单，自动带上权限码。
-- 不写入 sys_role_menu。

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2016, 2000, '审计日志', '/system/audit-logs', NULL, 'MENU', 4, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2017, 2016, '审计列表', NULL, 'audit:list', 'BUTTON', 1, 0);
