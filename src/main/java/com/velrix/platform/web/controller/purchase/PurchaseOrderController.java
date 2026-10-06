package com.velrix.platform.web.controller.purchase;


import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    @PostMapping("/submit")
    @RequirePerm("purchase-order:submit")
    public ApiResponse<Void> submit() {
        return ApiResponse.ok();
    }

}
