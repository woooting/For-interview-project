CREATE TABLE md_warehouse (
    id              BIGINT          NOT NULL COMMENT '主键（应用侧生成）',
    code            VARCHAR(64)     NOT NULL COMMENT '仓库编码（展示）',
    code_norm       VARCHAR(64)     NOT NULL COMMENT '编码规范化（大写），全局唯一',
    name            VARCHAR(256)    NOT NULL COMMENT '名称',
    address         VARCHAR(512)    NULL COMMENT '地址',
    status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / INACTIVE',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_warehouse_code_norm (code_norm)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主数据-仓库';

CREATE TABLE md_location (
    id              BIGINT          NOT NULL COMMENT '主键（应用侧生成）',
    warehouse_id    BIGINT          NOT NULL COMMENT '所属仓库',
    code            VARCHAR(64)     NOT NULL COMMENT '库位编码（展示）',
    code_norm       VARCHAR(64)     NOT NULL COMMENT '编码规范化（大写），同仓唯一',
    name            VARCHAR(256)    NOT NULL COMMENT '名称',
    description     VARCHAR(512)    NULL COMMENT '备注/说明',
    status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / INACTIVE',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_location_wh_code_norm (warehouse_id, code_norm),
    KEY idx_md_location_warehouse (warehouse_id),
    CONSTRAINT fk_md_location_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES md_warehouse (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主数据-库位';
