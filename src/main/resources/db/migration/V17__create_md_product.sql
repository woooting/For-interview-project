CREATE TABLE md_product (
    id                  BIGINT          NOT NULL COMMENT '主键（应用侧生成）',
    code                VARCHAR(64)     NOT NULL COMMENT '商品编码（展示，trim 后入库）',
    code_norm           VARCHAR(64)     NOT NULL COMMENT '编码规范化（大写），全局唯一',
    name                VARCHAR(256)    NOT NULL COMMENT '名称',
    unit                VARCHAR(32)     NOT NULL COMMENT '计量单位',
    status              VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / INACTIVE',
    max_inventory_qty   DECIMAL(18, 6)  NULL COMMENT '可选超储阈值',
    batch_managed       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否批次管理',
    serial_managed      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否序列号管理',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_product_code_norm (code_norm)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主数据-商品';
