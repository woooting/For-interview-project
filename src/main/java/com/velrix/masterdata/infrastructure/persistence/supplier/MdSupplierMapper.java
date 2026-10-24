package com.velrix.masterdata.infrastructure.persistence.supplier;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velrix.masterdata.domain.supplier.MdSupplier;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MdSupplierMapper extends BaseMapper<MdSupplier> {
}
