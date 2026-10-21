package com.velrix.shared.api;

/**
 * 接口返回里的业务码（字符串），和 HTTP 状态码是两回事。
 * <p>
 * HTTP：401 未登录、403 无权限、422 参数不对……<br>
 * code：给前端/调用方看的业务结果，成功固定 {@link #OK}，失败用 BIZ_ 开头等。
 */
public final class ApiCodes {

	private ApiCodes() {
	}

	/** 成功 */
	public static final String OK = "OK";

	/** 通用业务失败（具体场景以后可再加 BIZ_USER_DISABLED 等） */
	public static final String BIZ_ERROR = "BIZ_ERROR";
	/** 参数校验失败：请求体缺字段/空值/格式不对（HTTP 400） */
	public static final String PARAM_INVALID = "PARAM_INVALID";
	/** 登录失败（用户名或密码错误），统一提示、不暴露账号是否存在 */
	public static final String BIZ_LOGIN_FAILED = "BIZ_LOGIN_FAILED";

	/** 未认证：未登录、token 无效或已过期（HTTP 401） */
	public static final String UNAUTHORIZED = "UNAUTHORIZED";
	public static final String FORBIDDEN = "FORBIDDEN";
	/** 业务冲突：资源被引用等不允许的操作（HTTP 409） */
	public static final String BIZ_CONFLICT = "BIZ_CONFLICT";
}
