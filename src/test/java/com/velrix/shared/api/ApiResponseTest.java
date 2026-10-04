package com.velrix.shared.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

	@Test
	void ok_withData() {
		ApiResponse<String> resp = ApiResponse.ok("hello");
		assertTrue(resp.isSuccess());
		assertEquals(ApiCodes.OK, resp.code());
		assertEquals("", resp.message());
		assertEquals("hello", resp.data());
	}

	@Test
	void fail_setsCodeAndMessage() {
		ApiResponse<Void> resp = ApiResponse.fail(ApiCodes.BIZ_ERROR, "用户名或密码错误");
		assertEquals(ApiCodes.BIZ_ERROR, resp.code());
		assertEquals("用户名或密码错误", resp.message());
	}
}
