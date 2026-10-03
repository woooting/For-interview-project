CREATE TABLE sys_user (
    id              BIGINT          NOT NULL COMMENT '主键（应用侧生成，如雪花 ID）',
    username        VARCHAR(64)     NOT NULL COMMENT '登录名（展示用，trim 后入库）',
    username_norm   VARCHAR(64)     NOT NULL COMMENT '登录名规范化（小写），唯一',
    password_hash   VARCHAR(100)    NOT NULL COMMENT 'BCrypt 等密码哈希',
    display_name    VARCHAR(128)    NOT NULL COMMENT '显示名，可重复',
    enabled         TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
    org_id          BIGINT          NULL COMMENT '所属组织，FK 在 sys_org 创建后补充',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username_norm (username_norm),
    KEY idx_sys_user_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';
