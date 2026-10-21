package com.velrix.masterdata.web.controller;

import com.velrix.masterdata.application.impact.ImpactSummary;
import com.velrix.masterdata.application.product.ProductService;
import com.velrix.masterdata.domain.product.MdProduct;
import com.velrix.masterdata.web.dto.PatchStatusRequest;
import com.velrix.masterdata.web.dto.SaveProductRequest;
import com.velrix.masterdata.web.support.WebActor;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @RequirePerm("product:list")
    public ApiResponse<List<MdProduct>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(productService.list(keyword, status));
    }

    @GetMapping("/{id}")
    @RequirePerm("product:list")
    public ApiResponse<MdProduct> get(@PathVariable Long id) {
        return ApiResponse.ok(productService.get(id));
    }

    @PostMapping
    @RequirePerm("product:create")
    public ApiResponse<MdProduct> create(@Valid @RequestBody SaveProductRequest body, HttpServletRequest request) {
        return ApiResponse.ok(productService.create(
                body.code(), body.name(), body.unit(), body.batchManaged(), body.serialManaged(),
                WebActor.username(), WebActor.clientIp(request)));
    }

    @PutMapping("/{id}")
    @RequirePerm("product:update")
    public ApiResponse<MdProduct> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveProductRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(productService.update(
                id, body.code(), body.name(), body.unit(), body.batchManaged(), body.serialManaged(),
                WebActor.username(), WebActor.clientIp(request)));
    }

    @PatchMapping("/{id}/status")
    @RequirePerm("product:status")
    public ApiResponse<MdProduct> patchStatus(
            @PathVariable Long id,
            @Valid @RequestBody PatchStatusRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(productService.patchStatus(
                id, body.status(), WebActor.username(), WebActor.clientIp(request)));
    }

    @GetMapping("/{id}/impact")
    @RequirePerm("product:list")
    public ApiResponse<ImpactSummary> impact(@PathVariable Long id) {
        return ApiResponse.ok(productService.impact(id));
    }

    @DeleteMapping("/{id}")
    @RequirePerm("product:delete")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
