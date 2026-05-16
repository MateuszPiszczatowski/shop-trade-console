package local.pk154938.shop.config;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.repository.ProductRepository;
import local.pk154938.shop.application.repository.StockRepository;
import local.pk154938.shop.application.repository.TradeRepository;
import local.pk154938.shop.application.repository.UserRepository;
import local.pk154938.shop.application.service.TradeService;
import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.infrastructure.persistence.InMemoryProductRepository;
import local.pk154938.shop.infrastructure.persistence.InMemoryStockRepository;
import local.pk154938.shop.infrastructure.persistence.InMemoryUserRepository;
import local.pk154938.shop.infrastructure.persistence.JsonTradeRepository;
import local.pk154938.shop.ui.menu.MainMenu;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    /**
     * Prints the exit message, waits for the user to press Enter (so a
     * terminal that auto-closes still gives a chance to read the cause),
     * and terminates the JVM with the given exit code.
     */
    public static void terminate(ExitCode exitCode) {
        System.err.println("ZAMYKANIE: " + exitCode.getMessage());
        if (System.console() != null) {
            System.out.print("Naciśnij Enter, aby zamknąć: ");
            System.out.flush();
            System.console().readLine();
        }
        System.exit(exitCode.getCode());
    }

    public static void main(String[] args) {
        Session session = new Session();
        UserRepository userRepository = new InMemoryUserRepository();

        DataSeeder dataSeeder = new DataSeeder(userRepository);
        try {
            dataSeeder.seedAdminIfMissing();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            terminate(ExitCode.INVALID_CONFIG);
            return;
        }

        Path operationsRoot;
        try {
            Path jarPath = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            operationsRoot = jarPath.getParent().resolve("operations");
        } catch (URISyntaxException | NullPointerException e) {
            System.out.println("Nie można wyznaczyć katalogu pliku wykonywalnego: " + e.getMessage());
            terminate(ExitCode.INVALID_CONFIG);
            return;
        }

        TradeRepository tradeRepository;
        ProductRepository productRepository = new InMemoryProductRepository();
        StockRepository stockRepository = new InMemoryStockRepository();
        try {
            tradeRepository = new JsonTradeRepository(operationsRoot);
            new TradeBootstrapper(tradeRepository, productRepository, stockRepository).bootstrap();
        } catch (RuntimeException e) {
            System.out.println("Nie udało się wczytać danych handlowych: " + e.getMessage());
            terminate(ExitCode.DATA_ERROR);
            return;
        }

        AuthorizationService authService = new AuthorizationService();
        UserService userService = new UserService(userRepository, authService);
        TradeService tradeService = new TradeService(tradeRepository, productRepository, stockRepository, authService);

        MainMenu menu = new MainMenu(userService, tradeService,
                productRepository, stockRepository, tradeRepository,
                session, authService);
        menu.show();
    }
}
