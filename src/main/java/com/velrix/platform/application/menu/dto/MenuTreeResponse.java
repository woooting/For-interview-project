package com.velrix.platform.application.menu.dto;

import java.util.List;

public record MenuTreeResponse(
        Long id,
        Long parentId,
        String name,
        String path,
        String type,
        String permCode,
        Integer sort,
        Boolean hidden,
        List<MenuTreeResponse> children) {}
