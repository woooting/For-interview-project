package com.velrix.platform.application.access.dto;

import java.util.List;

public record MenuNodeResponse(Long id, String name, String path, List<MenuNodeResponse> children) {}
