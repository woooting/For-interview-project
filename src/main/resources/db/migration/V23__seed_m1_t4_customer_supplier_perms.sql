-- M1 T4：客户/供应商管理菜单与按钮权限（接 V20 的 2200 ERP 段；2220/2230 十位区分模块）

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2220, 2200, '客户管理', '/erp/customers', NULL, 'MENU', 3, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2221, 2220, '客户列表', NULL, 'customer:list', 'BUTTON', 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2222, 2220, '新建客户', NULL, 'customer:create', 'BUTTON', 2, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2223, 2220, '编辑客户', NULL, 'customer:update', 'BUTTON', 3, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2224, 2220, '客户启停', NULL, 'customer:status', 'BUTTON', 4, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2225, 2220, '删除客户', NULL, 'customer:delete', 'BUTTON', 5, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2230, 2200, '供应商管理', '/erp/suppliers', NULL, 'MENU', 4, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2231, 2230, '供应商列表', NULL, 'supplier:list', 'BUTTON', 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2232, 2230, '新建供应商', NULL, 'supplier:create', 'BUTTON', 2, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2233, 2230, '编辑供应商', NULL, 'supplier:update', 'BUTTON', 3, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2234, 2230, '供应商启停', NULL, 'supplier:status', 'BUTTON', 4, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2235, 2230, '删除供应商', NULL, 'supplier:delete', 'BUTTON', 5, 0);
