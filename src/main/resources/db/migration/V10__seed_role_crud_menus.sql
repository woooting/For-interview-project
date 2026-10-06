-- 角色创建、编辑、列表按钮。管理员拥有全部菜单，自动带上这些权限码。
-- 不写入 sys_role_menu：采购员、王五保持原有授权。

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2005, 2002, '新建角色', NULL, 'role:create', 'BUTTON', 2, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2006, 2002, '编辑角色', NULL, 'role:update', 'BUTTON', 3, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2007, 2002, '角色列表', NULL, 'role:list', 'BUTTON', 4, 0);
