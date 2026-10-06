package com.velrix.platform.infrastructure.security;

import com.velrix.platform.domain.user.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

	private final JwtService jwtService = new JwtService("velrix-test-secret-key-0123456789abcdef", 30);

	@Test
	void issue_thenParse_roundTrip() {
		SysUser user = new SysUser();
		user.setId(1001L);
		user.setUsername("admin");

		String token = jwtService.issue(user);
		Claims claims = jwtService.parse(token);

		assertEquals("1001", claims.getSubject());
		assertEquals("admin", claims.get("username", String.class));
		assertNotNull(claims.getExpiration());
	}

	@Test
	void parse_tamperedToken_throws() {
		SysUser user = new SysUser();
		user.setId(1001L);
		user.setUsername("admin");
		String token = jwtService.issue(user);

		int mid = token.length() / 2;
		String tampered = token.substring(0, mid)
				+ (token.charAt(mid) == 'a' ? 'b' : 'a')
				+ token.substring(mid + 1);

		assertThrows(JwtException.class, () -> jwtService.parse(tampered));
	}
}
