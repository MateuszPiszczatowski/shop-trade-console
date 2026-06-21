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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
        Delivery delivery = selected.get();
        System.out.println(Formatters.deliveryDetail(delivery));
        try {
            Boolean inFull = askArrivedInFull();
            if (inFull == null) {
                System.out.println("Anulowano.");
                return;
            }
            if (inFull) {
                tradeService.confirmDelivery(delivery.getId(), session.getCurrentUser());
                System.out.println("Potwierdzono dostarczenie w całości — stan magazynu zaktualizowany.");
            } else {
                List<OperationLine> received = readReceivedLines(delivery);
                if (received.isEmpty()) {
                    System.out.println("Żadna pozycja nie dotarła — dostawa pozostaje nadana (niepotwierdzona).");
                    return;
                }
                tradeService.confirmPartialDelivery(delivery.getId(), received, session.getCurrentUser());
                System.out.println("Potwierdzono dostawę niepełną — zapisano faktycznie dostarczone ilości, "
                        + "stan magazynu zaktualizowany.");
            }
        } catch (SecurityException e) {
            System.out.println("ODMOWA DOSTĘPU: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void listDeliveries() {
        PaginatedSelector.browse("Lista dostaw", tradeRepository.findDeliveries(),
                Formatters::renderDelivery, Formatters::deliveryDetail);
    }

    /**
     * Reads delivery lines using product references from the order — the user
     * supplies only product name and quantity; the price snapshot is copied
     * from the corresponding order line. The order contents and how much is
     * still left to dispatch are printed up front and reprinted after every
     * entered line, and an over-dispatch is rejected per-line (rather than
     * letting the service abort the whole delivery).
     */
    private List<OperationLine> readDeliveryLines(Order order) {
        Map<Product, Integer> ordered = orderedMap(order);
        Map<Product, Integer> dispatched = alreadyDispatchedFor(order.getId());
        Map<Product, Integer> entered = new LinkedHashMap<>();
        printRemaining(ordered, dispatched, entered);

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
            Product product = ordered.keySet().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
            if (product == null) {
                System.out.println("BŁĄD: produkt '" + name + "' nie znajduje się w zamówieniu — pozycja pominięta.");
                continue;
            }
            int remaining = ordered.get(product)
                    - dispatched.getOrDefault(product, 0)
                    - entered.getOrDefault(product, 0);
            if (remaining <= 0) {
                System.out.println("BŁĄD: produkt '" + product.getName()
                        + "' został już w całości nadany — pozycja pominięta.");
                continue;
            }
            try {
                int qty = InputReader.readPositiveInt("Ilość (pozostało do nadania: " + remaining + "): ");
                if (qty > remaining) {
                    System.out.println("BŁĄD: pozostało do nadania tylko " + remaining + " szt. — pozycja pominięta.");
                    continue;
                }
                lines.add(new OperationLine(product, qty));
                entered.merge(product, qty, Integer::sum);
                printRemaining(ordered, dispatched, entered);
            } catch (IllegalArgumentException e) {
                System.out.println("BŁĄD: " + e.getMessage() + " — pozycja pominięta.");
            }
        }
        return lines;
    }

    /**
     * For a delivery that arrived incomplete, asks per dispatched line how many
     * units actually arrived (Enter = the full dispatched amount). Lines for
     * which nothing arrived are dropped. The resulting list is what the
     * delivery will be rewritten to.
     */
    private List<OperationLine> readReceivedLines(Delivery dispatched) {
        System.out.println("\nPodaj, ile faktycznie dotarło z każdej nadanej pozycji:");
        List<OperationLine> received = new ArrayList<>();
        for (OperationLine sent : dispatched.getLines()) {
            Product p = sent.getProduct();
            int max = sent.getQuantity();
            while (true) {
                System.out.print("  " + p.getName() + " — nadano " + max
                        + " szt., dotarło (Enter = " + max + "): ");
                System.out.flush();
                String in = ConsoleIo.readLine();
                int qty;
                if (in == null || in.isBlank()) {
                    qty = max;
                } else {
                    try {
                        qty = Integer.parseInt(in.trim());
                    } catch (NumberFormatException e) {
                        System.out.println("  Niepoprawna liczba — spróbuj ponownie.");
                        continue;
                    }
                }
                if (qty < 0) {
                    System.out.println("  Wartość nie może być ujemna.");
                    continue;
                }
                if (qty > max) {
                    System.out.println("  Nie mogło dotrzeć więcej niż nadano (" + max + ").");
                    continue;
                }
                if (qty > 0) received.add(new OperationLine(p, qty));
                break;
            }
        }
        return received;
    }

    /** @return TRUE = full, FALSE = partial, null = cancel. */
    private Boolean askArrivedInFull() {
        while (true) {
            System.out.print("Czy dostawa dotarła w całości? [T/n] (puste = anuluj): ");
            System.out.flush();
            String in = ConsoleIo.readLine();
            if (in == null || in.isBlank()) return null;
            String v = in.trim().toLowerCase();
            if (v.equals("t") || v.equals("tak")) return Boolean.TRUE;
            if (v.equals("n") || v.equals("nie")) return Boolean.FALSE;
            System.out.println("Wpisz T (tak) lub N (nie).");
        }
    }

    private void printRemaining(Map<Product, Integer> ordered,
                                Map<Product, Integer> dispatched,
                                Map<Product, Integer> entered) {
        System.out.println("\nZawartość zamówienia — pozostało do nadania:");
        for (Map.Entry<Product, Integer> e : ordered.entrySet()) {
            Product p = e.getKey();
            int prev = dispatched.getOrDefault(p, 0);
            int now = entered.getOrDefault(p, 0);
            int rem = e.getValue() - prev - now;
            StringBuilder sb = new StringBuilder("  - " + p.getName() + ": zamówiono " + e.getValue());
            if (prev > 0) sb.append(", nadano wcześniej ").append(prev);
            if (now > 0) sb.append(", w tej dostawie ").append(now);
            sb.append("  => pozostało ").append(rem);
            System.out.println(sb);
        }
    }

    private static Map<Product, Integer> orderedMap(Order order) {
        Map<Product, Integer> map = new LinkedHashMap<>();
        for (OperationLine l : order.getLines()) {
            map.merge(l.getProduct(), l.getQuantity(), Integer::sum);
        }
        return map;
    }

    private Map<Product, Integer> alreadyDispatchedFor(UUID orderId) {
        Map<Product, Integer> map = new LinkedHashMap<>();
        for (Delivery d : tradeRepository.findDeliveries()) {
            if (d.getOrderId().equals(orderId)) {
                for (OperationLine l : d.getLines()) {
                    map.merge(l.getProduct(), l.getQuantity(), Integer::sum);
                }
            }
        }
        return map;
    }
}
