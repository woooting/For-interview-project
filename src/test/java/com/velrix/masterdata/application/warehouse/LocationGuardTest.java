package com.velrix.masterdata.application.warehouse;

import com.velrix.masterdata.domain.warehouse.MdLocation;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationMapper;
import com.velrix.shared.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationGuardTest {

	@Mock
	private MdLocationMapper mdLocationMapper;

	@InjectMocks
	private LocationGuard locationGuard;

	@Test
	void missingLocation_throws() {
		when(mdLocationMapper.selectById(9L)).thenReturn(null);

		BizException e = assertThrows(BizException.class,
				() -> locationGuard.requireBelongsToWarehouse(9L, 1L));
		assertEquals("库位不存在", e.getMessage());
	}

	@Test
	void locationOfOtherWarehouse_throws() {
		MdLocation location = new MdLocation();
		location.setWarehouseId(2L);
		when(mdLocationMapper.selectById(1L)).thenReturn(location);

		BizException e = assertThrows(BizException.class,
				() -> locationGuard.requireBelongsToWarehouse(1L, 1L));
		assertEquals("库位不属于该仓库", e.getMessage());
	}

	@Test
	void locationOfWarehouse_returnsIt() {
		MdLocation location = new MdLocation();
		location.setWarehouseId(1L);
		when(mdLocationMapper.selectById(1L)).thenReturn(location);

		assertSame(location, locationGuard.requireBelongsToWarehouse(1L, 1L));
	}
}
