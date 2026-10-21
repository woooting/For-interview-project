package com.velrix.masterdata.application.product;

import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.product.MdProduct;
import com.velrix.masterdata.infrastructure.persistence.product.MdProductMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 跨模块门禁（需求 §3.4 M1-F-30）：其他模块引用商品前先过这里。
 */
@Component
@RequiredArgsConstructor
public class ProductGuard {

    private final MdProductMapper mdProductMapper;

    public MdProduct requireActive(long productId) {
        MdProduct product = mdProductMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品不存在");
        }
        if (!MasterDataStatus.isActive(product.getStatus())) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品已停用");
        }
        return product;
    }
}
