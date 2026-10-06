package com.velrix.platform.infrastructure.persistence;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velrix.platform.domain.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}