package local.pk154938.shop.application.auth;

import local.pk154938.shop.domain.user.Permission;
import local.pk154938.shop.domain.user.User;

import java.util.Map;
import java.util.Set;

public class AuthorizationService {
    private final Map<Operation, Set<Permission>> securityMap = Map.ofEntries(
            Map.entry(Operation.ADD_EMPLOYEE, Set.of(Permission.MANAGE_EMPLOYEES)),
            Map.entry(Operation.MODIFY_EMPLOYEE, Set.of(Permission.MANAGE_EMPLOYEES)),
            Map.entry(Operation.REMOVE_EMPLOYEE, Set.of(Permission.MANAGE_EMPLOYEES)),
            Map.entry(Operation.ADD_MANAGER, Set.of(Permission.MANAGE_MANAGERS)),
            Map.entry(Operation.MODIFY_MANAGER, Set.of(Permission.MANAGE_MANAGERS)),
            Map.entry(Operation.REMOVE_MANAGER, Set.of(Permission.MANAGE_MANAGERS)),
            Map.entry(Operation.ADD_ADMIN, Set.of(Permission.MANAGE_ADMINISTRATORS)),
            Map.entry(Operation.MODIFY_ADMIN, Set.of(Permission.MANAGE_ADMINISTRATORS)),
            Map.entry(Operation.REMOVE_ADMIN, Set.of(Permission.MANAGE_ADMINISTRATORS)),
            Map.entry(Operation.VIEW_USER_LIST, Set.of(Permission.VIEW_USERS)),
            Map.entry(Operation.VIEW_STOCK, Set.of(Permission.PROCESS_TRADE)),
            Map.entry(Operation.MAKE_SALE, Set.of(Permission.PROCESS_TRADE)),
            Map.entry(Operation.MAKE_RETURN, Set.of(Permission.PROCESS_TRADE)),
            Map.entry(Operation.PLACE_SUPPLIER_ORDER, Set.of(Permission.PROCESS_TRADE)),
            Map.entry(Operation.REGISTER_DELIVERY, Set.of(Permission.PROCESS_TRADE))
    );

    public boolean isAuthorized(User user, Operation op) {
        if (op == Operation.ANONYMOUS) return true;
        if (user == null) return false;
        if (op == Operation.AUTHENTICATED) return true;

        Set<Permission> requiredPermissions = securityMap.get(op);
        if (requiredPermissions == null || requiredPermissions.isEmpty())
            return true;

        return requiredPermissions.stream()
                .anyMatch(reqPerm -> user.getRoles().stream().anyMatch(role -> role.hasPermission(reqPerm)));
    }
}
