package com.velrix.platform.application.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.platform.domain.user.SysUser;
import com.velrix.platform.infrastructure.persistence.user.SysUserMapper;
import com.velrix.platform.infrastructure.redis.LoginAttemptStore;
import com.velrix.platform.infrastructure.redis.TokenBlacklistStore;
import com.velrix.platform.infrastructure.security.JwtService;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistStore tokenBlacklistStore;
    private final LoginAttemptStore loginAttemptStore;

    public String login(String username, String rawPassword) {
        String norm = username.trim().toLowerCase(Locale.ROOT);
        if(loginAttemptStore.isLocked(norm)){
            throw new BizException(ApiCodes.BIZ_ERROR,"账号已锁定，请稍后重试");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsernameNorm, norm));
        try {
            String token = authenticate(user, rawPassword);
            loginAttemptStore.clearFailure(norm);
            return token;
        }catch (BizException e){
            if (ApiCodes.BIZ_LOGIN_FAILED.equals(e.getCode())) {
                loginAttemptStore.recordFailure(norm);
            }
            throw e;
        }

    }

    public String login(Long id, String rawPassword) {
        SysUser user = sysUserMapper.selectById(id);
        return authenticate(user, rawPassword);
    }

    public void logout(String accessToken) {
        try {
            Claims claims = jwtService.parse(accessToken);
            if (!JwtService.TYP_ACCESS.equals(claims.get(JwtService.CLAIM_TYP, String.class))) {
                return;
            }
            String jti = claims.getId();
            if (!StringUtils.hasText(jti) || tokenBlacklistStore.isBlacklisted(jti)) {
                return;
            }
            tokenBlacklistStore.blacklist(jti, Duration.ofSeconds(jwtService.remainingTtlSeconds(claims)));
        } catch (JwtException e) {
            throw new BizException(ApiCodes.UNAUTHORIZED, "未登录或登录已过期");
        }
    }

    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }

    /** 公共校验 + 签发 token */
    private String authenticate(SysUser user, String rawPassword) {
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BizException(ApiCodes.BIZ_LOGIN_FAILED, "用户名或密码输入错误");
        }
        return jwtService.issueAccess(user);
    }

}
