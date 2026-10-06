package com.velrix.platform.web.controller.role;


import com.velrix.platform.application.role.RoleService;
import com.velrix.platform.domain.role.SysRole;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @PostMapping
    @RequirePerm("role:create")
    public ApiResponse<SysRole> create(@RequestBody  SaveRoleRequest body) {
        SysRole role = roleService.create(body.name(), body.description(),body.administrator());
        return  ApiResponse.ok(role);
    }

    @PutMapping("/{id}")
    @RequirePerm("role:update")
    public ApiResponse<SysRole> update(
            @PathVariable("id") Long id,
            @RequestBody SaveRoleRequest body) {
        SysRole role = roleService.update(id, body.name(), body.description(),body.administrator());
        return ApiResponse.ok(role);
    }
    @GetMapping
    @RequirePerm("role:list")
    public ApiResponse<List<SysRole>> list() {
        return ApiResponse.ok(roleService.list());
    }

}
