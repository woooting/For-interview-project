package com.velrix.platform.infrastructure.redis;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "velrix:auth:refresh-token:";
    private final StringRedisTemplate stringRedisTemplate;

    public void registerRefreshToken(String jti, Long userId, Duration ttl) {
        if(jti ==null || ttl == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + jti, String.valueOf(userId), ttl);
    }
    public  Boolean isActiveRefreshToken(String jti) {
        return stringRedisTemplate.hasKey(KEY_PREFIX + jti);
    }
    public void revoke(String jti) {
        stringRedisTemplate.delete(KEY_PREFIX + jti);
    }
}
