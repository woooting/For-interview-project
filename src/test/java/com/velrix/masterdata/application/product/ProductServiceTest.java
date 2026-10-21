package com.velrix.masterdata.application.product;

import com.velrix.masterdata.application.impact.ImpactItem;
import com.velrix.masterdata.application.impact.ImpactSummary;
import com.velrix.masterdata.application.impact.MasterDataImpactService;
import com.velrix.masterdata.application.impact.MasterDataKind;
import com.velrix.masterdata.application.support.MasterdataAuditWriter;
import com.velrix.masterdata.domain.product.MdProduct;
import com.velrix.masterdata.infrastructure.persistence.product.MdProductMapper;
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
 * AC-M1-03/04：商品启停 + 删除一律 409（D2 定稿：主数据仅停用不物理删）。
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private MdProductMapper mdProductMapper;

	@Mock
	private MasterdataAuditWriter auditWriter;

	@Mock
	private MasterDataImpactService impactService;

	@InjectMocks
	private ProductService productService;

	@Test
	void patchStatus_rejectsUnknownStatus() {
		BizException e = assertThrows(BizException.class,
				() -> productService.patchStatus(1L, "PAUSED", "admin", "127.0.0.1"));

		assertEquals("状态只能是 ACTIVE 或 INACTIVE", e.getMessage());
		verifyNoInteractions(mdProductMapper, auditWriter);
	}

	@Test
	void patchStatus_updatesAndWritesAudit() {
		MdProduct product = new MdProduct();
		product.setId(1L);
		product.setCodeNorm("SKU001");
		when(mdProductMapper.selectById(1L)).thenReturn(product);

		MdProduct result = productService.patchStatus(1L, "INACTIVE", "admin", "127.0.0.1");

		assertEquals("INACTIVE", result.getStatus());
		verify(mdProductMapper).updateById(product);
		verify(auditWriter).write(eq("admin"), eq("MD_PRODUCT_STATUS"), eq("MD_PRODUCT"), eq(1L),
				anyString(), eq("127.0.0.1"));
	}

	@Test
	void delete_withRefs_throwsConflictWithDetail() {
		when(mdProductMapper.selectById(1L)).thenReturn(new MdProduct());
		when(impactService.summarize(MasterDataKind.PRODUCT, 1L)).thenReturn(new ImpactSummary(
				false, "商品被引用：库位容量配置 3 条",
				List.of(ImpactItem.of("LOCATION_CAPACITY", "库位容量配置", 3))));

		ConflictException e = assertThrows(ConflictException.class, () -> productService.delete(1L));

		assertEquals(ApiCodes.BIZ_CONFLICT, e.getCode());
		assertTrue(e.getMessage().contains("库位容量配置 3 条"));
		assertTrue(e.getMessage().endsWith("，不能删除"));
	}

	@Test
	void delete_withoutRefs_stillConflicts() {
		when(mdProductMapper.selectById(1L)).thenReturn(new MdProduct());
		when(impactService.summarize(MasterDataKind.PRODUCT, 1L)).thenReturn(
				new ImpactSummary(true, "商品暂无业务引用", List.of()));

		ConflictException e = assertThrows(ConflictException.class, () -> productService.delete(1L));

		assertEquals(ApiCodes.BIZ_CONFLICT, e.getCode());
		assertTrue(e.getMessage().contains("请使用停用"));
	}

	@Test
	void delete_missingProduct_throwsBizError() {
		when(mdProductMapper.selectById(9L)).thenReturn(null);

		BizException e = assertThrows(BizException.class, () -> productService.delete(9L));

		assertEquals("商品不存在", e.getMessage());
	}

	@Test
	void impact_returnsSummaryForExistingProduct() {
		when(mdProductMapper.selectById(1L)).thenReturn(new MdProduct());
		ImpactSummary expected = new ImpactSummary(true, "商品暂无业务引用", List.of());
		when(impactService.summarize(MasterDataKind.PRODUCT, 1L)).thenReturn(expected);

		assertEquals(expected, productService.impact(1L));
	}
}
