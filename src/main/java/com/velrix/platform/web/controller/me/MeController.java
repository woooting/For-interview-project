package com.velrix.platform.web.controller.me;

import com.velrix.platform.application.auth.AuthService;
import com.velrix.platform.application.access.MenuAccessService;
import com.velrix.platform.application.access.result.UserAccess;
import com.velrix.platform.infrastructure.security.AuthUser;
import com.velrix.platform.domain.user.SysUser;
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
