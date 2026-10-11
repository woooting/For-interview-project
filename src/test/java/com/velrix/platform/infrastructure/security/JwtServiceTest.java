package com.velrix.platform.infrastructure.security;

import com.velrix.platform.domain.user.SysUser;
import com.velrix.platform.infrastructure.config.VelrixJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

	private final JwtService jwtService = new JwtService(
			new VelrixJwtProperties("velrix-test-secret-key-0123456789abcdef", 30, 7));

	@Test
	void issueAccess_thenParse_roundTrip() {
		SysUser user = new SysUser();
		user.setId(1001L);
		user.setUsername("admin");

		Claims claims = jwtService.parse(jwtService.issueAccess(user));

		assertEquals("1001", claims.getSubject());
		assertEquals("admin", claims.get("username", String.class));
		assertNotNull(claims.getExpiration());
		assertNotNull(claims.getId());
		assertEquals(JwtService.TYP_ACCESS, claims.get(JwtService.CLAIM_TYP, String.class));
		assertTrue(jwtService.remainingTtlSeconds(claims) > 0);
	}

	@Test
	void issueRefresh_hasRefreshTyp() {
		SysUser user = new SysUser();
		user.setId(1001L);
		user.setUsername("admin");

		Claims claims = jwtService.parse(jwtService.issueRefresh(user));

		assertEquals(JwtService.TYP_REFRESH, claims.get(JwtService.CLAIM_TYP, String.class));
		assertNotNull(claims.getId());
	}

	@Test
	void parse_tamperedToken_throws() {
		SysUser user = new SysUser();
		user.setId(1001L);
		user.setUsername("admin");
		String token = jwtService.issueAccess(user);

		int mid = token.length() / 2;
		String tampered = token.substring(0, mid)
				+ (token.charAt(mid) == 'a' ? 'b' : 'a')
				+ token.substring(mid + 1);

		assertThrows(JwtException.class, () -> jwtService.parse(tampered));
	}
}
