package com.velrix.platform.application.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.platform.domain.user.SysUser;
import com.velrix.platform.infrastructure.persistence.user.SysUserMapper;
import com.velrix.platform.infrastructure.security.JwtService;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String login(String username, String rawPassword) {
        String norm = username.trim().toLowerCase(Locale.ROOT);
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsernameNorm, norm));
        return authenticate(user, rawPassword);
    }

    public String login(Long id, String rawPassword) {
        SysUser user = sysUserMapper.selectById(id);
        return authenticate(user, rawPassword);
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
        return jwtService.issue(user);
    }

}