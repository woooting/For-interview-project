package com.velrix.masterdata.application.product;

import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.product.MdProduct;
import com.velrix.masterdata.infrastructure.persistence.product.MdProductMapper;
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
 * AC-M1-04：停用商品不能再被业务引用。
 */
@ExtendWith(MockitoExtension.class)
class ProductGuardTest {

	@Mock
	private MdProductMapper mdProductMapper;

	@InjectMocks
	private ProductGuard productGuard;

	@Test
	void missingProduct_throws() {
		when(mdProductMapper.selectById(9L)).thenReturn(null);

		BizException e = assertThrows(BizException.class, () -> productGuard.requireActive(9L));
		assertEquals("商品不存在", e.getMessage());
	}

	@Test
	void inactiveProduct_throws() {
		MdProduct product = new MdProduct();
		product.setStatus(MasterDataStatus.INACTIVE);
		when(mdProductMapper.selectById(1L)).thenReturn(product);

		BizException e = assertThrows(BizException.class, () -> productGuard.requireActive(1L));
		assertEquals("商品已停用", e.getMessage());
	}

	@Test
	void activeProduct_returnsIt() {
		MdProduct product = new MdProduct();
		product.setStatus(MasterDataStatus.ACTIVE);
		when(mdProductMapper.selectById(1L)).thenReturn(product);

		assertSame(product, productGuard.requireActive(1L));
	}
}
