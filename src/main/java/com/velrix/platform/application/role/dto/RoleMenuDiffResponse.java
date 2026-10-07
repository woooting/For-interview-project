package com.velrix.platform.application.role.dto;

import java.util.Set;

public record RoleMenuDiffResponse(Set<Long> added, Set<Long> removed) {}
