package com.velrix.platform.web.controller.role;

public record SaveRoleRequest(String name, String description, Boolean administrator) {}