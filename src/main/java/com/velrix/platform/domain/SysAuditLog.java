package com.velrix.platform.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId
    private Long id;
    private String actor;
    private String action;
    private String targetType;
    private Long targetId;
    private String summary;
    private LocalDateTime occurredAt;
    private String ip;
}
