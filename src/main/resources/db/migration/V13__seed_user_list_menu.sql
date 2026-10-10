-- 用户列表与详情按钮。管理员拥有全部菜单，自动带上 user:list。
-- 不写入 sys_role_menu：采购员、王五保持原有授权。

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2015, 2001, '用户列表', NULL, 'user:list', 'BUTTON', 4, 0);
