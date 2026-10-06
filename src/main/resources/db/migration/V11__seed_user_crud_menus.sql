-- 用户创建、编辑、分配角色按钮。管理员拥有全部菜单，自动带上这些权限码。
-- 不写入 sys_role_menu：采购员、王五保持原有授权。

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2008, 2001, '新建用户', NULL, 'user:create', 'BUTTON', 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2009, 2001, '编辑用户', NULL, 'user:update', 'BUTTON', 2, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2010, 2001, '分配角色', NULL, 'user:assign-roles', 'BUTTON', 3, 0);
