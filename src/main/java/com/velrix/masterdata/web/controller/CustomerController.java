package com.velrix.masterdata.web.controller;

import com.velrix.masterdata.application.customer.CustomerService;
import com.velrix.masterdata.application.impact.ImpactSummary;
import com.velrix.masterdata.domain.customer.MdCustomer;
import com.velrix.masterdata.web.dto.PatchStatusRequest;
import com.velrix.masterdata.web.dto.SaveCustomerRequest;
import com.velrix.masterdata.web.support.WebActor;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @RequirePerm("customer:list")
    public ApiResponse<List<MdCustomer>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(customerService.list(keyword, status));
    }

    @GetMapping("/{id}")
    @RequirePerm("customer:list")
    public ApiResponse<MdCustomer> get(@PathVariable Long id) {
        return ApiResponse.ok(customerService.get(id));
    }

    @PostMapping
    @RequirePerm("customer:create")
    public ApiResponse<MdCustomer> create(@Valid @RequestBody SaveCustomerRequest body, HttpServletRequest request) {
        return ApiResponse.ok(customerService.create(
                body.code(), body.name(), body.contactName(), body.contactPhone(), body.address(),
                WebActor.username(), WebActor.clientIp(request)));
    }

    @PutMapping("/{id}")
    @RequirePerm("customer:update")
    public ApiResponse<MdCustomer> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveCustomerRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(customerService.update(
                id, body.code(), body.name(), body.contactName(), body.contactPhone(), body.address(),
                WebActor.username(), WebActor.clientIp(request)));
    }

    @PatchMapping("/{id}/status")
    @RequirePerm("customer:status")
    public ApiResponse<MdCustomer> patchStatus(
            @PathVariable Long id,
            @Valid @RequestBody PatchStatusRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(customerService.patchStatus(
                id, body.status(), WebActor.username(), WebActor.clientIp(request)));
    }

    @GetMapping("/{id}/impact")
    @RequirePerm("customer:list")
    public ApiResponse<ImpactSummary> impact(@PathVariable Long id) {
        return ApiResponse.ok(customerService.impact(id));
    }

    @DeleteMapping("/{id}")
    @RequirePerm("customer:delete")
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }
}
