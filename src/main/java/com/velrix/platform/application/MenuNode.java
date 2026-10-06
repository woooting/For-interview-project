package com.velrix.platform.application;

import java.util.List;

public record MenuNode(Long id, String name, String path, List<MenuNode> children) {}