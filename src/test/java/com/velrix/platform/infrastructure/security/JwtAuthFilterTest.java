package com.velrix.platform.infrastructure.security;

import com.velrix.platform.domain.user.SysUser;
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

class JwtAuthFilterTest {

	private JwtService jwtService;
	private JwtAuthFilter filter;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService("velrix-test-secret-key-0123456789abcdef", 30);
		filter = new JwtAuthFilter(jwtService);
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
		String token = jwtService.issue(user);

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
	void noHeader_noAuthentication() throws Exception {
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void tamperedToken_noAuthentication() throws Exception {
		SysUser user = new SysUser();
		user.setId(1001L);
		user.setUsername("admin");
		String token = jwtService.issue(user);

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
