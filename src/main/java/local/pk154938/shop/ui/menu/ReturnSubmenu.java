package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.repository.StockRepository;
import local.pk154938.shop.application.repository.TradeRepository;
import local.pk154938.shop.application.service.TradeService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.trade.OperationLine;
import local.pk154938.shop.domain.trade.Order;
import local.pk154938.shop.domain.trade.OrderStatus;
import local.pk154938.shop.domain.trade.Product;
import local.pk154938.shop.domain.trade.Return;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReturnSubmenu extends BaseMenu {
    private final TradeService tradeService;
    private final StockRepository stockRepository;
    private final TradeRepository tradeRepository;

    public ReturnSubmenu(TradeService tradeService,
                         StockRepository stockRepository,
                         TradeRepository tradeRepository,
                         Session session, AuthorizationService authorizationService) {
        super("ZWROTY", session, authorizationService);
        this.tradeService = tradeService;
        this.stockRepository = stockRepository;
        this.tradeRepository = tradeRepository;
    }

    @Override
    protected void addOptions() {
        addOption("Dokonaj zwrotu", this::recordReturn, Operation.MAKE_RETURN);
        addOption("Lista zwrotów", this::listReturns, Operation.VIEW_TRADE_INFO);
    }

    private void recordReturn() {
        List<Order> returnable = tradeRepository.findOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.PARTIAL || o.getStatus() == OrderStatus.FULFILLED)
                .toList();
        Optional<Order> selected = PaginatedSelector.selectOne(
                "Wybierz zamówienie, do którego dokonujesz zwrotu", returnable, Formatters::renderOrder);
        if (selected.isEmpty()) {
            System.out.println("Anulowano.");
            return;
        }
        Order order = selected.get();
        try {
            List<OperationLine> lines = readReturnLines(order);
            Return ret = tradeService.recordReturn(order.getId(), lines, session.getCurrentUser());
            System.out.println("Zarejestrowano zwrot. ID: " + ret.getId());
        } catch (InputReader.CancelledException e) {
            System.out.println("Anulowano.");
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void listReturns() {
        PaginatedSelector.display("Lista zwrotów", tradeRepository.findReturns(), Formatters::renderReturn);
    }

    /**
     * Reads return lines using product references from the order — return
     * price snapshot is taken from the order line (i.e. the original price
     * at which the product was purchased).
     */
    private List<OperationLine> readReturnLines(Order order) {
        List<OperationLine> lines = new ArrayList<>();
        while (true) {
            String name;
            try {
                name = InputReader.readNonBlankString(
                        "\nNazwa produktu z zamówienia (puste = zakończ wprowadzanie): ");
            } catch (InputReader.CancelledException e) {
                if (lines.isEmpty()) throw e;
                break;
            }
            Product product = order.getLines().stream()
                    .map(OperationLine::getProduct)
                    .filter(p -> p.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
            if (product == null) {
                System.out.println("BŁĄD: produkt '" + name + "' nie znajduje się w zamówieniu — pozycja pominięta.");
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
