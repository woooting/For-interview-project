package com.velrix.masterdata.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReplaceCapacitiesRequest(List<CapacityLine> items) {

    public record CapacityLine(Long productId, BigDecimal maxQty) {
    }
}
