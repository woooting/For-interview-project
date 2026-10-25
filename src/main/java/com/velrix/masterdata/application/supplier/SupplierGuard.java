package com.velrix.masterdata.application.supplier;

import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.supplier.MdSupplier;
import com.velrix.masterdata.infrastructure.persistence.supplier.MdSupplierMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupplierGuard {
    private final MdSupplierMapper mdSupplierMapper;

    public MdSupplier requireActive(long supplierId){
        MdSupplier supplier = mdSupplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "供应商不存在");
        }
        if(!MasterDataStatus.isActive(supplier.getStatus())) {
            throw new BizException(ApiCodes.BIZ_ERROR, "供应商已停用");
        }
        return supplier;
    }
}
