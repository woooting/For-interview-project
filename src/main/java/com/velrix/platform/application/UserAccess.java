package com.velrix.platform.application;

import java.util.List;

public record UserAccess(List<MenuNode> menus, List<String> permCodes) {}