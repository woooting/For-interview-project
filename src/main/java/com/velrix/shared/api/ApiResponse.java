package com.velrix.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 所有 REST 接口统一的 JSON 外形，对应课表约定：
 * <pre>
 * { "code": "OK", "message": "", "data": { ... } }
 * </pre>
 *
 * @param <T> data 的类型，登录接口可能是 Token 对象，/api/me 可能是用户信息对象
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String code, String message, T data) {

	public boolean isSuccess() {
		return ApiCodes.OK.equals(code);
	}

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(ApiCodes.OK, "", data);
	}

	public static <T> ApiResponse<T> ok() {
		return ok(null);
	}

	public static <T> ApiResponse<T> ok(T data, String message) {
		return new ApiResponse<>(ApiCodes.OK, message, data);
	}

	public static <T> ApiResponse<T> fail(String code, String message) {
		return new ApiResponse<>(code, message, null);
	}
}
