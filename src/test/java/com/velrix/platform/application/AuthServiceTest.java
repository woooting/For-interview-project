package com.velrix.platform.application;

import com.velrix.platform.domain.SysUser;
import com.velrix.platform.infrastructure.persistence.SysUserMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private SysUserMapper sysUserMapper;

	@Test
	void login_success_returnsJwt() {
		String token = authService.login("admin", "admin123");

		assertNotNull(token);
		assertEquals(3, token.split("\\.").length);
	}

	@Test
	void login_usernameIsCaseInsensitive() {
		String token = authService.login("ADMIN", "admin123");

		assertNotNull(token);
	}

	@Test
	void login_wrongPassword_throws() {
		BizException ex = assertThrows(BizException.class,
				() -> authService.login("admin", "wrong-password"));

		assertEquals(ApiCodes.BIZ_LOGIN_FAILED, ex.getCode());
	}

	@Test
	void login_unknownUser_throws() {
		BizException ex = assertThrows(BizException.class,
				() -> authService.login("nobody", "admin123"));

		assertEquals(ApiCodes.BIZ_LOGIN_FAILED, ex.getCode());
	}

	@Test
	void login_disabledUser_throws() {
		SysUser lisi = sysUserMapper.selectById(1002L);
		lisi.setEnabled(false);
		sysUserMapper.updateById(lisi);

		try {
			BizException ex = assertThrows(BizException.class,
					() -> authService.login("lisi", "admin123"));
			assertEquals(ApiCodes.BIZ_LOGIN_FAILED, ex.getCode());
		} finally {
			lisi.setEnabled(true);
			sysUserMapper.updateById(lisi);
		}
	}
}
