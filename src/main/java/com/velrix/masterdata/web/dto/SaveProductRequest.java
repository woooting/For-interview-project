package com.velrix.masterdata.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveProductRequest(
        @NotBlank(message = "商品编码不能为空") String code,
        @NotBlank(message = "商品名称不能为空") String name,
        @NotBlank(message = "计量单位不能为空") String unit,
        Boolean batchManaged,
        Boolean serialManaged) {
}
