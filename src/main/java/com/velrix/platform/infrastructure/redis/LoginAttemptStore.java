package com.velrix.platform.infrastructure.redis;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.velrix.platform.infrastructure.config.VelrixAuthProperties;

import java.time.Duration;


@Component
@RequiredArgsConstructor
public class LoginAttemptStore {

    private static final String KEY_PREFIX = "velrix:auth:login-fail:";
    private final StringRedisTemplate stringRedisTemplate;
    private final VelrixAuthProperties velrixAuthProperties;

    public boolean isLocked(String usernameNorm) {
        String v =  stringRedisTemplate.opsForValue().get(KEY_PREFIX + usernameNorm);
        if(v == null){
            return false;
        }
        return Integer.parseInt(v) >= velrixAuthProperties.maxLoginFailures();
    }

    public void recordFailure(String usernameNorm) {
        String key = KEY_PREFIX + usernameNorm;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(velrixAuthProperties.loginLockMinutes()));
        }
    }

    public void clearFailure(String usernameNorm){
        stringRedisTemplate.delete(KEY_PREFIX + usernameNorm);
    }
}
