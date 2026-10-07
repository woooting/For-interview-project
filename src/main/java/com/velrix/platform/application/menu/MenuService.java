package com.velrix.platform.application.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.velrix.platform.application.menu.dto.MenuTreeResponse;
import com.velrix.platform.application.menu.dto.SaveMenuRequest;
import com.velrix.platform.domain.menu.SysMenu;
import com.velrix.platform.infrastructure.persistence.menu.SysMenuMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuService {
    private final SysMenuMapper sysMenuMapper;

    public SysMenu create(SaveMenuRequest body) {
        // id 传 null：还没有这一行，prepare 按创建处理，不做编辑才有的检查
        SysMenu menu = prepare(null, body);
        // id、创建时间留空，insert 后雪花 id 会填回这个对象
        sysMenuMapper.insert(menu);
        return menu;
    }

    public SysMenu update(Long id, SaveMenuRequest body) {
        // null 不能传给 prepare，那里的 null 表示创建，不表示菜单不存在
        if (id == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该菜单不存在");
        }
        SysMenu menu = prepare(id, body);
        // 用 set 而不是 updateById：父级、路由、权限码要能写成 null，updateById 会跳过 null
        sysMenuMapper.update(null, new LambdaUpdateWrapper<SysMenu>()
                .eq(SysMenu::getId, id)
                .set(SysMenu::getParentId, menu.getParentId())
                .set(SysMenu::getName, menu.getName())
                .set(SysMenu::getPath, menu.getPath())
                .set(SysMenu::getPermCode, menu.getPermCode())
                .set(SysMenu::getType, menu.getType())
                .set(SysMenu::getSort, menu.getSort())
                .set(SysMenu::getHidden, menu.getHidden()));
        // 再查一次，把数据库填上的更新时间带回返回值
        return sysMenuMapper.selectById(id);
    }

    /**
     * 校验并组装尚未落库的菜单。id 为 null 表示创建；有值表示编辑。
     */
    private SysMenu prepare(Long id, SaveMenuRequest body) {
        Long parentId = body.parentId();
        String name = body.name();
        String path = body.path();
        String type = body.type();
        Boolean hidden = body.hidden();
        Integer sort = body.sort();
        String permCode = body.permCode();

        // 只有编辑才查这条在不在。创建时 id 为 null，这里直接跳过
        if (id != null && sysMenuMapper.selectById(id) == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该菜单不存在");
        }
        if (name == null || name.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "菜单名字不能为空");
        }
        // 页面靠菜单 id 授权，按钮靠权限码拦接口，类型只允许这两个
        if (!"MENU".equals(type) && !"BUTTON".equals(type)) {
            throw new BizException(ApiCodes.BIZ_ERROR, "菜单类型只能是页面或按钮");
        }

        // 先判空再 trim，页面经常不传权限码
        String trimmedPermCode = permCode == null ? null : permCode.trim();
        if ("BUTTON".equals(type)) {
            // 按钮没有权限码，@RequirePerm 对不上，接口无法授权
            if (trimmedPermCode == null || trimmedPermCode.isBlank()) {
                throw new BizException(ApiCodes.BIZ_ERROR, "按钮必须填写权限码");
            }
            LambdaQueryWrapper<SysMenu> sameCodeQuery = new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getPermCode, trimmedPermCode);
            // 编辑时排除自己，否则不改权限码也会报已经存在。创建没有 id，不能加这一句
            if (id != null) {
                sameCodeQuery.ne(SysMenu::getId, id);
            }
            if (sysMenuMapper.selectOne(sameCodeQuery) != null) {
                throw new BizException(ApiCodes.BIZ_ERROR, "该权限码已经存在");
            }
        } else if (trimmedPermCode != null && !trimmedPermCode.isBlank()) {
            // 页面不挂权限码。谁能进这个页面，看角色有没有勾上这条菜单的 id
            throw new BizException(ApiCodes.BIZ_ERROR, "页面不能填写权限码");
        }

        // parentId 为空表示根节点，不用查父级
        if (parentId != null) {
            // 自己当自己的父级会立刻成环
            if (id != null && id.equals(parentId)) {
                throw new BizException(ApiCodes.BIZ_ERROR, "不能把自己设为父级");
            }
            SysMenu parent = sysMenuMapper.selectById(parentId);
            if (parent == null) {
                throw new BizException(ApiCodes.BIZ_ERROR, "父级菜单不存在");
            }
            // 按钮下面不能再挂子节点，所以父级必须是页面
            if (!Objects.equals(parent.getType(), "MENU")) {
                throw new BizException(ApiCodes.BIZ_ERROR, "父级必须是页面");
            }
            // 从新父级往上走，若走到正在编辑的这一条，说明新父级是自己的子孙
            if (id != null) {
                SysMenu cursor = parent;
                while (cursor != null) {
                    if (Objects.equals(id, cursor.getId())) {
                        throw new BizException(ApiCodes.BIZ_ERROR, "不能把子孙设为父级");
                    }
                    Long nextId = cursor.getParentId();
                    cursor = nextId == null ? null : sysMenuMapper.selectById(nextId);
                }
            }
        }

        // 已有子节点时不能改成按钮，否则那些子节点的父级会变成按钮
        if (id != null && "BUTTON".equals(type)) {
            long children = sysMenuMapper.selectCount(
                    new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
            if (children > 0) {
                throw new BizException(ApiCodes.BIZ_ERROR, "下面还有子菜单，不能改成按钮");
            }
        }

        // 只组装，不落库。id 和时间留给数据库
        SysMenu menu = new SysMenu();
        menu.setParentId(parentId);
        menu.setName(name.trim());
        // 空白路由当成没有路由，存 null，不要存空字符串
        menu.setPath(path == null || path.isBlank() ? null : path.trim());
        // 页面存 null。唯一索引允许多个 null，空字符串只能有一条
        menu.setPermCode("MENU".equals(type) ? null : trimmedPermCode);
        menu.setType(type);
        menu.setSort(sort == null ? 0 : sort);
        // 只有传入 true 才隐藏，null 落成 false
        menu.setHidden(hidden != null && hidden);
        return menu;
    }

    public List<MenuTreeResponse> listTree(){
        List<SysMenu> rows = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        // byId 存的是菜单表中的行数据
        Map<Long,MenuTreeResponse> byId = new HashMap<>();
        for (SysMenu row : rows) {
            byId.put(row.getId(),new MenuTreeResponse(
                    row.getId(),
                    row.getParentId(),
                    row.getName(),
                    row.getPath(),
                    row.getType(),
                    row.getPermCode(),
                    row.getSort(),
                    row.getHidden(),
                    // children
                    new ArrayList<>()
            ));
        }
        //树形嵌套
        List<MenuTreeResponse> roots = new ArrayList<>();
        for(SysMenu row : rows){
            MenuTreeResponse node = byId.get(row.getId());
            MenuTreeResponse parent = byId.get(row.getParentId());
            // parent 取不到就是根；取到了就挂进父节点的 children
            if(parent == null){
                roots.add(node);
            }else {
                parent.children().add(node);
            }
        }
        return  roots;
    }

    public void delete(Long id){
        if (id == null || sysMenuMapper.selectById(id) == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "该菜单不存在");
        }
        long children = sysMenuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId,id)
        );
        if (children > 0) {
            throw new BizException(ApiCodes.BIZ_ERROR, "下面还有子菜单，不能删除");
        }
        sysMenuMapper.deleteByMenuId(id);
        sysMenuMapper.deleteById(id);
    }

}
