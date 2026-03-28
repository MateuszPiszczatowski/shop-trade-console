package local.pk154938.shop.domain.user;

import java.util.Set;

public enum Role {
    ADMIN(Set.of(Permission.values())),
    MANAGER(Set.of(
            Permission.PROCESS_TRADE,
            Permission.VIEW_USERS,
            Permission.MANAGE_EMPLOYEES
    )),
    EMPLOYEE(Set.of(
            Permission.PROCESS_TRADE
    ));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions){
        this.permissions = permissions;
    }

    public boolean hasPermission(Permission p){
        return permissions.contains((p));
    }
}
