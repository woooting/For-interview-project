-- M1 主数据：ERP 菜单与按钮（ID 2200+，避免与 2000/2100 段冲突）

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2200, NULL, 'ERP', '/erp', NULL, 'MENU', 30, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2201, 2200, '仓库管理', '/erp/warehouses', NULL, 'MENU', 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2202, 2201, '仓库列表', NULL, 'warehouse:list', 'BUTTON', 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2203, 2201, '新建仓库', NULL, 'warehouse:create', 'BUTTON', 2, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2204, 2201, '编辑仓库', NULL, 'warehouse:update', 'BUTTON', 3, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2205, 2201, '仓库启停', NULL, 'warehouse:status', 'BUTTON', 4, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2206, 2201, '库位与容量', NULL, 'location:manage', 'BUTTON', 5, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2210, 2200, '商品管理', '/erp/products', NULL, 'MENU', 2, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2211, 2210, '商品列表', NULL, 'product:list', 'BUTTON', 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2212, 2210, '新建商品', NULL, 'product:create', 'BUTTON', 2, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2213, 2210, '编辑商品', NULL, 'product:update', 'BUTTON', 3, 0);
