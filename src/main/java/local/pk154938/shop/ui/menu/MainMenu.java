package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.repository.ProductRepository;
import local.pk154938.shop.application.repository.StockRepository;
import local.pk154938.shop.application.repository.TradeRepository;
import local.pk154938.shop.application.service.TradeService;
import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.user.User;

import java.util.Optional;

public class MainMenu extends BaseMenu {
    private final UserService userService;
    private final TradeService tradeService;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final TradeRepository tradeRepository;

    public MainMenu(UserService userService,
                    TradeService tradeService,
                    ProductRepository productRepository,
                    StockRepository stockRepository,
                    TradeRepository tradeRepository,
                    Session session,
                    AuthorizationService authorizationService) {
        super("Menu główne", session, authorizationService);
        this.userService = userService;
        this.tradeService = tradeService;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.tradeRepository = tradeRepository;
    }

    private void handleLogin() {
        System.out.print("Login: ");
        String login = ConsoleIo.readLine();
        System.out.print("Hasło: ");
        String password = new String(ConsoleIo.readPassword());

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
        new TradeMenu(tradeService, productRepository, stockRepository, tradeRepository,
                session, authorizationService).show();
    }


    @Override
    protected void addOptions() {
        if (session.isLoggedIn()) {
            addOption("Zarządzanie użytkownikami", this::enterUserManagement,
                    Operation.VIEW_USER_LIST, Operation.ADD_EMPLOYEE, Operation.REMOVE_EMPLOYEE,
                    Operation.ADD_MANAGER, Operation.REMOVE_MANAGER,
                    Operation.ADD_ADMIN, Operation.REMOVE_ADMIN);
            addOption("Operacje handlowe", this::enterTradeMenu,
                    Operation.VIEW_TRADE_INFO, Operation.MAKE_SALE, Operation.MAKE_RETURN,
                    Operation.REGISTER_DELIVERY, Operation.PLACE_SUPPLIER_ORDER);
            addOption("Zarządzanie kontem", this::enterAccountManagement, Operation.AUTHENTICATED);
            addOption("Wyloguj", this::logout, Operation.AUTHENTICATED);
        } else {
            addOption("Zaloguj", this::handleLogin, Operation.ANONYMOUS);
        }
    }
}
