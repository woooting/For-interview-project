package com.velrix.masterdata.web.support;

import com.velrix.platform.infrastructure.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class WebActor {

    private WebActor() {
    }

    public static String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return authUser.username();
    }

    public static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
