-- 菜单列表、新建、编辑、删除按钮。管理员拥有全部菜单，自动带上这些权限码。
-- 不写入 sys_role_menu：采购员、王五保持原有授权。

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2011, 2003, '菜单列表', NULL, 'menu:list', 'BUTTON', 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2012, 2003, '新建菜单', NULL, 'menu:create', 'BUTTON', 2, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2013, 2003, '编辑菜单', NULL, 'menu:update', 'BUTTON', 3, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2014, 2003, '删除菜单', NULL, 'menu:delete', 'BUTTON', 4, 0);
