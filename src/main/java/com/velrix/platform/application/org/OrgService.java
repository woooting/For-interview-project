package com.velrix.platform.application.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.velrix.platform.application.org.dto.OrgTreeResponse;
import com.velrix.platform.application.org.dto.SaveOrgRequest;
import com.velrix.platform.domain.org.SysOrg;
import com.velrix.platform.domain.user.SysUser;
import com.velrix.platform.infrastructure.persistence.org.SysOrgMapper;
import com.velrix.platform.infrastructure.persistence.user.SysUserMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class OrgService {

    private final SysOrgMapper sysOrgMapper;
    private final SysUserMapper sysUserMapper;

    public SysOrg create(SaveOrgRequest body) {
        // id 传 null：还没有这一行，prepare 按创建处理
        SysOrg org = prepare(null, body);
        sysOrgMapper.insert(org);
        return org;
    }

    public SysOrg update(Long id, SaveOrgRequest body) {
        if (id == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该组织不存在");
        }
        SysOrg org = prepare(id, body);
        // 用 set 而不是 updateById：根节点的 parentId 要能写成 null，updateById 会跳过 null
        sysOrgMapper.update(null, new LambdaUpdateWrapper<SysOrg>()
                .eq(SysOrg::getId, id)
                .set(SysOrg::getParentId, org.getParentId())
                .set(SysOrg::getName, org.getName())
                .set(SysOrg::getType, org.getType())
                .set(SysOrg::getSort, org.getSort()));
        return sysOrgMapper.selectById(id);
    }

    /**
     * 校验并组装尚未落库的组织。id 为 null 表示创建；有值表示编辑。
     */
    private SysOrg prepare(Long id, SaveOrgRequest body) {
        Long parentId = body.parentId();
        String name = body.name();
        String type = body.type();
        Integer sort = body.sort();

        if (id != null && sysOrgMapper.selectById(id) == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该组织不存在");
        }
        if (name == null || name.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "部门名称不能为空");
        }
        String trimmedName = name.trim();

        // 同级不重名：根节点 parent_id 为 NULL，要用 isNull 而不是 eq(null)
        LambdaQueryWrapper<SysOrg> sameNameQuery = new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getName, trimmedName);
        if (parentId == null) {
            sameNameQuery.isNull(SysOrg::getParentId);
        } else {
            sameNameQuery.eq(SysOrg::getParentId, parentId);
        }
        if (id != null) {
            sameNameQuery.ne(SysOrg::getId, id);
        }
        if (sysOrgMapper.selectOne(sameNameQuery) != null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "同级下已有同名部门");
        }

        // parentId 为空表示根部门
        if (parentId != null) {
            if (id != null && id.equals(parentId)) {
                throw new BizException(ApiCodes.BIZ_ERROR, "不能把自己设为父级");
            }
            SysOrg parent = sysOrgMapper.selectById(parentId);
            if (parent == null) {
                throw new BizException(ApiCodes.BIZ_ERROR, "父级组织不存在");
            }
            // 从新父级往上走，若走到正在编辑的这一条，说明新父级是自己的子孙
            if (id != null) {
                SysOrg cursor = parent;
                while (cursor != null) {
                    if (Objects.equals(id, cursor.getId())) {
                        throw new BizException(ApiCodes.BIZ_ERROR, "不能把子孙设为父级");
                    }
                    Long nextId = cursor.getParentId();
                    cursor = nextId == null ? null : sysOrgMapper.selectById(nextId);
                }
            }
        }

        SysOrg org = new SysOrg();
        org.setParentId(parentId);
        org.setName(trimmedName);
        // type 可空，空白存 null
        org.setType(type == null || type.isBlank() ? null : type.trim());
        org.setSort(sort == null ? 0 : sort);
        return org;
    }

    public List<OrgTreeResponse> listTree() {
        List<SysOrg> rows = sysOrgMapper.selectList(
                new LambdaQueryWrapper<SysOrg>().orderByAsc(SysOrg::getSort));
        Map<Long, OrgTreeResponse> byId = new HashMap<>();
        for (SysOrg row : rows) {
            byId.put(row.getId(), new OrgTreeResponse(
                    row.getId(),
                    row.getParentId(),
                    row.getName(),
                    row.getType(),
                    row.getSort(),
                    new ArrayList<>()));
        }
        List<OrgTreeResponse> roots = new ArrayList<>();
        for (SysOrg row : rows) {
            OrgTreeResponse node = byId.get(row.getId());
            OrgTreeResponse parent = byId.get(row.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    public void delete(Long id) {
        if (id == null || sysOrgMapper.selectById(id) == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该组织不存在");
        }
        long children = sysOrgMapper.selectCount(
                new LambdaQueryWrapper<SysOrg>().eq(SysOrg::getParentId, id));
        if (children > 0) {
            throw new BizException(ApiCodes.BIZ_ERROR, "下面还有子部门，不能删除");
        }
        long users = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getOrgId, id));
        if (users > 0) {
            throw new BizException(ApiCodes.BIZ_ERROR, "还有用户归属该部门，不能删除");
        }
        sysOrgMapper.deleteById(id);
    }
}
