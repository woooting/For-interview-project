CREATE TABLE md_location_capacity (
    location_id     BIGINT          NOT NULL COMMENT '库位',
    product_id      BIGINT          NOT NULL COMMENT '商品',
    max_qty         DECIMAL(18, 6)  NOT NULL COMMENT '容量上限，>0',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (location_id, product_id),
    KEY idx_md_location_capacity_product (product_id),
    CONSTRAINT fk_md_location_capacity_location
        FOREIGN KEY (location_id) REFERENCES md_location (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_md_location_capacity_product
        FOREIGN KEY (product_id) REFERENCES md_product (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库位-商品容量';
