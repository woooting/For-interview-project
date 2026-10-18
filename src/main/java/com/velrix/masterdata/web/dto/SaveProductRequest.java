package com.velrix.masterdata.web.dto;

public record SaveProductRequest(
        String code,
        String name,
        String unit,
        Boolean batchManaged,
        Boolean serialManaged) {
}
