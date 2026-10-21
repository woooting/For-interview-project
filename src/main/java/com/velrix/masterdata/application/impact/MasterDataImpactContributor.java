package com.velrix.masterdata.application.impact;

import java.util.List;

/**
 * 引用影响贡献者 SPI（需求 §6.2）：各业务模块按 {@link MasterDataKind} 注册，
 * 报告某条主数据被本模块引用的情况。count 为 0 的项不返回。
 * <p>
 * M1 本地：LOCATION_CAPACITY（商品）、LOCATION（仓库）；<br>
 * M2 注册 INVENTORY_TXN / ON_HAND；M3 注册 PURCHASE_ORDER / SALES_ORDER。
 */
public interface MasterDataImpactContributor {

    MasterDataKind kind();

    List<ImpactItem> contribute(long entityId);
}
