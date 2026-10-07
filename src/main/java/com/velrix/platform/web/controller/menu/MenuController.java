package com.velrix.platform.web.controller.menu;

import com.velrix.platform.application.menu.MenuService;
import com.velrix.platform.application.menu.dto.SaveMenuRequest;
import com.velrix.platform.application.menu.dto.MenuTreeResponse;
import com.velrix.platform.domain.menu.SysMenu;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    @RequirePerm("menu:create")
    public ApiResponse<SysMenu> create(@RequestBody SaveMenuRequest body) {
        return ApiResponse.ok(menuService.create(body));
    }

    @PutMapping("/{id}")
    @RequirePerm("menu:update")
    public ApiResponse<SysMenu> update(
            @PathVariable("id") Long id,
            @RequestBody SaveMenuRequest body) {
        return ApiResponse.ok(menuService.update(id, body));
    }

    @GetMapping
    @RequirePerm("menu:list")
    public ApiResponse<List<MenuTreeResponse>> list() {
        return ApiResponse.ok(menuService.listTree());
    }

    @DeleteMapping("/{id}")
    @RequirePerm("menu:delete")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        menuService.delete(id);
        return ApiResponse.ok(null);
    }
}
