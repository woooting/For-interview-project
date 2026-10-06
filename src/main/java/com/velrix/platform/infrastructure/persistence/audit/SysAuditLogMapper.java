package com.velrix.platform.infrastructure.persistence.audit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velrix.platform.domain.audit.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}