package com.velrix.platform.application.access;

import com.velrix.platform.infrastructure.persistence.menu.SysMenuMapper;
import com.velrix.platform.infrastructure.persistence.role.SysRoleMapper;
import com.velrix.platform.infrastructure.redis.UserAccessCacheStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccessCacheInvalidatorTest {

	private static final long MENU_ID = 2100L;

	@Mock
	private SysMenuMapper sysMenuMapper;

	@Mock
	private SysRoleMapper sysRoleMapper;

	@Mock
	private UserAccessCacheStore userAccessCacheStore;

	@InjectMocks
	private UserAccessCacheInvalidator invalidator;

	@Test
	void resolveUserIdsByMenuId_mergesRoleLinkedUsersAndAdministrators() {
		when(sysRoleMapper.selectUserIdsWithAdministratorRole()).thenReturn(List.of(1001L));
		when(sysMenuMapper.selectRoleIdsByMenuid(MENU_ID)).thenReturn(List.of(2L));
		when(sysRoleMapper.selectUserIdsByRoleIds(List.of(2L))).thenReturn(List.of(1002L, 1001L));

		List<Long> userIds = invalidator.resolveUserIdsByMenuId(MENU_ID);

		assertEquals(List.of(1002L, 1001L), userIds);
	}

	@Test
	void resolveUserIdsByMenuId_noRoleMenuLink_stillReturnsAdministrators() {
		when(sysRoleMapper.selectUserIdsWithAdministratorRole()).thenReturn(List.of(1001L));
		when(sysMenuMapper.selectRoleIdsByMenuid(MENU_ID)).thenReturn(List.of());

		List<Long> userIds = invalidator.resolveUserIdsByMenuId(MENU_ID);

		assertEquals(List.of(1001L), userIds);
		verify(sysRoleMapper, never()).selectUserIdsByRoleIds(anyList());
	}

	@Test
	void resolveUserIdsByMenuId_nullMenuId_returnsEmpty() {
		assertTrue(invalidator.resolveUserIdsByMenuId(null).isEmpty());
	}

	@Test
	void invalidateByMenuId_evictsMergedUsers() {
		when(sysRoleMapper.selectUserIdsWithAdministratorRole()).thenReturn(List.of(1001L));
		when(sysMenuMapper.selectRoleIdsByMenuid(MENU_ID)).thenReturn(List.of(2L));
		when(sysRoleMapper.selectUserIdsByRoleIds(List.of(2L))).thenReturn(List.of(1002L));

		invalidator.invalidateByMenuId(MENU_ID);

		ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
		verify(userAccessCacheStore).evictAll(captor.capture());
		assertEquals(List.of(1002L, 1001L), captor.getValue());
	}

	@Test
	void invalidateByRoleId_delegatesToStore() {
		when(sysRoleMapper.selectUserIdsByRoleId(2L)).thenReturn(List.of(1002L));

		invalidator.invalidateByRoleId(2L);

		verify(userAccessCacheStore).evictAll(List.of(1002L));
	}

	@Test
	void invalidateUser_delegatesToStore() {
		invalidator.invalidateUser(1002L);
		verify(userAccessCacheStore).evict(1002L);
	}
}
