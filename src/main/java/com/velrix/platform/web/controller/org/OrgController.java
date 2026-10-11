package com.velrix.platform.web.controller.org;

import com.velrix.platform.application.org.OrgService;
import com.velrix.platform.application.org.dto.OrgTreeResponse;
import com.velrix.platform.application.org.dto.SaveOrgRequest;
import com.velrix.platform.domain.org.SysOrg;
import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.web.RequirePerm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orgs")
public class OrgController {

    private final OrgService orgService;

    @PostMapping
    @RequirePerm("org:create")
    public ApiResponse<SysOrg> create(@RequestBody SaveOrgRequest body) {
        return ApiResponse.ok(orgService.create(body));
    }

    @PutMapping("/{id}")
    @RequirePerm("org:update")
    public ApiResponse<SysOrg> update(
            @PathVariable("id") Long id,
            @RequestBody SaveOrgRequest body) {
        return ApiResponse.ok(orgService.update(id, body));
    }

    @GetMapping
    @RequirePerm("org:list")
    public ApiResponse<List<OrgTreeResponse>> list() {
        return ApiResponse.ok(orgService.listTree());
    }

    @DeleteMapping("/{id}")
    @RequirePerm("org:delete")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        orgService.delete(id);
        return ApiResponse.ok(null);
    }
}
