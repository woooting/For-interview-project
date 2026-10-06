package com.velrix.platform.application.access.result;

import java.util.List;

public record UserAccess(List<MenuNode> menus, List<String> permCodes) {}