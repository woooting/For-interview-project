package com.velrix.masterdata.application.supplier;

import com.velrix.masterdata.application.impact.ImpactSummary;
import com.velrix.masterdata.application.impact.MasterDataImpactService;
import com.velrix.masterdata.application.impact.MasterDataKind;
import com.velrix.masterdata.application.support.MasterdataAuditWriter;
import com.velrix.masterdata.domain.supplier.MdSupplier;
import com.velrix.masterdata.infrastructure.persistence.supplier.MdSupplierMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import com.velrix.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AC-M1-09/10：供应商启停 + 删除一律 409（与客户线对称，抽核心路径）。
 */
@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

	@Mock
	private MdSupplierMapper mdSupplierMapper;

	@Mock
	private MasterdataAuditWriter auditWriter;

	@Mock
	private MasterDataImpactService impactService;

	@InjectMocks
	private SupplierService supplierService;

	@Test
	void patchStatus_rejectsUnknownStatus() {
		BizException e = assertThrows(BizException.class,
				() -> supplierService.patchStatus(1L, "PAUSED", "admin", "127.0.0.1"));

		assertEquals("状态只能是 ACTIVE 或 INACTIVE", e.getMessage());
		verifyNoInteractions(mdSupplierMapper, auditWriter);
	}

	@Test
	void patchStatus_updatesAndWritesAudit() {
		MdSupplier supplier = new MdSupplier();
		supplier.setId(1L);
		supplier.setCodeNorm("S001");
		when(mdSupplierMapper.selectById(1L)).thenReturn(supplier);

		MdSupplier result = supplierService.patchStatus(1L, "INACTIVE", "admin", "127.0.0.1");

		assertEquals("INACTIVE", result.getStatus());
		verify(mdSupplierMapper).updateById(supplier);
		verify(auditWriter).write(eq("admin"), eq("MD_SUPPLIER_STATUS"), eq("MD_SUPPLIER"), eq(1L),
				anyString(), eq("127.0.0.1"));
	}

	@Test
	void delete_withoutRefs_stillConflicts() {
		when(mdSupplierMapper.selectById(1L)).thenReturn(new MdSupplier());
		when(impactService.summarize(MasterDataKind.SUPPLIER, 1L)).thenReturn(
				new ImpactSummary(true, "供应商暂无业务引用", List.of()));

		ConflictException e = assertThrows(ConflictException.class, () -> supplierService.delete(1L));

		assertEquals(ApiCodes.BIZ_CONFLICT, e.getCode());
		assertTrue(e.getMessage().contains("请使用停用"));
	}

	@Test
	void delete_missingSupplier_throwsBizError() {
		when(mdSupplierMapper.selectById(9L)).thenReturn(null);

		BizException e = assertThrows(BizException.class, () -> supplierService.delete(9L));

		assertEquals("供应商不存在", e.getMessage());
	}

	@Test
	void impact_returnsSummaryForExistingSupplier() {
		when(mdSupplierMapper.selectById(1L)).thenReturn(new MdSupplier());
		ImpactSummary expected = new ImpactSummary(true, "供应商暂无业务引用", List.of());
		when(impactService.summarize(MasterDataKind.SUPPLIER, 1L)).thenReturn(expected);

		assertEquals(expected, supplierService.impact(1L));
	}
}
