package com.velrix.masterdata.application.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.warehouse.MdLocation;
import com.velrix.masterdata.domain.warehouse.MdWarehouse;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationMapper;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdWarehouseMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WarehouseGuard {

    private final MdWarehouseMapper mdWarehouseMapper;
    private final MdLocationMapper mdLocationMapper;
    private final WarehouseReadinessPolicy readinessPolicy;

    public void requireActiveForInbound(long warehouseId) {
        MdWarehouse warehouse = mdWarehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "仓库不存在");
        }
        if (!MasterDataStatus.isActive(warehouse.getStatus())) {
            throw new BizException(ApiCodes.BIZ_ERROR, "仓库已停用，不能入库");
        }
        List<MdLocation> locations = mdLocationMapper.selectList(
                new LambdaQueryWrapper<MdLocation>().eq(MdLocation::getWarehouseId, warehouseId));
        if (!readinessPolicy.isInboundReady(warehouse, locations)) {
            throw new BizException(ApiCodes.BIZ_ERROR, "仓库没有可用的库位，不能入库");
        }
    }
}
