package com.velrix.platform.application.org.dto;

import java.util.List;

public record OrgTreeResponse(
        Long id,
        Long parentId,
        String name,
        String type,
        Integer sort,
        List<OrgTreeResponse> children) {}
