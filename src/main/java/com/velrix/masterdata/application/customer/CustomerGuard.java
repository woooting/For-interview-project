package com.velrix.masterdata.application.customer;

import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.customer.MdCustomer;
import com.velrix.masterdata.infrastructure.persistence.customer.MdCustomerMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerGuard {
    private final MdCustomerMapper mdCustomerMapper;

    public MdCustomer requireActive(long customerId){
        MdCustomer customer = mdCustomerMapper.selectById(customerId);
        if (customer == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "客户不存在");
        }
        if(!MasterDataStatus.isActive(customer.getStatus())) {
            throw new BizException(ApiCodes.BIZ_ERROR, "客户已停用");
        }
        return customer;
    }
}
