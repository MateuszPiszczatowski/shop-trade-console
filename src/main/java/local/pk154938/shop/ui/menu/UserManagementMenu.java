package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.user.Role;

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
    }

    private void list() {
        System.out.println("\n--- LISTA UŻYTKOWNIKÓW ---");
        userService.getAllUsers().forEach(u ->
                System.out.println("- " + u.getUsername() + " (Klasa: " + u.getClass().getSimpleName() + ")"));
    }

    private void add() {
        System.out.print("Podaj login: ");
        String login = System.console().readLine();
        System.out.print("Podaj hasło: ");
        String pass = Arrays.toString(System.console().readPassword());

        Role role = chooseRole();
        if (role == null) return;

        try {
            userService.createAndAddUser(login, pass, role, session.getCurrentUser());
            System.out.println("Dodano pomyślnie.");
        } catch (IllegalStateException e) {
            System.out.println("BŁĄD: " + e.getMessage());
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