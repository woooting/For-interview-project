package com.velrix.platform.application.access;


import com.velrix.platform.infrastructure.persistence.menu.SysMenuMapper;
import com.velrix.platform.infrastructure.persistence.role.SysRoleMapper;
import com.velrix.platform.infrastructure.redis.UserAccessCacheStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


@RequiredArgsConstructor
@Service

public class UserAccessCacheInvalidator {
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMapper sysRoleMapper;
    private final UserAccessCacheStore userAccessCacheStore;

    public List<Long> resolveUserIdsByMenuId(Long menuId) {
        if (menuId == null) {
            return Collections.emptyList();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        List<Long> roleIds = sysMenuMapper.selectRoleIdsByMenuid(menuId);
        if (roleIds != null && !roleIds.isEmpty()) {
            List<Long> linked = sysRoleMapper.selectUserIdsByRoleIds(roleIds);
            if (linked != null) {
                userIds.addAll(linked);
            }
        }
        List<Long> administrators = sysRoleMapper.selectUserIdsWithAdministratorRole();
        if (administrators != null) {
            userIds.addAll(administrators);
        }
        return new ArrayList<>(userIds);
    }

    public void invalidateByMenuId(Long menuId) {
        if (menuId == null) {
            return;
        }
        List<Long> userIds = resolveUserIdsByMenuId(menuId);
        if (userIds.isEmpty()) {
            return;
        }
        userAccessCacheStore.evictAll(userIds);
    }

    public void invalidateByRoleId(Long roleId){
        if(roleId == null){return;}
        List<Long> userIds = sysRoleMapper.selectUserIdsByRoleId(roleId);
        userAccessCacheStore.evictAll(userIds);
    }

    public void invalidateUser(Long userId){
        userAccessCacheStore.evict(userId);
    }

}
