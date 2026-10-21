package com.velrix.masterdata.application.warehouse;

import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.warehouse.MdLocation;
import com.velrix.masterdata.domain.warehouse.MdWarehouse;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationMapper;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdWarehouseMapper;
import com.velrix.shared.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * AC-M1-07/08：入库前置校验——仓库须启用且有可用库位。
 */
@ExtendWith(MockitoExtension.class)
class WarehouseGuardTest {

	@Mock
	private MdWarehouseMapper mdWarehouseMapper;

	@Mock
	private MdLocationMapper mdLocationMapper;

	@Mock
	private WarehouseReadinessPolicy readinessPolicy;

	@InjectMocks
	private WarehouseGuard warehouseGuard;

	@Test
	void missingWarehouse_throws() {
		when(mdWarehouseMapper.selectById(9L)).thenReturn(null);

		BizException e = assertThrows(BizException.class,
				() -> warehouseGuard.requireActiveForInbound(9L));
		assertEquals("仓库不存在", e.getMessage());
	}

	@Test
	void frozenWarehouse_throws() {
		MdWarehouse warehouse = new MdWarehouse();
		warehouse.setStatus(MasterDataStatus.INACTIVE);
		when(mdWarehouseMapper.selectById(1L)).thenReturn(warehouse);

		BizException e = assertThrows(BizException.class,
				() -> warehouseGuard.requireActiveForInbound(1L));
		assertEquals("仓库已停用，不能入库", e.getMessage());
	}

	@Test
	void activeWarehouseWithoutUsableLocations_throws() {
		MdWarehouse warehouse = new MdWarehouse();
		warehouse.setStatus(MasterDataStatus.ACTIVE);
		when(mdWarehouseMapper.selectById(1L)).thenReturn(warehouse);
		when(mdLocationMapper.selectList(any())).thenReturn(List.of());
		when(readinessPolicy.isInboundReady(warehouse, List.of())).thenReturn(false);

		BizException e = assertThrows(BizException.class,
				() -> warehouseGuard.requireActiveForInbound(1L));
		assertEquals("仓库没有可用的库位，不能入库", e.getMessage());
	}

	@Test
	void activeWarehouseWithUsableLocations_passes() {
		MdWarehouse warehouse = new MdWarehouse();
		warehouse.setStatus(MasterDataStatus.ACTIVE);
		when(mdWarehouseMapper.selectById(1L)).thenReturn(warehouse);
		when(mdLocationMapper.selectList(any())).thenReturn(List.of(new MdLocation()));
		when(readinessPolicy.isInboundReady(any(MdWarehouse.class), anyList())).thenReturn(true);

		assertDoesNotThrow(() -> warehouseGuard.requireActiveForInbound(1L));
	}
}
