package com.velrix.masterdata.infrastructure.persistence.warehouse;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velrix.masterdata.domain.warehouse.MdWarehouse;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MdWarehouseMapper extends BaseMapper<MdWarehouse> {
}
