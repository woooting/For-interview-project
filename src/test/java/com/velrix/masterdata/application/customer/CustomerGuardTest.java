package com.velrix.masterdata.application.customer;

import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.customer.MdCustomer;
import com.velrix.masterdata.infrastructure.persistence.customer.MdCustomerMapper;
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
 * AC-M1-10：停用客户不能再被业务引用（M3 下单门禁调用入口，接口先交付）。
 */
@ExtendWith(MockitoExtension.class)
class CustomerGuardTest {

	@Mock
	private MdCustomerMapper mdCustomerMapper;

	@InjectMocks
	private CustomerGuard customerGuard;

	@Test
	void missingCustomer_throws() {
		when(mdCustomerMapper.selectById(9L)).thenReturn(null);

		BizException e = assertThrows(BizException.class, () -> customerGuard.requireActive(9L));
		assertEquals("客户不存在", e.getMessage());
	}

	@Test
	void inactiveCustomer_throws() {
		MdCustomer customer = new MdCustomer();
		customer.setStatus(MasterDataStatus.INACTIVE);
		when(mdCustomerMapper.selectById(1L)).thenReturn(customer);

		BizException e = assertThrows(BizException.class, () -> customerGuard.requireActive(1L));
		assertEquals("客户已停用", e.getMessage());
	}

	@Test
	void activeCustomer_returnsIt() {
		MdCustomer customer = new MdCustomer();
		customer.setStatus(MasterDataStatus.ACTIVE);
		when(mdCustomerMapper.selectById(1L)).thenReturn(customer);

		assertSame(customer, customerGuard.requireActive(1L));
	}
}
