package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.repository.ProductRepository;
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
    private final ProductRepository productRepository;

    public OrderSubmenu(TradeService tradeService, TradeRepository tradeRepository,
                        ProductRepository productRepository,
                        Session session, AuthorizationService authorizationService) {
        super("ZAMÓWIENIA", session, authorizationService);
        this.tradeService = tradeService;
        this.tradeRepository = tradeRepository;
        this.productRepository = productRepository;
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
        PaginatedSelector.browse("Lista zamówień", tradeRepository.findOrders(),
                Formatters::renderOrder, Formatters::orderDetail);
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
                product = reconcileWithCatalog(product);
                lines.add(new OperationLine(product, qty));
            } catch (IllegalArgumentException e) {
                System.out.println("BŁĄD: " + e.getMessage() + " — pozycja pominięta.");
            }
        }
        return lines;
    }

    /**
     * If the catalog already holds a product with this name but different
     * price/VAT, warns the user and lets them decide: apply the new values
     * (they become the single current snapshot for the whole stock of this
     * product) or keep the existing catalog values for this line. Returns the
     * product that should actually be used.
     */
    private Product reconcileWithCatalog(Product incoming) {
        Product existing = productRepository.findByName(incoming.getName()).orElse(null);
        if (existing == null || !differsInPriceOrVat(existing, incoming)) {
            return incoming;
        }
        System.out.println("\nUWAGA: produkt '" + existing.getName()
                + "' już istnieje w katalogu z innymi parametrami:");
        System.out.println("  dotychczas: netto kupna " + existing.getPriceNetPurchase()
                + ", netto sprzedaży " + existing.getPriceNetSale()
                + ", VAT " + existing.getVatRate());
        System.out.println("  podane:     netto kupna " + incoming.getPriceNetPurchase()
                + ", netto sprzedaży " + incoming.getPriceNetSale()
                + ", VAT " + incoming.getVatRate());
        System.out.println("Zastosowanie nowych wartości zmieni cenę/VAT dla CAŁEGO stanu tego produktu "
                + "(model: jeden produkt = jedna aktualna cena).");
        while (true) {
            System.out.print("Zastosować nowe wartości? [T/n] (n / puste = zachowaj dotychczasowe): ");
            System.out.flush();
            String in = ConsoleIo.readLine();
            if (in == null || in.isBlank() || in.trim().equalsIgnoreCase("n") || in.trim().equalsIgnoreCase("nie")) {
                System.out.println("Zachowano dotychczasowe wartości z katalogu.");
                return existing;
            }
            String v = in.trim().toLowerCase();
            if (v.equals("t") || v.equals("tak")) {
                System.out.println("Zastosowano nowe wartości — cena/VAT zostaną zaktualizowane dla całego stanu.");
                return incoming;
            }
            System.out.println("Wpisz T (tak) lub N (nie).");
        }
    }

    private static boolean differsInPriceOrVat(Product a, Product b) {
        return a.getPriceNetPurchase().compareTo(b.getPriceNetPurchase()) != 0
                || a.getPriceNetSale().compareTo(b.getPriceNetSale()) != 0
                || a.getVatRate().compareTo(b.getVatRate()) != 0;
    }
}
