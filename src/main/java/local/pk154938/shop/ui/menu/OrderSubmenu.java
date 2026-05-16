package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.repository.TradeRepository;
import local.pk154938.shop.application.service.TradeService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.trade.OperationLine;
import local.pk154938.shop.domain.trade.Order;
import local.pk154938.shop.domain.trade.OrderStatus;
import local.pk154938.shop.domain.trade.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderSubmenu extends BaseMenu {
    private final TradeService tradeService;
    private final TradeRepository tradeRepository;

    public OrderSubmenu(TradeService tradeService, TradeRepository tradeRepository,
                        Session session, AuthorizationService authorizationService) {
        super("ZAMÓWIENIA", session, authorizationService);
        this.tradeService = tradeService;
        this.tradeRepository = tradeRepository;
    }

    @Override
    protected void addOptions() {
        addOption("Złóż zamówienie u dostawcy", this::placeOrder, Operation.PLACE_SUPPLIER_ORDER);
        addOption("Anuluj zamówienie", this::cancelOrder, Operation.PLACE_SUPPLIER_ORDER);
        addOption("Lista zamówień", this::listOrders, Operation.VIEW_TRADE_INFO);
    }

    private void placeOrder() {
        try {
            String supplier = InputReader.readNonBlankString("Nazwa dostawcy: ");
            List<OperationLine> lines = readNewLines();
            Order order = tradeService.placeOrder(supplier, lines, session.getCurrentUser());
            System.out.println("Złożono zamówienie. ID: " + order.getId());
        } catch (InputReader.CancelledException e) {
            System.out.println("Anulowano.");
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void cancelOrder() {
        List<Order> cancellable = tradeRepository.findOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.OPEN)
                .collect(Collectors.toList());
        Optional<Order> selected = PaginatedSelector.selectOne(
                "Zamówienia możliwe do anulowania", cancellable, Formatters::renderOrder);
        if (selected.isEmpty()) {
            System.out.println("Anulowano.");
            return;
        }
        try {
            tradeService.cancelOrder(selected.get().getId(), session.getCurrentUser());
            System.out.println("Zamówienie zostało anulowane.");
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void listOrders() {
        PaginatedSelector.display("Lista zamówień", tradeRepository.findOrders(), Formatters::renderOrder);
    }

    private List<OperationLine> readNewLines() {
        List<OperationLine> lines = new ArrayList<>();
        while (true) {
            String name;
            try {
                name = InputReader.readNonBlankString(
                        "\nNazwa produktu (puste = zakończ wprowadzanie): ");
            } catch (InputReader.CancelledException e) {
                if (lines.isEmpty()) throw e;
                break;
            }
            try {
                BigDecimal netPurchase = InputReader.readNonNegativeBigDecimal("Cena netto kupna: ");
                BigDecimal netSale = InputReader.readNonNegativeBigDecimal("Cena netto sprzedaży: ");
                BigDecimal vat = InputReader.readNonNegativeBigDecimal("Stawka VAT (np. 0.23 = 23%): ");
                int qty = InputReader.readPositiveInt("Ilość: ");
                Product product = new Product(name, netPurchase, netSale, vat);
                lines.add(new OperationLine(product, qty));
            } catch (IllegalArgumentException e) {
                System.out.println("BŁĄD: " + e.getMessage() + " — pozycja pominięta.");
            }
        }
        return lines;
    }
}
