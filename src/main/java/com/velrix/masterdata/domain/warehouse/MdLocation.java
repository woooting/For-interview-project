package com.velrix.masterdata.domain.warehouse;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("md_location")
public class MdLocation {

    @TableId
    private Long id;
    private Long warehouseId;
    private String code;
    private String codeNorm;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
