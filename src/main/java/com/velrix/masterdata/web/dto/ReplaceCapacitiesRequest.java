package com.velrix.masterdata.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ReplaceCapacitiesRequest(
        @Valid List<CapacityLine> items) {

    public record CapacityLine(
            @NotNull(message = "商品不能为空") Long productId,
            @NotNull(message = "maxQty 不能为空")
            @DecimalMin(value = "0", inclusive = false, message = "maxQty 必须大于 0") BigDecimal maxQty) {
    }
}
