package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.user.User;

import java.util.Optional;

public class MainMenu {
    private final UserService userService;
    private final Session session;

    public MainMenu(UserService userService, Session session) {
        this.userService = userService;
        this.session = session;
    }

    public void show() {
        boolean running = true;

        while (running) {
            if (session.isLoggedIn()) {
                System.out.println("1. Wyloguj");
                System.out.println("2. Zarządzanie użytkownikami");
                System.out.println("0. Wyjście");
                System.out.print("Wybierz opcję: ");

                String choice = System.console().readLine();
                switch (choice) {
                    case "1":
                        session.logout();
                        System.out.println("Wylogowano.");
                        break;
                    case "2":
                        new UserManagementMenu(userService, session).show();
                        break;
                    case "0":
                        running = false;
                        break;
                }
            } else {
                System.out.println("=== MENU GŁÓWNE ===");
                System.out.println("1. Zaloguj");
                System.out.println("0. Wyjście");
                System.out.print("Wybierz opcję: ");

                String choice = System.console().readLine();

                switch (choice) {
                    case "1":
                        handleLogin();
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Niepoprawny wybór.");
                }
            }
        }
        System.out.println("Zamykanie programu...");
    }

    private void handleLogin() {
        System.out.print("Login: ");
        String login = System.console().readLine();
        System.out.print("Hasło: ");
        String password = System.console().readLine();

        Optional<User> user = userService.login(login);
        if (user.isPresent()) {
            session.login(user.get());
            System.out.println("Zalogowano jako: " + user.get().getUsername());
        } else {
            System.out.println("Błędne dane logowania");
        }
    }
}
