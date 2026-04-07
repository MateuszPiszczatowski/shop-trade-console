package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.user.User;

import java.util.Arrays;
import java.util.Optional;

public class MainMenu extends BaseMenu {
    private final UserService userService;

    public MainMenu(UserService userService, Session session, AuthorizationService authorizationService) {
        super("Menu główne", session, authorizationService);
        this.userService = userService;
    }

    private void handleLogin() {
        System.out.print("Login: ");
        String login = System.console().readLine();
        System.out.print("Hasło: ");
        String password = Arrays.toString(System.console().readPassword());

        Optional<User> user = userService.login(login);
        if (user.isPresent()) {
            session.login(user.get());
            System.out.println("Zalogowano jako: " + user.get().getUsername());
        } else {
            System.out.println("Błędne dane logowania");
        }
    }

    private void logout(){
        session.logout();
        System.out.println("Wylogowano.");
    }

    private void enterUserManagement() {
        new UserManagementMenu(userService, session, authorizationService).show();
    }


    @Override
    protected void addOptions() {
        if (session.isLoggedIn()) {
            addOption("Zarządzanie użytkownikami", this::enterUserManagement, Operation.ENTER_USER_MANAGEMENT);
            addOption("Wyloguj", this::logout, Operation.UNRESTRICTED);
        } else {
            addOption("Zaloguj", this::handleLogin, Operation.UNRESTRICTED);
        }
    }
}
