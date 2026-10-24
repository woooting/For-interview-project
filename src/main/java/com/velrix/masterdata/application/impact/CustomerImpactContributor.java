package com.velrix.masterdata.application.impact;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * M1 无本地引用，M3 订单注册。
 */
@Component
public class CustomerImpactContributor implements MasterDataImpactContributor {

    @Override
    public MasterDataKind kind() {
        return MasterDataKind.CUSTOMER;
    }

    @Override
    public List<ImpactItem> contribute(long entityId) {
        // 客户引用影响要等订单模块（M3）实现后才能实际统计
        return List.of();
    }
}
