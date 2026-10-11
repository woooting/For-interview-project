package com.velrix.platform.application.org.dto;

public record SaveOrgRequest(
        Long parentId,
        String name,
        Integer sort,
        String type) {}
