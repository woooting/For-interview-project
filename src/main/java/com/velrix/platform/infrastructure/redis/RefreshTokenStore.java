package com.velrix.platform.infrastructure.redis;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "velrix:auth:refresh-token:";
    private final StringRedisTemplate stringRedisTemplate;

    public void registerRefreshToken(String jti, Long userId, Duration ttl) {
        if (!StringUtils.hasText(jti) || userId == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + jti, String.valueOf(userId), ttl);
    }

    public boolean isActiveRefreshToken(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + jti));
    }
    public void revoke(String jti) {
        stringRedisTemplate.delete(KEY_PREFIX + jti);
    }
}
