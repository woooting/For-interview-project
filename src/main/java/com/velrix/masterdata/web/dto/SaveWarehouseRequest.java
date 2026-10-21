package com.velrix.masterdata.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveWarehouseRequest(
        @NotBlank(message = "仓库编码不能为空") String code,
        @NotBlank(message = "仓库名称不能为空") String name,
        String address) {
}
