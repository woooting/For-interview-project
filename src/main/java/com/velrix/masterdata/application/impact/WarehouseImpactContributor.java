package com.velrix.masterdata.application.impact;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.masterdata.domain.warehouse.MdLocation;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * M1 本地：仓库被其下库位引用（md_location 按 warehouse_id 计数）。
 */
@Component
@RequiredArgsConstructor
public class WarehouseImpactContributor implements MasterDataImpactContributor {

    private final MdLocationMapper mdLocationMapper;

    @Override
    public MasterDataKind kind() {
        return MasterDataKind.WAREHOUSE;
    }

    @Override
    public List<ImpactItem> contribute(long entityId) {
        Long count = mdLocationMapper.selectCount(
                new LambdaQueryWrapper<MdLocation>().eq(MdLocation::getWarehouseId, entityId));
        if (count == null || count == 0) {
            return List.of();
        }
        return List.of(ImpactItem.of("LOCATION", "下挂库位", count));
    }
}
