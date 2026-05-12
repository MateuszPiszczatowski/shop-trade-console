package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.user.Role;
import local.pk154938.shop.util.SecurityUtils;


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
        addOption("Modyfikuj użytkownika", this::changeOtherUser, Operation.MODIFY_EMPLOYEE, Operation.MODIFY_MANAGER, Operation.MODIFY_ADMIN);
    }

    private static final String SELF_OPERATION_BLOCKED =
            "Nie możesz modyfikować własnego konta z poziomu zarządzania użytkownikami. Użyj menu Zarządzanie kontem.";

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
        if (login.equals(session.getCurrentUser().getUsername())) {
            System.out.println(SELF_OPERATION_BLOCKED);
            return;
        }

        try {
            userService.removeUser(login, session.getCurrentUser());
            System.out.println("Pomyślnie usunięto użytkownika: " + login);
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

    private void changeOtherUser() {
        System.out.print("Podaj login użytkownika do modyfikacji: ");
        String login = System.console().readLine();
        if (login.isBlank()) {
            System.out.println("Anulowano.");
            return;
        }

        if (login.equals(session.getCurrentUser().getUsername())) {
            System.out.println(SELF_OPERATION_BLOCKED);
            return;
        }

        System.out.println("Wybierz co chcesz zmienić: 1. Nazwa użytkownika | 2. Hasło");
        System.out.print("Wybór: ");
        String choice = System.console().readLine();

        try {
            if ("1".equals(choice)) {
                System.out.print("Podaj nową nazwę użytkownika: ");
                String newLogin = System.console().readLine();
                if (newLogin.isBlank()) return;
                userService.changeUsername(login, newLogin, session.getCurrentUser());
                System.out.println("Pomyślnie zmieniono nazwę użytkownika.");
            } else if ("2".equals(choice)) {
                System.out.print("Podaj nowe hasło: ");
                String pass = new String(System.console().readPassword());
                if (!SecurityUtils.isPasswordStrong(pass)) {
                    System.out.println("BŁĄD: Hasło nie spełnia wymogów bezpieczeństwa.");
                    return;
                }
                userService.changePassword(login, pass, session.getCurrentUser());
                System.out.println("Pomyślnie zmieniono hasło.");
            } else {
                System.out.println("Niepoprawny wybór.");
            }
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

}