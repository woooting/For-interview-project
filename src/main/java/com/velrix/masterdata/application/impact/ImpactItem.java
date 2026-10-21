package com.velrix.masterdata.application.impact;

import java.math.BigDecimal;

/**
 * 一类引用的计数（如"库位容量配置 3 条"）。
 *
 * @param refType  引用类型标识，如 LOCATION_CAPACITY
 * @param label    中文展示名
 * @param count    引用条数
 * @param quantity 涉及数量合计（可空，M2 库存占用会用到）
 */
public record ImpactItem(String refType, String label, long count, BigDecimal quantity) {

    public static ImpactItem of(String refType, String label, long count) {
        return new ImpactItem(refType, label, count, null);
    }
}
