package com.velrix.masterdata.application.supplier;

import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.supplier.MdSupplier;
import com.velrix.masterdata.infrastructure.persistence.supplier.MdSupplierMapper;
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

/**
 * AC-M1-10：停用供应商不能再被业务引用（M3 采购门禁调用入口，接口先交付）。
 */
@ExtendWith(MockitoExtension.class)
class SupplierGuardTest {

	@Mock
	private MdSupplierMapper mdSupplierMapper;

	@InjectMocks
	private SupplierGuard supplierGuard;

	@Test
	void missingSupplier_throws() {
		when(mdSupplierMapper.selectById(9L)).thenReturn(null);

		BizException e = assertThrows(BizException.class, () -> supplierGuard.requireActive(9L));
		assertEquals("供应商不存在", e.getMessage());
	}

	@Test
	void inactiveSupplier_throws() {
		MdSupplier supplier = new MdSupplier();
		supplier.setStatus(MasterDataStatus.INACTIVE);
		when(mdSupplierMapper.selectById(1L)).thenReturn(supplier);

		BizException e = assertThrows(BizException.class, () -> supplierGuard.requireActive(1L));
		assertEquals("供应商已停用", e.getMessage());
	}

	@Test
	void activeSupplier_returnsIt() {
		MdSupplier supplier = new MdSupplier();
		supplier.setStatus(MasterDataStatus.ACTIVE);
		when(mdSupplierMapper.selectById(1L)).thenReturn(supplier);

		assertSame(supplier, supplierGuard.requireActive(1L));
	}
}
