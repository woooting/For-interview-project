package com.velrix.masterdata.application.impact;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.masterdata.domain.warehouse.MdLocationCapacity;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationCapacityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * M1 本地：商品被库位容量配置引用（md_location_capacity 按 product_id 计数）。
 * M2 的库存、M3 的单据引用由各自模块注册 Contributor。
 */
@Component
@RequiredArgsConstructor
public class ProductImpactContributor implements MasterDataImpactContributor {

    private final MdLocationCapacityMapper mdLocationCapacityMapper;

    @Override
    public MasterDataKind kind() {
        return MasterDataKind.PRODUCT;
    }

    @Override
    public List<ImpactItem> contribute(long entityId) {
        Long count = mdLocationCapacityMapper.selectCount(
                new LambdaQueryWrapper<MdLocationCapacity>().eq(MdLocationCapacity::getProductId, entityId));
        if (count == null || count == 0) {
            return List.of();
        }
        return List.of(ImpactItem.of("LOCATION_CAPACITY", "库位容量配置", count));
    }
}
