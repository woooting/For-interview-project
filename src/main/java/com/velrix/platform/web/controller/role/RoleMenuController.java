package com.velrix.platform.web.controller.role;

import com.velrix.platform.application.role.result.RoleMenuDiff;
import com.velrix.platform.application.role.RoleMenuService;
import com.velrix.platform.infrastructure.security.AuthUser;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleMenuController {
    private final RoleMenuService roleMenuService;

    @PutMapping("/{id}/menus")
    @RequirePerm("role:grant-menus")
    public ApiResponse<RoleMenuDiff> replace(
            @PathVariable("id")Long id,
            @RequestBody ReplaceRoleMenusRequest body,
            HttpServletRequest request){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        String actor = authUser.username();
        String ip = request.getRemoteAddr();

        List<Long> menuIds = (body.menuIds() == null || body.menuIds().isEmpty())
                ? null
                : body.menuIds();
        RoleMenuDiff diff = roleMenuService.replace(id,menuIds,actor,ip);
        return ApiResponse.ok(diff);
    }
}
