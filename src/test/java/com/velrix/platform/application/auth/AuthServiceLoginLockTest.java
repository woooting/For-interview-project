package com.velrix.platform.application.auth;

import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource(properties = {
		"velrix.auth.max-login-failures=3",
		"velrix.auth.login-lock-minutes=15"
})
class AuthServiceLoginLockTest {

	@Autowired
	private AuthService authService;

	@Test
	void login_afterMaxFailures_accountLocked() {
		String user = "锁定验收专用";
		for (int i = 0; i < 3; i++) {
			assertEquals(ApiCodes.BIZ_LOGIN_FAILED,
					assertThrows(BizException.class, () -> authService.login(user, "wrong"))
							.getCode());
		}
		BizException locked = assertThrows(BizException.class,
				() -> authService.login(user, "wrong"));
		assertEquals(ApiCodes.BIZ_ERROR, locked.getCode());

		assertNotNull(authService.login("admin", "admin123").accessToken());
	}
}
