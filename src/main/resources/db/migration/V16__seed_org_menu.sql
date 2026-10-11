-- 组织管理：页面 + org:list/create/update/delete 按钮。管理员 is_administrator=1 自动拥有全部权限码。
-- 不写入 sys_role_menu：采购员等保持原有授权。

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2018, 2000, '组织管理', '/system/orgs', NULL, 'MENU', 5, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2019, 2018, '组织列表', NULL, 'org:list', 'BUTTON', 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2020, 2018, '新建组织', NULL, 'org:create', 'BUTTON', 2, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2021, 2018, '编辑组织', NULL, 'org:update', 'BUTTON', 3, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2022, 2018, '删除组织', NULL, 'org:delete', 'BUTTON', 4, 0);
