-- M1 T3：商品启停/删除按钮权限（接 V20 的 2210 商品管理段）

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2214, 2210, '商品启停', NULL, 'product:status', 'BUTTON', 4, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2215, 2210, '删除商品', NULL, 'product:delete', 'BUTTON', 5, 0);
