package com.velrix.masterdata.infrastructure.persistence.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velrix.masterdata.domain.product.MdProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MdProductMapper extends BaseMapper<MdProduct> {
}
