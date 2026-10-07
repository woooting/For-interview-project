package com.velrix.platform.application.menu.dto;

public record SaveMenuRequest(
        Long parentId,
        String name,
        String path,
        String type,
        Boolean hidden,
        Integer sort,
        String permCode) {}
