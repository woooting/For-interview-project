package com.velrix.masterdata.application.impact;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * M1 无本地引用，M3 订单注册。
 */
@Component
public class SupplierImpactContributor implements MasterDataImpactContributor {

    @Override
    public MasterDataKind kind() {
        return MasterDataKind.SUPPLIER;
    }

    @Override
    public List<ImpactItem> contribute(long entityId) {
        // 供应商引用影响要等订单模块（M3）实现后才能实际统计
        return List.of();
    }
}
