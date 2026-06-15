package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.user.Role;
import local.pk154938.shop.domain.user.User;
import local.pk154938.shop.util.SecurityUtils;

public class AccountManagementMenu extends BaseMenu {
    private final UserService userService;

    public AccountManagementMenu(UserService userService, Session session, AuthorizationService authorizationService) {
        super("ZARZĄDZANIE KONTEM", session, authorizationService);
        this.userService = userService;
    }

    @Override
    protected void addOptions() {
        addOption("Zmień hasło", this::changePassword, Operation.AUTHENTICATED);

        Operation modifyOp = getSelfModifyOperation();
        if (modifyOp != null)
            addOption("Zmień nazwę użytkownika", this::changeUsername, modifyOp);

        Operation removeOp = getSelfRemoveOperation();
        if (removeOp != null)
            addOption("Usuń konto", this::removeAccount, removeOp);
    }

    private void changePassword() {
        System.out.print("Podaj nowe hasło: ");
        String pass = new String(ConsoleIo.readPassword());
        if (!SecurityUtils.isPasswordStrong(pass)) {
            System.out.println("BŁĄD: Hasło musi składać się z minimum 8 znaków i zawierać małą literę, wielką literę, cyfrę oraz znak specjalny.");
            return;
        }
        try {
            User updated = userService.changePassword(session.getCurrentUser().getUsername(), pass, session.getCurrentUser());
            session.login(updated);
            System.out.println("Twoje hasło zostało pomyślnie zmienione.");
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void changeUsername() {
        System.out.print("Podaj nową nazwę użytkownika: ");
        String newLogin = ConsoleIo.readLine();
        if (newLogin.isBlank()) {
            System.out.println("Anulowano.");
            return;
        }
        try {
            User updated = userService.changeUsername(session.getCurrentUser().getUsername(), newLogin, session.getCurrentUser());
            session.login(updated);
            System.out.println("Twoja nazwa użytkownika została pomyślnie zmieniona.");
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void removeAccount() {
        System.out.print("Czy na pewno chcesz usunąć własne konto? Operacja jest nieodwracalna. (T/N): ");
        String confirm = ConsoleIo.readLine();
        if (!confirm.equalsIgnoreCase("T")) {
            System.out.println("Anulowano usuwanie konta.");
            return;
        }
        try {
            userService.removeUser(session.getCurrentUser().getUsername(), session.getCurrentUser());
            session.logout();
            System.out.println("Twoje konto zostało usunięte. Zostałeś wylogowany.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("BŁĄD: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        }
    }

    private Operation getSelfModifyOperation() {
        User u = session.getCurrentUser();
        if (u == null) return null;
        if (u.getRoles().contains(Role.ADMIN))    return Operation.MODIFY_ADMIN;
        if (u.getRoles().contains(Role.MANAGER))  return Operation.MODIFY_MANAGER;
        if (u.getRoles().contains(Role.EMPLOYEE)) return Operation.MODIFY_EMPLOYEE;
        return null;
    }

    private Operation getSelfRemoveOperation() {
        User u = session.getCurrentUser();
        if (u == null) return null;
        if (u.getRoles().contains(Role.ADMIN))    return Operation.REMOVE_ADMIN;
        if (u.getRoles().contains(Role.MANAGER))  return Operation.REMOVE_MANAGER;
        if (u.getRoles().contains(Role.EMPLOYEE)) return Operation.REMOVE_EMPLOYEE;
        return null;
    }
}
