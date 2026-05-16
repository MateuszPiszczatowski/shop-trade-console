package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.repository.TradeRepository;
import local.pk154938.shop.application.service.TradeService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.trade.Delivery;
import local.pk154938.shop.domain.trade.DeliveryStatus;
import local.pk154938.shop.domain.trade.OperationLine;
import local.pk154938.shop.domain.trade.Order;
import local.pk154938.shop.domain.trade.OrderStatus;
import local.pk154938.shop.domain.trade.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DeliverySubmenu extends BaseMenu {
    private final TradeService tradeService;
    private final TradeRepository tradeRepository;

    public DeliverySubmenu(TradeService tradeService, TradeRepository tradeRepository,
                           Session session, AuthorizationService authorizationService) {
        super("DOSTAWY", session, authorizationService);
        this.tradeService = tradeService;
        this.tradeRepository = tradeRepository;
    }

    @Override
    protected void addOptions() {
        addOption("Zarejestruj dostawę nadaną", this::registerDispatched, Operation.REGISTER_DELIVERY);
        addOption("Potwierdź dostarczenie", this::confirmDelivery, Operation.REGISTER_DELIVERY);
        addOption("Lista dostaw", this::listDeliveries, Operation.VIEW_TRADE_INFO);
    }

    private void registerDispatched() {
        List<Order> open = tradeRepository.findOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.OPEN || o.getStatus() == OrderStatus.PARTIAL)
                .collect(Collectors.toList());
        Optional<Order> selected = PaginatedSelector.selectOne(
                "Wybierz zamówienie do realizacji dostawy", open, Formatters::renderOrder);
        if (selected.isEmpty()) {
            System.out.println("Anulowano.");
            return;
        }
        Order order = selected.get();
        try {
            List<OperationLine> lines = readDeliveryLines(order);
            Delivery delivery = tradeService.registerDispatchedDelivery(
                    order.getId(), lines, session.getCurrentUser());
            System.out.println("Zarejestrowano dostawę nadaną. ID: " + delivery.getId());
        } catch (InputReader.CancelledException e) {
            System.out.println("Anulowano.");
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void confirmDelivery() {
        List<Delivery> dispatched = tradeRepository.findDeliveries().stream()
                .filter(d -> d.getStatus() == DeliveryStatus.DISPATCHED)
                .collect(Collectors.toList());
        Optional<Delivery> selected = PaginatedSelector.selectOne(
                "Dostawy oczekujące na potwierdzenie", dispatched, Formatters::renderDelivery);
        if (selected.isEmpty()) {
            System.out.println("Anulowano.");
            return;
        }
        try {
            tradeService.confirmDelivery(selected.get().getId(), session.getCurrentUser());
            System.out.println("Potwierdzono dostarczenie — stan magazynu zaktualizowany.");
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void listDeliveries() {
        PaginatedSelector.display("Lista dostaw", tradeRepository.findDeliveries(), Formatters::renderDelivery);
    }

    /**
     * Reads delivery lines using product references from the order — the
     * user supplies only product name and quantity; the price snapshot is
     * copied from the corresponding order line.
     */
    private List<OperationLine> readDeliveryLines(Order order) {
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
            try {
                int qty = InputReader.readPositiveInt("Ilość: ");
                lines.add(new OperationLine(product, qty));
            } catch (IllegalArgumentException e) {
                System.out.println("BŁĄD: " + e.getMessage() + " — pozycja pominięta.");
            }
        }
        return lines;
    }
}
