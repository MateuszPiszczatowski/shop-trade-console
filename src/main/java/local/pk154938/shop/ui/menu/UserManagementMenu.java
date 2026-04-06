package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.user.Role;

public class UserManagementMenu {
    private final UserService userService;
    private final Session session;

    public UserManagementMenu(UserService userService, Session session) {
        this.userService = userService;
        this.session = session;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- ZARZĄDZANIE UŻYTKOWNIKAMI ---");
            System.out.println("1. Lista | 2. Dodaj | 0. Powrót");
            String choice = System.console().readLine();
            switch (choice) {
                case "1":
                    list();
                    break;
                case "2":
                    add();
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Niewłaściwa opcja");
            }
        }
    }

    private void list() {
        userService.getAllUsers().forEach(u ->
                System.out.println("- " + u.getUsername() + " (Klasa: " + u.getClass().getSimpleName() + ")"));
    }

    private void add() {
        System.out.println("Podal login: ");
        String login = System.console().readLine();
        System.out.println("Podaj hasło: ");
        String pass = System.console().readLine();
        boolean running = true;
        Role role = null;
        while (running) {
            running = false;
            System.out.println("Rola: 1. ADMIN, 2. MANAGER, 3. EMPLOYEE\nWybór: ");
            String r = System.console().readLine();
            switch (r) {
                case "1":
                    role = Role.ADMIN;
                    break;
                case "2":
                    role = Role.MANAGER;
                    break;
                case "3":
                    role = Role.EMPLOYEE;
                    break;
                default:
                    System.out.println("Rola nie istnieje, powrót do menu zarządzania użytkownikami");
                    running = true;
            }
        }

        try {
            userService.createAndAddUser(login, pass, role, session.getCurrentUser());
            System.out.println("Dodano pomyślnie.");
        } catch (IllegalStateException e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }
}
