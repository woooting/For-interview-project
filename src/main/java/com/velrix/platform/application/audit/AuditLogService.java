package com.velrix.platform.application.audit;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.platform.domain.audit.SysAuditLog;
import com.velrix.platform.infrastructure.persistence.audit.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final SysAuditLogMapper sysAuditLogMapper;

    public List<SysAuditLog> getLog(String actor, String targetType, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<SysAuditLog>();
        if(actor != null && !actor.trim().isBlank()){
            wrapper.eq(SysAuditLog::getActor,actor.trim());
        }
        if(targetType != null && !targetType.trim().isBlank()){
            wrapper.eq(SysAuditLog::getTargetType,targetType.trim());
        }
        if(from != null ){
            wrapper.ge(SysAuditLog::getOccurredAt,from);
        }
        if(to!=null){
            wrapper.le(SysAuditLog::getOccurredAt,to);
        }
        wrapper.orderByDesc(SysAuditLog::getOccurredAt);
        return sysAuditLogMapper.selectList(wrapper);
    }
}
