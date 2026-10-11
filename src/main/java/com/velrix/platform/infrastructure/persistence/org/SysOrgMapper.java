package com.velrix.platform.infrastructure.persistence.org;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velrix.platform.domain.org.SysOrg;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {

}
