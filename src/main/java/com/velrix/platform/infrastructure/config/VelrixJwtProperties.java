package com.velrix.platform.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "velrix.jwt")
public record VelrixJwtProperties(
        String secret,
        long accessTokenMinutes,
        long refreshTokenDays) {}
