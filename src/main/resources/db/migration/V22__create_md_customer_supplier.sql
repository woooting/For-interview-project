-- M1 T4：客户/供应商主数据（骨架对标 V17 md_product；字段见需求 M1-F-20/21）
-- 联系人三件套可空：档案允许先建后补，门禁只看 status（D2 定稿：仅停用不物理删）

CREATE TABLE md_customer (
    id              BIGINT          NOT NULL COMMENT '主键（应用侧生成）',
    code            VARCHAR(64)     NOT NULL COMMENT '客户编码（展示，trim 后入库）',
    code_norm       VARCHAR(64)     NOT NULL COMMENT '编码规范化（大写），全局唯一',
    name            VARCHAR(256)    NOT NULL COMMENT '客户名称',
    status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / INACTIVE',
    contact_name    VARCHAR(64)     NULL COMMENT '联系人姓名（可选）',
    contact_phone   VARCHAR(32)     NULL COMMENT '联系电话（可选）',
    address         VARCHAR(512)    NULL COMMENT '通信地址（可选）',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_customer_code_norm (code_norm)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主数据-客户';

CREATE TABLE md_supplier (
    id              BIGINT          NOT NULL COMMENT '主键（应用侧生成）',
    code            VARCHAR(64)     NOT NULL COMMENT '供应商编码（展示，trim 后入库）',
    code_norm       VARCHAR(64)     NOT NULL COMMENT '编码规范化（大写），全局唯一',
    name            VARCHAR(256)    NOT NULL COMMENT '供应商名称',
    status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / INACTIVE',
    contact_name    VARCHAR(64)     NULL COMMENT '联系人姓名（可选）',
    contact_phone   VARCHAR(32)     NULL COMMENT '联系电话（可选）',
    address         VARCHAR(512)    NULL COMMENT '通信地址（可选）',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_supplier_code_norm (code_norm)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主数据-供应商';
