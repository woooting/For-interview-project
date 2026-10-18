package com.velrix.masterdata.domain.warehouse;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("md_warehouse")
public class MdWarehouse {

    @TableId
    private Long id;
    private String code;
    private String codeNorm;
    private String name;
    private String address;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
