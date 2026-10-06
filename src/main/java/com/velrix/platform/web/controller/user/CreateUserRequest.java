package com.velrix.platform.web.controller.user;

public record CreateUserRequest(String username, String displayName, String password, Boolean enabled) {}
