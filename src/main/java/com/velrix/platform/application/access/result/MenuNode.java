package com.velrix.platform.application.access.result;

import java.util.List;

public record MenuNode(Long id, String name, String path, List<MenuNode> children) {}