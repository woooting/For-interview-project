-- 角色授权按钮。管理员拥有全部菜单，自动带上该权限码。
-- 不写入 sys_role_menu：采购员、王五保持原有授权。

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2004, 2002, '保存授权', NULL, 'role:grant-menus', 'BUTTON', 1, 0);
