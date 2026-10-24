package com.velrix.masterdata.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveSupplierRequest(
        @NotBlank(message = "供应商编码不能为空") String code,
        @NotBlank(message = "供应商名称不能为空") String name,
        String contactName,
        String contactPhone,
        String address) {
}
