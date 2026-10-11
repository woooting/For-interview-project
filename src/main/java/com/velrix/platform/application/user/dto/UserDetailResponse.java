package com.velrix.platform.application.user.dto;

import java.util.List;

public record UserDetailResponse(
        Long id,
        String username,
        String displayName,
        Boolean enabled,
        Long orgId,
        List<Long> roleIds) {
}

