package com.velrix.platform.web.controller.audit;

import com.velrix.platform.application.audit.AuditLogService;
import com.velrix.platform.domain.audit.SysAuditLog;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit-logs")

public class AuditLogController {
    private final AuditLogService auditLogService;

    @GetMapping
    @RequirePerm("audit:list")
    public ApiResponse<List<SysAuditLog>> getLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ApiResponse.ok(auditLogService.getLog(actor,targetType,from,to));
    }
}
