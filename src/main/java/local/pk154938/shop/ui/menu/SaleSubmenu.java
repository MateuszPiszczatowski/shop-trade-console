package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.repository.ProductRepository;
import local.pk154938.shop.application.repository.StockRepository;
import local.pk154938.shop.application.repository.TradeRepository;
import local.pk154938.shop.application.service.TradeService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.trade.OperationLine;
import local.pk154938.shop.domain.trade.Product;
import local.pk154938.shop.domain.trade.Sale;

import java.util.ArrayList;
import java.util.List;

public class SaleSubmenu extends BaseMenu {
    private final TradeService tradeService;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final TradeRepository tradeRepository;

    public SaleSubmenu(TradeService tradeService,
                       ProductRepository productRepository,
                       StockRepository stockRepository,
                       TradeRepository tradeRepository,
                       Session session, AuthorizationService authorizationService) {
        super("SPRZEDAŻ", session, authorizationService);
        this.tradeService = tradeService;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.tradeRepository = tradeRepository;
    }

    @Override
    protected void addOptions() {
        addOption("Dokonaj sprzedaży", this::recordSale, Operation.MAKE_SALE);
        addOption("Lista sprzedaży", this::listSales, Operation.VIEW_TRADE_INFO);
    }

    private void recordSale() {
        try {
            List<OperationLine> lines = readSaleLines();
            Sale sale = tradeService.recordSale(lines, session.getCurrentUser());
            System.out.println("Zarejestrowano sprzedaż. ID: " + sale.getId());
        } catch (InputReader.CancelledException e) {
            System.out.println("Anulowano.");
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void listSales() {
        PaginatedSelector.display("Lista sprzedaży", tradeRepository.findSales(), Formatters::renderSale);
    }

    private List<OperationLine> readSaleLines() {
        List<OperationLine> lines = new ArrayList<>();
        while (true) {
            String name;
            try {
                name = InputReader.readNonBlankString(
                        "\nNazwa produktu z katalogu (puste = zakończ wprowadzanie): ");
            } catch (InputReader.CancelledException e) {
                if (lines.isEmpty()) throw e;
                break;
            }
            Product product = productRepository.findByName(name).orElse(null);
            if (product == null) {
                System.out.println("BŁĄD: produkt '" + name + "' nie istnieje w katalogu — pozycja pominięta.");
                continue;
            }
            int available = stockRepository.getQuantity(product);
            if (available == 0) {
                System.out.println("BŁĄD: produkt '" + product.getName() + "' nie jest dostępny w magazynie — pozycja pominięta.");
                continue;
            }
            try {
                int qty = InputReader.readPositiveInt("Ilość (dostępne: " + available + "): ");
                lines.add(new OperationLine(product, qty));
            } catch (IllegalArgumentException e) {
                System.out.println("BŁĄD: " + e.getMessage() + " — pozycja pominięta.");
            }
        }
        return lines;
    }
}
