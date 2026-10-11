package com.velrix.platform.domain.org;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_org")
public class SysOrg {

    @TableId
    private Long id;
    private Long parentId;
    private String name;
    private String type;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
