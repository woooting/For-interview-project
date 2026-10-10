package com.velrix.platform.web.controller.user;

import com.velrix.platform.application.user.UserService;
import com.velrix.platform.application.user.dto.UserDetailResponse;
import com.velrix.platform.domain.user.SysUser;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    // 去除user隐私数据
    private UserResponse toResponse(SysUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEnabled());
    }

    @PostMapping
    @RequirePerm("user:create")
    public ApiResponse<UserResponse> create(@RequestBody CreateUserRequest body) {
        SysUser user = userService.create(body.username(), body.displayName(), body.password(), body.enabled());
        return ApiResponse.ok(toResponse(user));
    }

    @PutMapping("/{id}")
    @RequirePerm("user:update")
    public ApiResponse<UserResponse> update(
            @PathVariable("id") Long id,
            @RequestBody UpdateUserRequest body) {
        SysUser user = userService.update(id,body.displayName(), body.password(), body.enabled());
        return ApiResponse.ok(toResponse(user));
    }
    @PutMapping("/{id}/roles")
    @RequirePerm("user:assign-roles")
    public ApiResponse<List<Long>> replaceRoles(
            @PathVariable("id") Long id,
            @RequestBody ReplaceUserRolesRequest body) {
        List<Long> roleIds = userService.replaceRoles(id, body.roleIds());
        return ApiResponse.ok(roleIds);
    }

    @GetMapping
    @RequirePerm("user:list")
    public ApiResponse<List<UserResponse>> getUserList(){
        List<UserResponse> users = userService.list().stream().map(this::toResponse).toList();
        return ApiResponse.ok(users);
    }

    @GetMapping("/{id}")
    @RequirePerm("user:list")
    public ApiResponse<UserDetailResponse> getUserDetail(@PathVariable("id") Long id){
        return ApiResponse.ok(userService.getDetail(id));
    }
}