package com.velrix.platform.application.access;

import com.velrix.platform.application.menu.MenuService;
import com.velrix.platform.application.menu.dto.SaveMenuRequest;
import com.velrix.platform.application.user.UserService;
import com.velrix.platform.infrastructure.redis.UserAccessCacheStore;
import com.velrix.platform.support.RedisTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 依赖本机 MySQL（Flyway 迁移含 V7/V8 种子）与 Redis。密码见 src/test/resources/application.properties。
 */
@SpringBootTest
class UserAccessCacheIntegrationTest {

	private static final long ADMIN_USER_ID = 1001L;
	private static final long LISI_USER_ID = 1002L;
	private static final long PURCHASE_MENU_ID = 2100L;

	@Autowired
	private MenuAccessService menuAccessService;

	@Autowired
	private UserAccessCacheInvalidator userAccessCacheInvalidator;

	@Autowired
	private UserAccessCacheStore userAccessCacheStore;

	@Autowired
	private MenuService menuService;

	@Autowired
	private UserService userService;

	@BeforeEach
	void clearKnownUsersCache() {
		if (!RedisTestSupport.isRedisUp()) {
			return;
		}
		userAccessCacheInvalidator.invalidateUser(ADMIN_USER_ID);
		userAccessCacheInvalidator.invalidateUser(LISI_USER_ID);
	}

	@Test
	void resolveUserIdsByMenuId_usesFlywaySeedData() {
		List<Long> userIds = userAccessCacheInvalidator.resolveUserIdsByMenuId(PURCHASE_MENU_ID);

		assertTrue(userIds.contains(LISI_USER_ID));
		assertTrue(userIds.contains(ADMIN_USER_ID));
	}

	@Test
	void menuUpdate_afterCommit_evictsRedisCache() {
		RedisTestSupport.assumeRedisUp();
		menuAccessService.loadAccess(LISI_USER_ID);
		assertTrue(userAccessCacheStore.getCacheMenu(LISI_USER_ID).isPresent());

		menuService.update(PURCHASE_MENU_ID, new SaveMenuRequest(
				null, "采购", "/purchase", "MENU", false, 20, null));

		assertFalse(userAccessCacheStore.getCacheMenu(LISI_USER_ID).isPresent());
		assertFalse(userAccessCacheStore.getCacheMenu(ADMIN_USER_ID).isPresent());
	}

	@Test
	void replaceRoles_afterCommit_evictsRedisCache() {
		RedisTestSupport.assumeRedisUp();
		menuAccessService.loadAccess(LISI_USER_ID);
		assertTrue(userAccessCacheStore.getCacheMenu(LISI_USER_ID).isPresent());

		List<Long> roles = userService.getDetail(LISI_USER_ID).roleIds();
		userService.replaceRoles(LISI_USER_ID, roles);

		assertFalse(userAccessCacheStore.getCacheMenu(LISI_USER_ID).isPresent());
	}
}
