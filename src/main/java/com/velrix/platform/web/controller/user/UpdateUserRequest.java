package com.velrix.platform.web.controller.user;

public record UpdateUserRequest(String displayName, String password, Boolean enabled) {}
