-- M0 平台种子数据（开发/演示）
-- 默认密码均为 admin123（BCrypt）
-- 管理员：admin / admin123
-- 采购员演示：lisi / admin123

SET @pwd_hash = '$2b$10$X/u0LuDP50ZAnD3ptGj.qO0cGUMK1VJ09j36E4rK2Vllf1/BH5qHu';

-- ---------- 角色 ----------
INSERT INTO sys_role (id, name, description, is_administrator)
VALUES (1, '系统管理员', '拥有全部菜单与权限', 1);

INSERT INTO sys_role (id, name, description, is_administrator)
VALUES (2, '采购员', 'M0 验收演示：仅采购相关菜单与提交权限', 0);

-- ---------- 用户 ----------
INSERT INTO sys_user (id, username, username_norm, password_hash, display_name, enabled, org_id)
VALUES (1001, 'admin', 'admin', @pwd_hash, '系统管理员', 1, NULL);

INSERT INTO sys_user (id, username, username_norm, password_hash, display_name, enabled, org_id)
VALUES (1002, 'lisi', 'lisi', @pwd_hash, '李四', 1, NULL);

-- ---------- 用户-角色 ----------
INSERT INTO sys_user_role (user_id, role_id) VALUES (1001, 1);
INSERT INTO sys_user_role (user_id, role_id) VALUES (1002, 2);

-- ---------- 菜单树（系统管理 + 采购演示）----------
INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2000, NULL, '系统管理', '/system', NULL, 'MENU', 10, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2001, 2000, '用户管理', '/system/users', NULL, 'MENU', 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2002, 2000, '角色管理', '/system/roles', NULL, 'MENU', 2, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2003, 2000, '菜单管理', '/system/menus', NULL, 'MENU', 3, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2100, NULL, '采购', '/purchase', NULL, 'MENU', 20, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2101, 2100, '采购订单', '/purchase/orders', NULL, 'MENU', 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, perm_code, type, sort, is_hidden)
VALUES (2102, 2101, '提交订单', NULL, 'purchase-order:submit', 'BUTTON', 1, 0);

-- ---------- 采购员角色授权（覆盖式授权的数据态）----------
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 2100);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 2101);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 2102);

-- 系统管理员 is_administrator=1，无需写入 sys_role_menu
