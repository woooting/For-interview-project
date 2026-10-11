package com.velrix.platform.web.controller.auth;

import com.velrix.platform.application.auth.AuthService;
import com.velrix.platform.application.auth.dto.LoginTokens;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginTokens tokens = authService.login(request.username(), request.password());
        return ApiResponse.ok(new LoginResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BizException(ApiCodes.UNAUTHORIZED, "未登录或登录已过期");
        }
        authService.logout(authorization.substring(7));
        return ApiResponse.ok();
    }
}
