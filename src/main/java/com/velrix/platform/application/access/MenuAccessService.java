package com.velrix.platform.application.access;

import com.velrix.platform.application.access.result.MenuNode;
import com.velrix.platform.application.access.result.UserAccess;
import com.velrix.platform.domain.menu.SysMenu;
import com.velrix.platform.domain.role.SysRole;
import com.velrix.platform.infrastructure.persistence.menu.SysMenuMapper;
import com.velrix.platform.infrastructure.persistence.role.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MenuAccessService {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMapper sysRoleMapper;

    public List<SysMenu> listGrantedMenus(Long userId) {
        // 1. 用户 → 角色 id；无角色则无菜单
        List<Long> roleIds = sysRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty())
            return Collections.emptyList();
        // 2. 任一管理员角色 → 全部菜单
        List<SysRole> roleData = sysRoleMapper.selectByIds(roleIds);

        boolean isAdmin = roleData.stream()
                .anyMatch(role -> Boolean.TRUE.equals(role.getAdministrator()));
        if (isAdmin) {
            return sysMenuMapper.selectList(null);
        }

        // 3. 汇总各角色授权的菜单 id（Set 去重）
        Set<Long> menuIds = new HashSet<>(sysMenuMapper.selectMenuIdsByRoleIds(roleIds));

        // 4. 无菜单 id 则空列表，否则按 id 加载菜单
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysMenuMapper.selectByIds(menuIds);
    }

    public List<SysMenu> listVisibleMenus(Long userId) {
        // 先拿「有权限的叶子/中间节点」；树形展示还需要祖先节点
        List<SysMenu> menus = listGrantedMenus(userId);

        // LinkedHashMap：按 id 去重，并保留插入顺序
        Map<Long, SysMenu> resultMap = new LinkedHashMap<>();
        for (SysMenu m : menus) {
            resultMap.put(m.getId(), m);
        }

        // 逐层向上补父菜单：子节点在 map 里但 parent 不在时，查库并入 map，直到根或全部补齐
        while (true) {
            Set<Long> parentIds = resultMap.values().stream()
                    .map(SysMenu::getParentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            parentIds.removeAll(resultMap.keySet());
            if (parentIds.isEmpty())break; //没有非空parentId了 break

            List<SysMenu> parents = sysMenuMapper.selectByIds(parentIds);
            if (parents.isEmpty())break; // 根节点没有parent 这里要break

            for (SysMenu p : parents) {
                resultMap.put(p.getId(), p);
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    public UserAccess loadAccess(Long userId) {

        List<SysMenu> visible = listVisibleMenus(userId);
        // 把button权限级别的PermCode 装起来  他们不用做树状结构化
        List<String> buttonList = visible.stream()
                .filter(menu -> "BUTTON".equals(menu.getType()))
                .map(SysMenu::getPermCode)
                .filter(Objects::nonNull)
                .toList();

        // 升序排列 只保留菜单级别的数据
        List<SysMenu> menuList = visible.stream()
                .filter(menu -> "MENU".equals(menu.getType()))
                .sorted(Comparator.comparing(SysMenu::getSort))
                .toList();

        // 把数据丢到map里面 准备塞children 这里用hashmap id是menu实体的id，方便查询menu的父节点
        Map<Long,MenuNode> treeList = new HashMap<>();
        for (SysMenu menu : menuList) {
            MenuNode node = new MenuNode(menu.getId(),menu.getName(), menu.getPath(), new ArrayList<>());
            treeList.put(menu.getId(),node);
        }

        List<MenuNode> menus = new ArrayList<>();
        for (SysMenu m : menuList) {
            Long parentId =  m.getParentId();
            Long id = m.getId();

            MenuNode Item = treeList.get(id);
            MenuNode parentItem = treeList.get(parentId);

            if(m.getParentId() == null || !treeList.containsKey(parentId)) {
                menus.add(Item);
            }else{
                parentItem.children().add(Item);
            }
        }
        return new UserAccess(menus,buttonList);
    }
    public boolean hasPerm(Long userId, String code) {
        return loadAccess(userId).permCodes().contains(code);
    }
}
