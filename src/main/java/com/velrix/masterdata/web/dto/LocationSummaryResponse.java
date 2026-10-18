package com.velrix.masterdata.web.dto;

import com.velrix.masterdata.domain.warehouse.MdLocation;

public record LocationSummaryResponse(
        Long id,
        Long warehouseId,
        String code,
        String name,
        String description,
        String status,
        String storageLabel,
        boolean warehouseFrozen) {

    public static LocationSummaryResponse from(MdLocation loc, String storageLabel, boolean warehouseFrozen) {
        return new LocationSummaryResponse(
                loc.getId(),
                loc.getWarehouseId(),
                loc.getCode(),
                loc.getName(),
                loc.getDescription(),
                loc.getStatus(),
                storageLabel,
                warehouseFrozen);
    }
}
