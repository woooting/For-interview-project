package com.velrix.masterdata.application.warehouse;

import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.warehouse.MdLocation;
import com.velrix.masterdata.domain.warehouse.MdWarehouse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 「可入库」主数据侧规则：仓库 ACTIVE 且至少一个库位 ACTIVE。与 {@link WarehouseGuard} 共用。
 */
@Component
public class WarehouseReadinessPolicy {

    public boolean isInboundReady(MdWarehouse warehouse, List<MdLocation> locations) {
        if (warehouse == null || !MasterDataStatus.isActive(warehouse.getStatus())) {
            return false;
        }
        if (locations == null || locations.isEmpty()) {
            return false;
        }
        return locations.stream().anyMatch(loc -> MasterDataStatus.isActive(loc.getStatus()));
    }

    public boolean isWarehouseFrozen(MdWarehouse warehouse) {
        return warehouse != null && MasterDataStatus.INACTIVE.equals(warehouse.getStatus());
    }
}
