package com.velrix.platform.domain;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
@Data
@TableName("sys_user")
public class SysUser {

    @TableId
    private Long id;
    private String username;
    private String usernameNorm;
    private String passwordHash;
    private String displayName;
    private Boolean enabled;
    private Long orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}