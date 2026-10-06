package com.velrix.platform.application.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.platform.domain.user.SysUser;
import com.velrix.platform.infrastructure.persistence.role.SysRoleMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import com.velrix.platform.infrastructure.persistence.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final SysRoleMapper sysRoleMapper;

    public SysUser create(String username, String displayName, String rawPassword, Boolean enabled) {
        if (username == null || username.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "登录名不能为空");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "昵称不能为空");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "密码不能为空");
        }
        String trimmed = username.trim();
        String norm = trimmed.toLowerCase(Locale.ROOT);
        SysUser existing = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsernameNorm,norm));
        if (existing != null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该用户已经存在");
        }
        SysUser user = new SysUser();
        user.setUsername(trimmed);
        user.setUsernameNorm(norm);
        user.setDisplayName(displayName.trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEnabled(!Boolean.FALSE.equals(enabled));
        sysUserMapper.insert(user);
        return user;
    }

    public SysUser update(Long id, String displayName, String rawPassword, Boolean enabled) {
        if (id == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该用户不存在");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该用户不存在");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "昵称不能为空");
        }

        user.setDisplayName(displayName.trim());
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        sysUserMapper.updateById(user);
        return sysUserMapper.selectById(id);
    }

    public List<Long> replaceRoles(Long userId, List<Long> roleIds){
        if (userId == null || sysUserMapper.selectById(userId) == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该用户不存在");
        }
        Set<Long> newIds = (roleIds == null || roleIds.isEmpty())
                ? new HashSet<>()
                : new HashSet<>(roleIds);

        for (Long roleId : newIds) {
            if (roleId == null || sysRoleMapper.selectById(roleId) == null) {
                throw new BizException(ApiCodes.BIZ_ERROR, "此角色未创建，无法分配给当前用户");
            }
        }
            sysRoleMapper.deleteByUserId(userId);

        for (Long roleId : newIds) {
            sysRoleMapper.insertUserRole(userId, roleId);
        }

        return new ArrayList<>(newIds);
    }
}
