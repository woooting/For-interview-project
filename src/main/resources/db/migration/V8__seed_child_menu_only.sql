-- 仅授权子菜单的演示账号，用来验收祖先补齐（AC-M0-05）
-- 只写入「采购订单」2101，不写父菜单 2100，也不写按钮 2102
-- 账号：wangwu / admin123

SET @pwd_hash = '$2b$10$X/u0LuDP50ZAnD3ptGj.qO0cGUMK1VJ09j36E4rK2Vllf1/BH5qHu';

INSERT INTO sys_role (id, name, description, is_administrator)
VALUES (3, '仅采购订单', '只授权子菜单「采购订单」，父菜单靠接口补齐', 0);

INSERT INTO sys_user (id, username, username_norm, password_hash, display_name, enabled, org_id)
VALUES (1003, 'wangwu', 'wangwu', @pwd_hash, '王五', 1, NULL);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1003, 3);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (3, 2101);
