package com.velrix.masterdata.infrastructure.persistence.customer;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velrix.masterdata.domain.customer.MdCustomer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MdCustomerMapper extends BaseMapper<MdCustomer> {
}
