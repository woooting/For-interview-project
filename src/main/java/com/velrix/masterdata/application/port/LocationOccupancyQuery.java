package com.velrix.masterdata.application.port;

import java.math.BigDecimal;
import java.util.Map;

/**
 * M2 库存模块实现：按库位返回各商品剩余可放数量。M1 使用 stub，联调后替换。
 */
public interface LocationOccupancyQuery {

    /**
     * @return productId → 剩余可放数量；无 M2 数据时可返回空 Map
     */
    Map<Long, BigDecimal> remainingQtyByProduct(long locationId);
}
