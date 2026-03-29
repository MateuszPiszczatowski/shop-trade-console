package local.pk154938.shop.application.auth;

import local.pk154938.shop.domain.user.Permission;
import local.pk154938.shop.domain.user.User;

import java.util.Map;

public class AuthorizationService {
    private final Map<Operation, Permission> securityMap = Map.ofEntries(
            Map.entry(Operation.ADD_EMPLOYEE, Permission.MANAGE_EMPLOYEES),
            Map.entry(Operation.MODIFY_EMPLOYEE, Permission.MANAGE_EMPLOYEES),
            Map.entry(Operation.REMOVE_EMPLOYEE, Permission.MANAGE_EMPLOYEES),
            Map.entry(Operation.ADD_MANAGER, Permission.MANAGE_MANAGERS),
            Map.entry(Operation.MODIFY_MANAGER, Permission.MANAGE_MANAGERS),
            Map.entry(Operation.REMOVE_MANAGER, Permission.MANAGE_MANAGERS),
            Map.entry(Operation.ADD_ADMIN, Permission.MANAGE_ADMINISTRATORS),
            Map.entry(Operation.MODIFY_ADMIN, Permission.MANAGE_ADMINISTRATORS),
            Map.entry(Operation.REMOVE_ADMIN, Permission.MANAGE_ADMINISTRATORS),
            Map.entry(Operation.VIEW_USER_LIST, Permission.VIEW_USERS),
            Map.entry(Operation.VIEW_STOCK, Permission.PROCESS_TRADE),
            Map.entry(Operation.MAKE_SALE, Permission.PROCESS_TRADE),
            Map.entry(Operation.MAKE_RETURN, Permission.PROCESS_TRADE),
            Map.entry(Operation.PLACE_SUPPLIER_ORDER, Permission.PROCESS_TRADE),
            Map.entry(Operation.REGISTER_DELIVERY, Permission.PROCESS_TRADE)
    );
    public boolean isAuthorized(User user, Operation op){
        Permission required = securityMap.get(op);
        if(required==null)
            return true;
        return user.getRoles().stream().anyMatch(role -> role.hasPermission(required));
    }
}
