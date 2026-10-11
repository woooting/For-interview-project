CREATE TABLE sys_org (
    id              BIGINT          NOT NULL COMMENT '主键（应用侧生成，如雪花 ID）',
    parent_id       BIGINT          NULL COMMENT '父组织 id，根节点为 NULL',
    name            VARCHAR(64)     NOT NULL COMMENT '部门名称，同级不可重名',
    type            VARCHAR(32)     NULL COMMENT '部门分类，如 MARKET、DEV',
    sort            INT             NOT NULL DEFAULT 0 COMMENT '同级排序，越小越靠前',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_org_parent_name (parent_id, name),
    KEY idx_sys_org_parent (parent_id),
    CONSTRAINT fk_sys_org_parent FOREIGN KEY (parent_id) REFERENCES sys_org (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织（部门）树';

ALTER TABLE sys_user
    ADD CONSTRAINT fk_sys_user_org FOREIGN KEY (org_id) REFERENCES sys_org (id);
