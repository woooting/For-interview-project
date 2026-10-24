package com.velrix.masterdata.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveCustomerRequest(
        @NotBlank(message = "客户编码不能为空") String code,
        @NotBlank(message = "客户名称不能为空") String name,
        String contactName,
        String contactPhone,
        String address) {
}
