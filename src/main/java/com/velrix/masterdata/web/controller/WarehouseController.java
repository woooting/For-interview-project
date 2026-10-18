package com.velrix.masterdata.web.controller;

import com.velrix.masterdata.application.warehouse.LocationService;
import com.velrix.masterdata.application.warehouse.LocationStorageSummaryAssembler;
import com.velrix.masterdata.application.warehouse.WarehouseReadinessPolicy;
import com.velrix.masterdata.application.warehouse.WarehouseService;
import com.velrix.masterdata.domain.warehouse.MdWarehouse;
import com.velrix.masterdata.web.dto.*;
import com.velrix.masterdata.web.support.WebActor;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final LocationService locationService;
    private final WarehouseReadinessPolicy readinessPolicy;

    @GetMapping
    @RequirePerm("warehouse:list")
    public ApiResponse<List<WarehouseListResponse>> list() {
        List<WarehouseListResponse> rows = warehouseService.list().stream()
                .map(item -> WarehouseListResponse.from(
                        item.warehouse(), item.inboundReady(), item.warehouseFrozen()))
                .toList();
        return ApiResponse.ok(rows);
    }

    @GetMapping("/{id}")
    @RequirePerm("warehouse:list")
    public ApiResponse<MdWarehouse> get(@PathVariable Long id) {
        return ApiResponse.ok(warehouseService.get(id));
    }

    @GetMapping("/{id}/locations")
    @RequirePerm("warehouse:list")
    public ApiResponse<List<LocationSummaryResponse>> listLocations(@PathVariable Long id) {
        return ApiResponse.ok(locationService.listByWarehouse(id).stream()
                .map(v -> LocationSummaryResponse.from(v.location(), v.storageLabel(), v.warehouseFrozen()))
                .toList());
    }

    @PostMapping
    @RequirePerm("warehouse:create")
    public ApiResponse<MdWarehouse> create(@RequestBody SaveWarehouseRequest body, HttpServletRequest request) {
        return ApiResponse.ok(warehouseService.create(
                body.code(), body.name(), body.address(),
                WebActor.username(), WebActor.clientIp(request)));
    }

    @PutMapping("/{id}")
    @RequirePerm("warehouse:update")
    public ApiResponse<MdWarehouse> update(
            @PathVariable Long id,
            @RequestBody SaveWarehouseRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(warehouseService.update(
                id, body.code(), body.name(), body.address(),
                WebActor.username(), WebActor.clientIp(request)));
    }

    @PatchMapping("/{id}/status")
    @RequirePerm("warehouse:status")
    public ApiResponse<MdWarehouse> patchStatus(
            @PathVariable Long id,
            @RequestBody PatchStatusRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(warehouseService.patchStatus(
                id, body.status(), WebActor.username(), WebActor.clientIp(request)));
    }

    @PostMapping("/{id}/locations")
    @RequirePerm("location:manage")
    public ApiResponse<List<LocationSummaryResponse>> createLocations(
            @PathVariable Long id,
            @RequestBody CreateLocationsRequest body,
            HttpServletRequest request) {
        List<LocationService.CreateLocationCommand> commands = body.locations() == null
                ? List.of()
                : body.locations().stream()
                .map(l -> new LocationService.CreateLocationCommand(l.code(), l.name(), l.description()))
                .toList();
        var created = locationService.createBatch(id, commands, WebActor.username(), WebActor.clientIp(request));
        MdWarehouse warehouse = warehouseService.get(id);
        boolean frozen = readinessPolicy.isWarehouseFrozen(warehouse);
        return ApiResponse.ok(created.stream()
                .map(loc -> LocationSummaryResponse.from(
                        loc, LocationStorageSummaryAssembler.LABEL_NO_CAPACITY, frozen))
                .toList());
    }
}
