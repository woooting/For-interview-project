package com.velrix.platform.domain.role;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")

public class SysRole {
    @TableId
    private Long id;
    private Integer seq;
    private String name;
    private String description;
    @TableField("is_administrator")
    private Boolean administrator;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
