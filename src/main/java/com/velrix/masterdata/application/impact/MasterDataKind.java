package com.velrix.masterdata.application.impact;

/**
 * 主数据类型（引用影响 SPI 按它注册，需求 §6.2）。
 * M1 用 PRODUCT / WAREHOUSE / LOCATION，CUSTOMER / SUPPLIER 留给 T4。
 */
public enum MasterDataKind {
    PRODUCT,
    WAREHOUSE,
    CUSTOMER,
    SUPPLIER,
    LOCATION
}
