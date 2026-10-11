package com.velrix.platform.infrastructure.security;

import com.velrix.platform.domain.user.SysUser;
import com.velrix.platform.infrastructure.config.VelrixJwtProperties;
import com.velrix.platform.infrastructure.redis.TokenBlacklistStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

	private JwtService jwtService;
	private TokenBlacklistStore tokenBlacklistStore;
	private JwtAuthFilter filter;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService(
				new VelrixJwtProperties("velrix-test-secret-key-0123456789abcdef", 30, 7));
		tokenBlacklistStore = mock(TokenBlacklistStore.class);
		when(tokenBlacklistStore.isBlacklisted(anyString())).thenReturn(false);
		filter = new JwtAuthFilter(jwtService, tokenBlacklistStore);
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void validToken_setsAuthentication() throws Exception {
		SysUser user = new SysUser();
		user.setId(1001L);
		user.setUsername("admin");
		String token = jwtService.issueAccess(user);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(authentication);
		AuthUser principal = (AuthUser) authentication.getPrincipal();
		assertEquals(1001L, principal.id());
		assertEquals("admin", principal.username());
	}

	@Test
	void blacklistedToken_noAuthentication() throws Exception {
		SysUser user = new SysUser();
		user.setId(1001L);
		user.setUsername("admin");
		String token = jwtService.issueAccess(user);
		String jti = jwtService.parse(token).getId();
		when(tokenBlacklistStore.isBlacklisted(jti)).thenReturn(true);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void noHeader_noAuthentication() throws Exception {
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void tamperedToken_noAuthentication() throws Exception {
		SysUser user = new SysUser();
		user.setId(1001L);
		user.setUsername("admin");
		String token = jwtService.issueAccess(user);

		int mid = token.length() / 2;
		String tampered = token.substring(0, mid)
				+ (token.charAt(mid) == 'a' ? 'b' : 'a')
				+ token.substring(mid + 1);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + tampered);

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}
}
