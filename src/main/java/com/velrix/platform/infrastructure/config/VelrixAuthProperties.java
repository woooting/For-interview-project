package com.velrix.platform.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "velrix.auth")
public record VelrixAuthProperties(
        int maxLoginFailures,
        int loginLockMinutes) {}
