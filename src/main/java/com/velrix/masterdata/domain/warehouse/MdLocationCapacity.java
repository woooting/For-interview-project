package com.velrix.masterdata.domain.warehouse;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("md_location_capacity")
public class MdLocationCapacity {

    private Long locationId;
    private Long productId;
    private BigDecimal maxQty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
