package com.velrix.platform.application.access.event;



import com.velrix.platform.application.access.UserAccessCacheInvalidator;
import com.velrix.platform.infrastructure.redis.UserAccessCacheStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class UserAccessCacheEventListener {

    private final UserAccessCacheInvalidator userAccessCacheInvalidator;
    private final UserAccessCacheStore userAccessCacheStore;

    // 更新用户绑定的角色 replacerole 时 令缓存失效
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserAccessEvict(UserAccessEvictEvent event) {
        if(event.userId() == null ) return;
        userAccessCacheInvalidator.invalidateUser(event.userId());
    }
    //更新角色绑定的菜单时 令缓存失效
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleMenuReplaced(RoleMenuReplacedEvent event) {
        if(event.roleId() == null) return;
        userAccessCacheInvalidator.invalidateByRoleId(event.roleId());
    }

    //更新菜单详情时 令缓存失效
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMenuUpdated(MenuChangedEvent event) {
        if(event.menuId() == null) return;
        if(event.userIds().isPresent()){
            userAccessCacheStore.evictAll(event.userIds().get());
            return;
        }
        userAccessCacheInvalidator.invalidateByMenuId(event.menuId());
    }
}
