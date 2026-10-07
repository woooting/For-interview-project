package com.velrix.platform.application.access;

import com.velrix.platform.application.access.dto.MenuNodeResponse;
import com.velrix.platform.application.access.dto.UserAccessResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MenuAccessServiceTest {

	@Autowired
	private MenuAccessService menuAccessService;

	@Test
	void lisi_seesPurchaseMenuAndSubmitPerm() {
		UserAccessResponse access = menuAccessService.loadAccess(1002L);

		assertEquals(List.of("purchase-order:submit"), access.permCodes());
		assertEquals(1, access.menus().size());

		MenuNodeResponse purchase = access.menus().get(0);
		assertEquals(2100L, purchase.id());
		assertEquals("采购", purchase.name());
		assertEquals("/purchase", purchase.path());
		assertEquals(1, purchase.children().size());

		MenuNodeResponse order = purchase.children().get(0);
		assertEquals(2101L, order.id());
		assertEquals("采购订单", order.name());
		assertEquals("/purchase/orders", order.path());
		assertTrue(order.children().isEmpty());
	}

	@Test
	void admin_seesFullMenuTreeAndSubmitPerm() {
		UserAccessResponse access = menuAccessService.loadAccess(1001L);

		assertEquals(Set.of("purchase-order:submit", "role:grant-menus"), Set.copyOf(access.permCodes()));
		assertEquals(List.of(2000L, 2100L), access.menus().stream().map(MenuNodeResponse::id).toList());

		MenuNodeResponse system = access.menus().get(0);
		assertEquals("系统管理", system.name());
		assertEquals(List.of(2001L, 2002L, 2003L),
				system.children().stream().map(MenuNodeResponse::id).toList());

		MenuNodeResponse purchase = access.menus().get(1);
		assertEquals("采购", purchase.name());
		assertEquals(List.of(2101L), purchase.children().stream().map(MenuNodeResponse::id).toList());
		assertTrue(purchase.children().get(0).children().isEmpty());
	}

	@Test
	void wangwu_onlyChildMenu_includesAncestor() {
		UserAccessResponse access = menuAccessService.loadAccess(1003L);

		assertTrue(access.permCodes().isEmpty());
		assertEquals(1, access.menus().size());

		MenuNodeResponse purchase = access.menus().get(0);
		assertEquals(2100L, purchase.id());
		assertEquals("采购", purchase.name());
		assertEquals(1, purchase.children().size());

		MenuNodeResponse order = purchase.children().get(0);
		assertEquals(2101L, order.id());
		assertEquals("采购订单", order.name());
		assertTrue(order.children().isEmpty());
	}
}
