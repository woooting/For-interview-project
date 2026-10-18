package com.velrix.masterdata.web.dto;

import com.velrix.masterdata.domain.warehouse.MdWarehouse;

public record WarehouseListResponse(
        Long id,
        String code,
        String name,
        String address,
        String status,
        boolean inboundReady,
        boolean warehouseFrozen) {

    public static WarehouseListResponse from(MdWarehouse w, boolean inboundReady, boolean warehouseFrozen) {
        return new WarehouseListResponse(
                w.getId(), w.getCode(), w.getName(), w.getAddress(), w.getStatus(),
                inboundReady, warehouseFrozen);
    }
}
