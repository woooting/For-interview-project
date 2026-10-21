package com.velrix.masterdata.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveLocationRequest(
        @NotBlank(message = "库位编码不能为空") String code,
        @NotBlank(message = "库位名称不能为空") String name,
        String description) {
}
