package com.velrix.platform.domain;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String permCode;
    private String type;
    private Integer sort;
    @TableField("is_hidden")
    private Boolean hidden;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
