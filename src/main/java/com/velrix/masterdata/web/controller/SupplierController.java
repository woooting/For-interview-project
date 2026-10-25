package com.velrix.masterdata.web.controller;

import com.velrix.masterdata.application.impact.ImpactSummary;
import com.velrix.masterdata.application.supplier.SupplierService;
import com.velrix.masterdata.domain.supplier.MdSupplier;
import com.velrix.masterdata.web.dto.PatchStatusRequest;
import com.velrix.masterdata.web.dto.SaveSupplierRequest;
import com.velrix.masterdata.web.support.WebActor;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @RequirePerm("supplier:list")
    public ApiResponse<List<MdSupplier>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(supplierService.list(keyword, status));
    }

    @GetMapping("/{id}")
    @RequirePerm("supplier:list")
    public ApiResponse<MdSupplier> get(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.get(id));
    }

    @PostMapping
    @RequirePerm("supplier:create")
    public ApiResponse<MdSupplier> create(@Valid @RequestBody SaveSupplierRequest body, HttpServletRequest request) {
        return ApiResponse.ok(supplierService.create(
                body.code(), body.name(), body.contactName(), body.contactPhone(), body.address(),
                WebActor.username(), WebActor.clientIp(request)));
    }

    @PutMapping("/{id}")
    @RequirePerm("supplier:update")
    public ApiResponse<MdSupplier> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveSupplierRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(supplierService.update(
                id, body.code(), body.name(), body.contactName(), body.contactPhone(), body.address(),
                WebActor.username(), WebActor.clientIp(request)));
    }

    @PatchMapping("/{id}/status")
    @RequirePerm("supplier:status")
    public ApiResponse<MdSupplier> patchStatus(
            @PathVariable Long id,
            @Valid @RequestBody PatchStatusRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(supplierService.patchStatus(
                id, body.status(), WebActor.username(), WebActor.clientIp(request)));
    }

    @GetMapping("/{id}/impact")
    @RequirePerm("supplier:list")
    public ApiResponse<ImpactSummary> impact(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.impact(id));
    }

    @DeleteMapping("/{id}")
    @RequirePerm("supplier:delete")
    public void delete(@PathVariable Long id) {
        supplierService.delete(id);
    }
}
