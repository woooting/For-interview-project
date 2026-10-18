package com.velrix.masterdata.application.warehouse;

import com.velrix.masterdata.application.port.LocationOccupancyQuery;
import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.warehouse.MdLocation;
import com.velrix.masterdata.domain.warehouse.MdLocationCapacity;
import com.velrix.masterdata.domain.warehouse.MdWarehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * M2 出数 + M1 拼展示标签（可存 / 部分可存 / 已满 / 停用 / 随仓冻结）。
 */
@Component
@RequiredArgsConstructor
public class LocationStorageSummaryAssembler {

    public static final String LABEL_FROZEN_WAREHOUSE = "随仓冻结";
    public static final String LABEL_LOCATION_INACTIVE = "库位停用";
    public static final String LABEL_STORABLE = "可存";
    public static final String LABEL_PARTIAL = "部分可存";
    public static final String LABEL_FULL = "已满";
    public static final String LABEL_NO_CAPACITY = "未配置容量";

    private final LocationOccupancyQuery locationOccupancyQuery;

    public String summarize(MdWarehouse warehouse, MdLocation location, List<MdLocationCapacity> capacities) {
        if (warehouse != null && MasterDataStatus.INACTIVE.equals(warehouse.getStatus())) {
            return LABEL_FROZEN_WAREHOUSE;
        }
        if (!MasterDataStatus.isActive(location.getStatus())) {
            return LABEL_LOCATION_INACTIVE;
        }
        if (capacities == null || capacities.isEmpty()) {
            return LABEL_NO_CAPACITY;
        }
        Map<Long, BigDecimal> remaining = locationOccupancyQuery.remainingQtyByProduct(location.getId());
        if (remaining.isEmpty()) {
            return LABEL_STORABLE;
        }
        boolean anySpace = false;
        boolean anyFull = false;
        for (MdLocationCapacity cap : capacities) {
            BigDecimal max = cap.getMaxQty();
            BigDecimal left = remaining.get(cap.getProductId());
            if (left == null) {
                anySpace = true;
                continue;
            }
            if (left.compareTo(BigDecimal.ZERO) > 0) {
                anySpace = true;
            }
            if (max != null && left.compareTo(BigDecimal.ZERO) <= 0) {
                anyFull = true;
            }
        }
        if (anySpace && anyFull) {
            return LABEL_PARTIAL;
        }
        if (anyFull && !anySpace) {
            return LABEL_FULL;
        }
        return LABEL_STORABLE;
    }
}
