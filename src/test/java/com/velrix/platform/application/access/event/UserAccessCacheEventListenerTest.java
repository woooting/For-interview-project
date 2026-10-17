package com.velrix.platform.application.access.event;

import com.velrix.platform.application.access.UserAccessCacheInvalidator;
import com.velrix.platform.infrastructure.redis.UserAccessCacheStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserAccessCacheEventListenerTest {

	@Mock
	private UserAccessCacheInvalidator invalidator;

	@Mock
	private UserAccessCacheStore cacheStore;

	@InjectMocks
	private UserAccessCacheEventListener listener;

	@Test
	void onUserAccessEvict_invalidatesUser() {
		listener.onUserAccessEvict(new UserAccessEvictEvent(1002L));
		verify(invalidator).invalidateUser(1002L);
	}

	@Test
	void onRoleMenuReplaced_invalidatesByRole() {
		listener.onRoleMenuReplaced(new RoleMenuReplacedEvent(2L));
		verify(invalidator).invalidateByRoleId(2L);
	}

	@Test
	void onMenuUpdated_withUserIdsSnapshot_evictsWithoutLookup() {
		listener.onMenuUpdated(new MenuChangedEvent(2100L, Optional.of(List.of(1002L, 1003L))));

		verify(cacheStore).evictAll(List.of(1002L, 1003L));
		verifyNoInteractions(invalidator);
	}

	@Test
	void onMenuUpdated_withoutSnapshot_invalidatesByMenuId() {
		listener.onMenuUpdated(new MenuChangedEvent(2100L, Optional.empty()));

		verify(invalidator).invalidateByMenuId(2100L);
		verifyNoInteractions(cacheStore);
	}
}
