package com.velrix.masterdata.domain.product;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("md_product")
public class MdProduct {

    @TableId
    private Long id;
    private String code;
    private String codeNorm;
    private String name;
    private String unit;
    private String status;
    private BigDecimal maxInventoryQty;
    private Boolean batchManaged;
    private Boolean serialManaged;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
