package com.velrix.platform.application.access.dto;

import java.util.List;

public record UserAccessResponse(List<MenuNodeResponse> menus, List<String> permCodes) {}
