package com.velrix.masterdata.application.support;

import com.velrix.platform.domain.audit.SysAuditLog;
import com.velrix.platform.infrastructure.persistence.audit.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MasterdataAuditWriter {

    private final SysAuditLogMapper sysAuditLogMapper;

    public void write(String actor, String action, String targetType, long targetId, String summary, String ip) {
        String trimmed = summary == null ? "" : summary.trim();
        if (trimmed.length() > 512) {
            trimmed = trimmed.substring(0, 512);
        }
        SysAuditLog log = new SysAuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setSummary(trimmed);
        log.setIp(ip);
        sysAuditLogMapper.insert(log);
    }
}
