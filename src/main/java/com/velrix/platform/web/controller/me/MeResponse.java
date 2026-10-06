package com.velrix.platform.web.controller.me;

import com.velrix.platform.application.access.result.MenuNode;

import java.util.List;

public record MeResponse(Long id, String username, String displayName, List<MenuNode> menus,List<String>permCodes) {
}


