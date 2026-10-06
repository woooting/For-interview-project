package com.velrix.platform.web.controller;

import com.velrix.platform.application.AuthService;
import com.velrix.platform.application.MenuAccessService;
import com.velrix.platform.application.UserAccess;
import com.velrix.platform.domain.AuthUser;
import com.velrix.platform.domain.SysUser;
import com.velrix.platform.web.dto.MeResponse;
import com.velrix.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MeController {

    private final AuthService authService;
    private final MenuAccessService menuAccessService;

    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        SysUser user = authService.getById(authUser.id());
        UserAccess access = menuAccessService.loadAccess(authUser.id());
        return ApiResponse.ok(new MeResponse(user.getId(), user.getUsername(), user.getDisplayName(),access.menus(), access.permCodes()));
    }
}
