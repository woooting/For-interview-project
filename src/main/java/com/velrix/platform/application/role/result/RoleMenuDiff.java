package com.velrix.platform.application.role.result;

import java.util.Set;

public record RoleMenuDiff(Set<Long> added, Set<Long> removed) {}
