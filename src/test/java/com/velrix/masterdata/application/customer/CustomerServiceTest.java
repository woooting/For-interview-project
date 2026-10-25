package com.velrix.masterdata.application.customer;

import com.velrix.masterdata.application.impact.ImpactItem;
import com.velrix.masterdata.application.impact.ImpactSummary;
import com.velrix.masterdata.application.impact.MasterDataImpactService;
import com.velrix.masterdata.application.impact.MasterDataKind;
import com.velrix.masterdata.application.support.MasterdataAuditWriter;
import com.velrix.masterdata.domain.customer.MdCustomer;
import com.velrix.masterdata.infrastructure.persistence.customer.MdCustomerMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import com.velrix.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AC-M1-09/10：客户启停 + 删除一律 409（D2 定稿：主数据仅停用不物理删）。
 * 另覆盖 T4 特有逻辑：可选联系人字段空白归 null、编码规范化查重。
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

	@Mock
	private MdCustomerMapper mdCustomerMapper;

	@Mock
	private MasterdataAuditWriter auditWriter;

	@Mock
	private MasterDataImpactService impactService;

	@InjectMocks
	private CustomerService customerService;

	@Test
	void create_trimsOptionalsAndNormalizesCode() {
		when(mdCustomerMapper.selectOne(any())).thenReturn(null);
		// 模拟 MyBatis-Plus 雪花 id 回填（mock insert 默认不回填，write(long) 拆箱会 NPE）
		when(mdCustomerMapper.insert(any(MdCustomer.class))).thenAnswer(inv -> {
			inv.getArgument(0, MdCustomer.class).setId(1L);
			return 1;
		});

		customerService.create(" c001 ", " 客户A ", "  张三 ", "  ", null, "admin", "127.0.0.1");

		ArgumentCaptor<MdCustomer> captor = ArgumentCaptor.forClass(MdCustomer.class);
		verify(mdCustomerMapper).insert(captor.capture());
		MdCustomer saved = captor.getValue();

		assertEquals("c001", saved.getCode());
		assertEquals("C001", saved.getCodeNorm());
		assertEquals("客户A", saved.getName());
		assertEquals("张三", saved.getContactName());
		// 空白电话/地址归 null（V22 可选列）
		assertNull(saved.getContactPhone());
		assertNull(saved.getAddress());
		assertEquals("ACTIVE", saved.getStatus());
	}

	@Test
	void create_duplicateNorm_throwsBizError() {
		when(mdCustomerMapper.selectOne(any())).thenReturn(new MdCustomer());

		BizException e = assertThrows(BizException.class,
				() -> customerService.create("C001", "客户A", null, null, null, "admin", "127.0.0.1"));

		assertEquals("客户编码已存在", e.getMessage());
		verifyNoInteractions(auditWriter);
	}

	@Test
	void patchStatus_rejectsUnknownStatus() {
		BizException e = assertThrows(BizException.class,
				() -> customerService.patchStatus(1L, "PAUSED", "admin", "127.0.0.1"));

		assertEquals("状态只能是 ACTIVE 或 INACTIVE", e.getMessage());
		verifyNoInteractions(mdCustomerMapper, auditWriter);
	}

	@Test
	void patchStatus_updatesAndWritesAudit() {
		MdCustomer customer = new MdCustomer();
		customer.setId(1L);
		customer.setCodeNorm("C001");
		when(mdCustomerMapper.selectById(1L)).thenReturn(customer);

		MdCustomer result = customerService.patchStatus(1L, "INACTIVE", "admin", "127.0.0.1");

		assertEquals("INACTIVE", result.getStatus());
		verify(mdCustomerMapper).updateById(customer);
		verify(auditWriter).write(eq("admin"), eq("MD_CUSTOMER_STATUS"), eq("MD_CUSTOMER"), eq(1L),
				anyString(), eq("127.0.0.1"));
	}

	@Test
	void delete_withRefs_throwsConflictWithDetail() {
		when(mdCustomerMapper.selectById(1L)).thenReturn(new MdCustomer());
		when(impactService.summarize(MasterDataKind.CUSTOMER, 1L)).thenReturn(new ImpactSummary(
				false, "客户被引用：销售订单 2 条",
				List.of(ImpactItem.of("SALES_ORDER", "销售订单", 2))));

		ConflictException e = assertThrows(ConflictException.class, () -> customerService.delete(1L));

		assertEquals(ApiCodes.BIZ_CONFLICT, e.getCode());
		assertTrue(e.getMessage().contains("销售订单 2 条"));
		assertTrue(e.getMessage().endsWith("，不能删除"));
	}

	@Test
	void delete_withoutRefs_stillConflicts() {
		when(mdCustomerMapper.selectById(1L)).thenReturn(new MdCustomer());
		when(impactService.summarize(MasterDataKind.CUSTOMER, 1L)).thenReturn(
				new ImpactSummary(true, "客户暂无业务引用", List.of()));

		ConflictException e = assertThrows(ConflictException.class, () -> customerService.delete(1L));

		assertEquals(ApiCodes.BIZ_CONFLICT, e.getCode());
		assertTrue(e.getMessage().contains("请使用停用"));
	}

	@Test
	void delete_missingCustomer_throwsBizError() {
		when(mdCustomerMapper.selectById(9L)).thenReturn(null);

		BizException e = assertThrows(BizException.class, () -> customerService.delete(9L));

		assertEquals("客户不存在", e.getMessage());
	}

	@Test
	void impact_returnsSummaryForExistingCustomer() {
		when(mdCustomerMapper.selectById(1L)).thenReturn(new MdCustomer());
		ImpactSummary expected = new ImpactSummary(true, "客户暂无业务引用", List.of());
		when(impactService.summarize(MasterDataKind.CUSTOMER, 1L)).thenReturn(expected);

		assertEquals(expected, customerService.impact(1L));
	}
}
