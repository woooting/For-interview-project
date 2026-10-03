CREATE TABLE sys_role (
    id                BIGINT          NOT NULL COMMENT '主键（应用侧生成，如雪花 ID）',
    seq               INT             NOT NULL AUTO_INCREMENT COMMENT '内部自增序号',
    name              VARCHAR(64)     NOT NULL COMMENT '角色名称，业务唯一',
    description       VARCHAR(255)    NULL COMMENT '描述',
    is_administrator  TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '1=超级管理员',
    created_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_seq (seq),
    UNIQUE KEY uk_sys_role_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色';
