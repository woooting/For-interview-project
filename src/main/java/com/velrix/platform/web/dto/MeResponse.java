package com.velrix.platform.web.dto;

import com.velrix.platform.application.MenuNode;

import java.util.List;

public record MeResponse(Long id, String username, String displayName, List<MenuNode> menus,List<String>permCodes) {
}


