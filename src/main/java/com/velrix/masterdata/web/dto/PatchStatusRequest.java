package com.velrix.masterdata.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PatchStatusRequest(
        @NotBlank(message = "状态不能为空") String status) {
}
