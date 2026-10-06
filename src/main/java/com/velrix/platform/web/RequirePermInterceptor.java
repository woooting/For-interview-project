package com.velrix.platform.web;

import com.velrix.platform.application.MenuAccessService;
import com.velrix.platform.domain.AuthUser;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.ForbiddenException;
import com.velrix.shared.web.RequirePerm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
@RequiredArgsConstructor
public  class  RequirePermInterceptor  implements HandlerInterceptor {
    private final MenuAccessService menuAccessService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequirePerm perm = method.getMethodAnnotation(RequirePerm.class);
        if (perm == null) {
            return true;
        }
        // 1. 取出 AuthUser
        // 2. menuAccessService.hasPerm(用户id, perm.value()) 为 false 时抛 ForbiddenException
        // 3. 有权限则 return true
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        if(!menuAccessService.hasPerm(authUser.id(), perm.value())){
            throw new ForbiddenException(ApiCodes.FORBIDDEN,"没有权限");
        }
        return true;
    }
}




