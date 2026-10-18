package com.velrix.masterdata.application.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.masterdata.domain.warehouse.MdLocationCapacity;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationCapacityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** M2 入库前查容量上限。 */
@Component
@RequiredArgsConstructor
public class LocationCapacityQuery {

    private final MdLocationCapacityMapper mdLocationCapacityMapper;

    public BigDecimal getMaxQty(long locationId, long productId) {
        MdLocationCapacity row = mdLocationCapacityMapper.selectOne(new LambdaQueryWrapper<MdLocationCapacity>()
                .eq(MdLocationCapacity::getLocationId, locationId)
                .eq(MdLocationCapacity::getProductId, productId));
        return row == null ? null : row.getMaxQty();
    }
}
