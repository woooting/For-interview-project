package com.velrix.masterdata.web.controller;

import com.velrix.masterdata.application.warehouse.LocationService;
import com.velrix.masterdata.domain.warehouse.MdLocation;
import com.velrix.masterdata.domain.warehouse.MdLocationCapacity;
import com.velrix.masterdata.web.dto.PatchStatusRequest;
import com.velrix.masterdata.web.dto.ReplaceCapacitiesRequest;
import com.velrix.masterdata.web.dto.SaveLocationRequest;
import com.velrix.masterdata.web.support.WebActor;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/{id}")
    @RequirePerm("warehouse:list")
    public ApiResponse<LocationService.LocationDetailView> get(@PathVariable Long id) {
        return ApiResponse.ok(locationService.getDetail(id));
    }

    @PutMapping("/{id}")
    @RequirePerm("location:manage")
    public ApiResponse<MdLocation> update(
            @PathVariable Long id,
            @RequestBody SaveLocationRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(locationService.update(
                id, body.code(), body.name(), body.description(),
                WebActor.username(), WebActor.clientIp(request)));
    }

    @PatchMapping("/{id}/status")
    @RequirePerm("location:manage")
    public ApiResponse<MdLocation> patchStatus(
            @PathVariable Long id,
            @RequestBody PatchStatusRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(locationService.patchStatus(
                id, body.status(), WebActor.username(), WebActor.clientIp(request)));
    }

    @DeleteMapping("/{id}")
    @RequirePerm("location:manage")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        locationService.delete(id, WebActor.username(), WebActor.clientIp(request));
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/capacities")
    @RequirePerm("location:manage")
    public ApiResponse<List<MdLocationCapacity>> replaceCapacities(
            @PathVariable Long id,
            @RequestBody ReplaceCapacitiesRequest body,
            HttpServletRequest request) {
        List<LocationService.CapacityLineCommand> lines = body.items() == null
                ? List.of()
                : body.items().stream()
                .map(i -> new LocationService.CapacityLineCommand(i.productId(), i.maxQty()))
                .toList();
        return ApiResponse.ok(locationService.replaceCapacities(
                id, lines, WebActor.username(), WebActor.clientIp(request)));
    }
}
