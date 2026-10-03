CREATE TABLE sys_audit_log (
    id              BIGINT          NOT NULL COMMENT '主键（应用侧生成，如雪花 ID）',
    actor           VARCHAR(64)     NOT NULL COMMENT '操作人（登录名，大小写与业务一致）',
    action          VARCHAR(64)     NOT NULL COMMENT '动作类型，如 ROLE_MENU_UPDATE',
    target_type     VARCHAR(64)     NOT NULL COMMENT '对象类型，如 ROLE',
    target_id       BIGINT          NOT NULL COMMENT '对象 id',
    summary         VARCHAR(512)    NOT NULL COMMENT '变更摘要（给人阅读）',
    occurred_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
    ip              VARCHAR(45)     NULL COMMENT '客户端 IP（IPv4/IPv6）',
    PRIMARY KEY (id),
    KEY idx_sys_audit_log_occurred (occurred_at),
    KEY idx_sys_audit_log_actor (actor),
    KEY idx_sys_audit_log_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统审计日志（只插入）';
