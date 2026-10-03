CREATE TABLE sys_menu (
    id              BIGINT          NOT NULL COMMENT '主键（应用侧生成，如雪花 ID）',
    parent_id       BIGINT          NULL COMMENT '父菜单 id，根节点为 NULL',
    name            VARCHAR(64)     NOT NULL COMMENT '显示名称',
    path            VARCHAR(255)    NULL COMMENT '前端路由（MENU 常用）',
    perm_code       VARCHAR(128)    NULL COMMENT '权限码（BUTTON 必填，如 purchase-order:submit）',
    type            VARCHAR(16)     NOT NULL COMMENT 'MENU=页面入口，BUTTON=操作按钮',
    sort            INT             NOT NULL DEFAULT 0 COMMENT '同级排序，越小越靠前',
    is_hidden       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '1=隐藏',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_menu_perm_code (perm_code),
    KEY idx_sys_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单与按钮权限';
