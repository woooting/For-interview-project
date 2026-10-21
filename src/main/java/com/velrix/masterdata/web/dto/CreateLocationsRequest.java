package com.velrix.masterdata.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateLocationsRequest(
        @NotEmpty(message = "库位列表不能为空") @Valid List<LocationLine> locations) {

    public record LocationLine(
            @NotBlank(message = "编码不能为空") String code,
            @NotBlank(message = "名称不能为空") String name,
            String description) {
    }
}
