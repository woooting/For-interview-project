package com.velrix.platform.application.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.platform.domain.role.SysRole;
import com.velrix.platform.infrastructure.persistence.role.SysRoleMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {
    private final SysRoleMapper sysRoleMapper;

    public SysRole create(String name, String description, Boolean administrator){
        if (name == null || name.isBlank()){
            throw new BizException(ApiCodes.BIZ_ERROR,"角色名不能为空");
        }
        String trimmed =  name.trim();
        SysRole existing = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getName, trimmed));
        if (existing != null){
            throw new BizException(ApiCodes.BIZ_ERROR,"该角色已经存在");
        }
        // 用实体更新数据库
        SysRole role = new SysRole();
        role.setName(trimmed);
        role.setDescription(description);
        role.setAdministrator( Boolean.TRUE.equals(administrator)); // null 要落成 false，true 才是超级管理员
        sysRoleMapper.insert(role);
        return sysRoleMapper.selectById(role.getId());
    }

    public SysRole update(Long id, String name, String description, Boolean administrator) {
        if (id == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该角色不存在");
        }
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该角色不存在");
        }
        if (name == null || name.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "角色名不能为空");
        }
        String trimmed = name.trim();
        SysRole existing = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getName, trimmed)
                        .ne(SysRole::getId, id));
        if (existing != null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该角色已经存在");
        }
        sysRoleMapper.update(null, new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, id)
                .set(SysRole::getName, trimmed)
                .set(SysRole::getDescription, description)
                .set(SysRole::getAdministrator, Boolean.TRUE.equals(administrator)));
        return sysRoleMapper.selectById(id);
    }
    public List<SysRole> list() {

        return sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSeq));
    }
}
