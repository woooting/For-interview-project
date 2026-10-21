package com.velrix.masterdata.application.warehouse;

import com.velrix.masterdata.domain.warehouse.MdLocation;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 跨模块门禁（需求 §3.4）：其他模块引用库位前先过这里。
 */
@Component
@RequiredArgsConstructor
public class LocationGuard {

    private final MdLocationMapper mdLocationMapper;

    /**
     * 校验库位存在且属于指定仓库（M2 移动/库存按 warehouseId + locationId 引用时用）。
     */
    public MdLocation requireBelongsToWarehouse(long locationId, long warehouseId) {
        MdLocation location = mdLocationMapper.selectById(locationId);
        if (location == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "库位不存在");
        }
        if (!Long.valueOf(warehouseId).equals(location.getWarehouseId())) {
            throw new BizException(ApiCodes.BIZ_ERROR, "库位不属于该仓库");
        }
        return location;
    }
}
