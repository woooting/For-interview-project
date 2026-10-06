package com.velrix.platform.web.controller.auth;

import com.velrix.platform.application.auth.AuthService;
import com.velrix.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        String jwtToken = authService.login(request.username(), request.password());
        return ApiResponse.ok(new LoginResponse(jwtToken));
    }
}
