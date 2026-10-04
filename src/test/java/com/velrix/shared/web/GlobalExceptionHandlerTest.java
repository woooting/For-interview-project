package com.velrix.shared.web;

import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void bizException_becomes422Json() {
		BizException ex = new BizException(ApiCodes.BIZ_LOGIN_FAILED, "用户名或密码错误");

		ResponseEntity<ApiResponse<Void>> resp = handler.handleBizException(ex);

		assertEquals(422, resp.getStatusCode().value());
		assertEquals(ApiCodes.BIZ_LOGIN_FAILED, resp.getBody().code());
		assertEquals("用户名或密码错误", resp.getBody().message());
	}
}
