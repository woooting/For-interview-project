package com.velrix.platform.application;

import java.util.Set;

public record RoleMenuDiff(Set<Long> added, Set<Long> removed) {}
