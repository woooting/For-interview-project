package com.velrix.platform.web.controller.me;

import com.velrix.platform.application.access.dto.MenuNodeResponse;

import java.util.List;

public record MeResponse(Long id, String username, String displayName, List<MenuNodeResponse> menus,List<String>permCodes) {
}


