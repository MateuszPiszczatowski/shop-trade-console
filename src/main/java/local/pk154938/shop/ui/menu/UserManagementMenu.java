package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.user.Role;
import local.pk154938.shop.util.SecurityUtils;

import java.util.Arrays;

public class UserManagementMenu extends BaseMenu {
    private final UserService userService;
    private final Session session;

    public UserManagementMenu(UserService userService, Session session, AuthorizationService authorizationService) {
        super("ZARZĄDZANIE UŻYTKOWNIKAMI", session, authorizationService);
        this.userService = userService;
        this.session = session;
    }

    @Override
    protected void addOptions() {
        addOption("Lista użytkowników", this::list, Operation.VIEW_USER_LIST);
        addOption("Dodaj użytkownika", this::add, Operation.ADD_EMPLOYEE);
        addOption("Usuń użytkownika", this::remove, Operation.REMOVE_EMPLOYEE);
    }

    private void list() {
        System.out.println("\n--- LISTA UŻYTKOWNIKÓW ---");
        userService.getAllUsers().forEach(u ->
                System.out.println("- " + u.getUsername() + " (Klasa: " + u.getClass().getSimpleName() + ")"));
    }

    private void add() {
        System.out.print("Podaj login: ");
        String login = System.console().readLine();
        if(login.isBlank())
        {
            System.out.println("Dodawanie anulowane.");
            return;
        }
        System.out.print("Podaj hasło: ");
        String pass = new String(System.console().readPassword());

        if (!SecurityUtils.isPasswordStrong(pass)) {
            System.out.println("BŁĄD: Hasło musi składać się z minimum 8 znaków i zawierać małą literę, wielką literę, cyfrę oraz znak specjalny.");
            return;
        }

        Role role = chooseRole();
        if (role == null) return;

        try {
            userService.createAndAddUser(login, pass, role, session.getCurrentUser());
            System.out.println("Dodano pomyślnie.");
        } catch (IllegalStateException e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void remove() {
        System.out.print("Podaj login użytkownika do usunięcia: ");
        String login = System.console().readLine();
        if(login.isBlank())
        {
            System.out.println("Usuwanie anulowane.");
            return;
        }
        boolean isSelfDeletion = login.equals(session.getCurrentUser().getUsername());
        
        if (isSelfDeletion) {
            System.out.print("Czy na pewno chcesz usunąć własne konto? Operacja jest nieodwracalna. (T/N): ");
            String confirm = System.console().readLine();
            if (!confirm.equalsIgnoreCase("T")) {
                System.out.println("Anulowano usuwanie konta.");
                return;
            }
        }

        try {
            userService.removeUser(login, session.getCurrentUser());
            System.out.println("Pomyślnie usunięto użytkownika: " + login);
            if (isSelfDeletion) {
                session.logout();
                System.out.println("Zostałeś wylogowany, ponieważ Twoje konto zostało usunięte.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("BŁĄD: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        }
    }

    private Role chooseRole() {
        System.out.println("Wybierz rolę: 1. ADMIN | 2. MANAGER | 3. EMPLOYEE");
        String r = System.console().readLine();
        switch (r) {
            case "1":
                return Role.ADMIN;
            case "2":
                return Role.MANAGER;
            case "3":
                return Role.EMPLOYEE;
            default:
                System.out.println("Niepoprawna rola.");
                return null;
        }
    }
}