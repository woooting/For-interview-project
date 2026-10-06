package com.velrix.platform.web.controller.user;

import java.util.List;

public record ReplaceUserRolesRequest(List<Long> roleIds) {}
