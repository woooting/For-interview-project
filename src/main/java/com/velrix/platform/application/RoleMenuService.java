package com.velrix.platform.application;



import com.velrix.platform.infrastructure.persistence.SysAuditLogMapper;
import com.velrix.platform.infrastructure.persistence.SysMenuMapper;
import com.velrix.platform.infrastructure.persistence.SysRoleMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.velrix.platform.domain.SysRole;
import com.velrix.platform.domain.SysAuditLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleMenuService {
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysAuditLogMapper sysAuditLogMapper;

    // 用 menuIds 全量替换该角色的菜单绑定，并返回相对变更前的增删 diff
    public RoleMenuDiff replace(Long roleId, List<Long> menuIds, String actor, String ip) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ApiCodes.BIZ_ERROR,"该角色不存在");
        }
        // 管理员隐含全部菜单，不允许改 sys_role_menu
        if (Boolean.TRUE.equals(role.getAdministrator())){
            throw new BizException(ApiCodes.BIZ_ERROR,"管理员已经拥有全部菜单");
        }
        // null/空列表视为清空该角色所有菜单
        Set<Long> newIds = (menuIds == null || menuIds.isEmpty())
                ? new HashSet<>()
                : new HashSet<>(menuIds);

        Set<Long> oldIds = new HashSet<>(sysMenuMapper.selectMenuIdsByRoleId(roleId));

        // 新增的：newIds 有、oldIds 没有
        Set<Long> added = new HashSet<>(newIds);
        added.removeAll(oldIds);
        // 删除的：oldIds 有、newIds 没有
        Set<Long> removed = new HashSet<>(oldIds);
        removed.removeAll(newIds);

        if (actor == null || actor.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR,"缺少操作人名称");
        }
        // 先删后插，保证结果集与 newIds 一致
        sysMenuMapper.deleteByRoleId(roleId);

        for (Long id : newIds) {
            sysMenuMapper.insertRoleMenu(roleId, id);
        }
        saveGrantAudit(role, added, removed, actor, ip);
        return new RoleMenuDiff(added, removed);
    }

    // 写入 sys_audit_log（只插入，summary 对齐 varchar(512)）
    private void saveGrantAudit(SysRole role, Set<Long> added, Set<Long> removed, String actor, String ip) {
        String summary = "角色「" + role.getName() + "」新增 " + added + "，移除 " + removed;
        if (summary.length() > 512) {
            summary = summary.substring(0, 512);
        }
        SysAuditLog log = new SysAuditLog();
        log.setActor(actor);
        log.setAction("ROLE_MENU_UPDATE");
        log.setTargetType("ROLE");
        log.setTargetId(role.getId());
        log.setSummary(summary);
        log.setIp(ip);
        sysAuditLogMapper.insert(log);
    }

}
