package com.velrix.platform.infrastructure.security;

import com.velrix.platform.domain.user.SysUser;
import com.velrix.platform.infrastructure.config.VelrixJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String CLAIM_TYP = "typ";
    public static final String TYP_ACCESS = "access";
    public static final String TYP_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    public JwtService(VelrixJwtProperties jwtProperties) {
        key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = jwtProperties.accessTokenMinutes();
        this.refreshTokenDays = jwtProperties.refreshTokenDays();
    }

    public String issueAccess(SysUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim("username", user.getUsername())
                .claim(CLAIM_TYP, TYP_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String issueRefresh(SysUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim("username", user.getUsername())
                .claim(CLAIM_TYP, TYP_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenDays, ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Token 剩余有效秒数，已过期返回 0（登出写 Redis 黑名单 TTL 用） */
    public long remainingTtlSeconds(Claims claims) {
        if (claims.getExpiration() == null) {
            return 0;
        }
        long seconds = claims.getExpiration().toInstant().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(seconds, 0);
    }
}
