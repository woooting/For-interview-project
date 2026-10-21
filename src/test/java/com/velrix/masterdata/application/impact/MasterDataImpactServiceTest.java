package com.velrix.masterdata.application.impact;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasterDataImpactServiceTest {

	private static MasterDataImpactContributor contributor(MasterDataKind kind, List<ImpactItem> items) {
		return new MasterDataImpactContributor() {
			@Override
			public MasterDataKind kind() {
				return kind;
			}

			@Override
			public List<ImpactItem> contribute(long entityId) {
				return items;
			}
		};
	}

	@Test
	void aggregatesItemsAcrossContributorsOfSameKind() {
		MasterDataImpactService service = new MasterDataImpactService(List.of(
				contributor(MasterDataKind.WAREHOUSE, List.of(ImpactItem.of("LOCATION", "下挂库位", 2))),
				contributor(MasterDataKind.WAREHOUSE, List.of(ImpactItem.of("OTHER", "其他引用", 5))),
				contributor(MasterDataKind.PRODUCT, List.of(ImpactItem.of("LOCATION_CAPACITY", "库位容量配置", 9)))));

		ImpactSummary summary = service.summarize(MasterDataKind.WAREHOUSE, 1L);

		assertFalse(summary.deletable());
		assertEquals(2, summary.items().size());
		assertEquals("仓库被引用：下挂库位 2 条、其他引用 5 条", summary.message());
	}

	@Test
	void noRefs_meansDeletable() {
		MasterDataImpactService service = new MasterDataImpactService(List.of(
				contributor(MasterDataKind.PRODUCT, List.of())));

		ImpactSummary summary = service.summarize(MasterDataKind.PRODUCT, 1L);

		assertTrue(summary.deletable());
		assertTrue(summary.items().isEmpty());
		assertEquals("商品暂无业务引用", summary.message());
	}

	@Test
	void kindWithoutContributors_meansDeletable() {
		MasterDataImpactService service = new MasterDataImpactService(List.of(
				contributor(MasterDataKind.PRODUCT, List.of(ImpactItem.of("X", "x", 1)))));

		ImpactSummary summary = service.summarize(MasterDataKind.CUSTOMER, 1L);

		assertTrue(summary.deletable());
		assertEquals("客户暂无业务引用", summary.message());
	}

	@Test
	void impactItemFactory_leavesQuantityNull() {
		ImpactItem item = ImpactItem.of("LOCATION_CAPACITY", "库位容量配置", 3);

		assertEquals("LOCATION_CAPACITY", item.refType());
		assertEquals("库位容量配置", item.label());
		assertEquals(3L, item.count());
		assertNull(item.quantity());
	}
}
