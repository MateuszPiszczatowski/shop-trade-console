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
        String password = new String(System.console().readPassword());

        Optional<User> user = userService.login(login, password);
        if (user.isPresent()) {
            session.login(user.get());
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

    private void enterAccountManagement() {
        new AccountManagementMenu(userService, session, authorizationService).show();
    }

    private void enterTradeMenu() {
        new TradeMenu(session, authorizationService).show();
    }


    @Override
    protected void addOptions() {
        if (session.isLoggedIn()) {
            addOption("Zarządzanie użytkownikami", this::enterUserManagement,
                    Operation.VIEW_USER_LIST, Operation.ADD_EMPLOYEE, Operation.REMOVE_EMPLOYEE,
                    Operation.ADD_MANAGER, Operation.REMOVE_MANAGER,
                    Operation.ADD_ADMIN, Operation.REMOVE_ADMIN);
            addOption("Operacje handlowe", this::enterTradeMenu,
                    Operation.VIEW_STOCK, Operation.MAKE_SALE, Operation.MAKE_RETURN,
                    Operation.REGISTER_DELIVERY, Operation.PLACE_SUPPLIER_ORDER);
            addOption("Zarządzanie kontem", this::enterAccountManagement, Operation.AUTHENTICATED);
            addOption("Wyloguj", this::logout, Operation.AUTHENTICATED);
        } else {
            addOption("Zaloguj", this::handleLogin, Operation.ANONYMOUS);
        }
    }
}
