package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;

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
            System.out.println("=== MENU GŁÓWNE ===");
            System.out.println("1. Zaloguj");
            System.out.println("2. Wyjście");
            System.out.print("Wybierz opcję: ");

            String choice = System.console().readLine();

            switch (choice) {
                case "1":
                    handleLogin();
                    break;
                case "2":
                    running = false;
                    break;
                default:
                    System.out.println("Niepoprawny wybór.");
            }
        }
        System.out.println("Zamykanie programu...");
    }

    private void handleLogin() {
        System.out.print("Login: ");
        String login = System.console().readLine();
        System.out.print("Hasło: ");
        String password = System.console().readLine();

        var user = userService.login(login, password);
        if (user == null) {
            System.out.println("Błędne dane logowania");
        } else {
            session.login(user);
            System.out.println("Zalogowano jako: " + user.getUsername());
        }
    }

    ;
}
