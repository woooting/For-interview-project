package com.velrix.platform.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velrix.platform.domain.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
