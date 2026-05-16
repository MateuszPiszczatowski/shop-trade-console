package local.pk154938.shop.application.service;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.repository.ProductRepository;
import local.pk154938.shop.application.repository.StockRepository;
import local.pk154938.shop.application.repository.TradeRepository;
import local.pk154938.shop.domain.trade.Delivery;
import local.pk154938.shop.domain.trade.DeliveryStatus;
import local.pk154938.shop.domain.trade.OperationLine;
import local.pk154938.shop.domain.trade.Order;
import local.pk154938.shop.domain.trade.OrderStatus;
import local.pk154938.shop.domain.trade.Product;
import local.pk154938.shop.domain.trade.Return;
import local.pk154938.shop.domain.trade.Sale;
import local.pk154938.shop.domain.user.User;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service orchestrating all trade-domain mutations. Holds the
 * business rules:
 *
 * <ul>
 *   <li>permission checks per operation via {@link AuthorizationService};</li>
 *   <li>order lifecycle: OPEN → PARTIAL (on first dispatch) → FULFILLED
 *       (when every ordered quantity is fully {@link DeliveryStatus#DELIVERED});
 *       OPEN → CANCELLED (only if no deliveries exist for the order);</li>
 *   <li>dispatched deliveries cannot overflow the order's quantities;</li>
 *   <li>stock is updated only on {@link #confirmDelivery}, {@link #recordSale}
 *       and {@link #recordReturn};</li>
 *   <li>sales and returns require sufficient stock — the service refuses to
 *       drive stock negative;</li>
 *   <li>every operation refreshes the product catalog with its line snapshot
 *       (most recent operation wins).</li>
 * </ul>
 */
public class TradeService {
    private final TradeRepository tradeRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final AuthorizationService authService;

    public TradeService(TradeRepository tradeRepository,
                        ProductRepository productRepository,
                        StockRepository stockRepository,
                        AuthorizationService authService) {
        this.tradeRepository = tradeRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.authService = authService;
    }

    public Order placeOrder(String supplierName, List<OperationLine> lines, User currentUser) {
        requireAuthorized(currentUser, Operation.PLACE_SUPPLIER_ORDER);
        Order order = new Order(UUID.randomUUID(), Instant.now(), lines, supplierName, OrderStatus.OPEN);
        tradeRepository.save(order);
        refreshProductCatalog(lines);
        return order;
    }

    public void cancelOrder(UUID orderId, User currentUser) {
        requireAuthorized(currentUser, Operation.PLACE_SUPPLIER_ORDER);
        Order order = tradeRepository.findOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nie znaleziono zamówienia o ID: " + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED)
            throw new IllegalStateException("Zamówienie jest już anulowane.");
        boolean hasDeliveries = tradeRepository.findDeliveries().stream()
                .anyMatch(d -> d.getOrderId().equals(orderId));
        if (hasDeliveries)
            throw new IllegalStateException(
                    "Nie można anulować zamówienia, dla którego zarejestrowano już dostawę.");
        order.setStatus(OrderStatus.CANCELLED);
        tradeRepository.update(order);
    }

    public Delivery registerDispatchedDelivery(UUID orderId, List<OperationLine> lines, User currentUser) {
        requireAuthorized(currentUser, Operation.REGISTER_DELIVERY);
        Order order = tradeRepository.findOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nie znaleziono zamówienia o ID: " + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED)
            throw new IllegalStateException("Zamówienie jest anulowane — dostawy nie można zarejestrować.");
        if (order.getStatus() == OrderStatus.FULFILLED)
            throw new IllegalStateException("Zamówienie jest już zrealizowane.");

        Map<Product, Integer> ordered = sumByProduct(order.getLines());
        Map<Product, Integer> alreadyDispatched = sumByProduct(allDeliveryLinesFor(orderId));
        Map<Product, Integer> newDispatch = sumByProduct(lines);
        for (Map.Entry<Product, Integer> e : newDispatch.entrySet()) {
            Product p = e.getKey();
            int orderedQty = ordered.getOrDefault(p, 0);
            if (orderedQty == 0)
                throw new IllegalStateException(
                        "Produkt '" + p.getName() + "' nie znajduje się w zamówieniu " + orderId + ".");
            int totalAfter = alreadyDispatched.getOrDefault(p, 0) + e.getValue();
            if (totalAfter > orderedQty)
                throw new IllegalStateException(
                        "Dostawa przepełniłaby zamówienie dla produktu '" + p.getName()
                                + "': zamówione " + orderedQty + ", łącznie po tej dostawie " + totalAfter + ".");
        }

        Delivery delivery = new Delivery(UUID.randomUUID(), Instant.now(), lines, orderId, DeliveryStatus.DISPATCHED);
        tradeRepository.save(delivery);
        if (order.getStatus() == OrderStatus.OPEN) {
            order.setStatus(OrderStatus.PARTIAL);
            tradeRepository.update(order);
        }
        refreshProductCatalog(lines);
        return delivery;
    }

    public void confirmDelivery(UUID deliveryId, User currentUser) {
        requireAuthorized(currentUser, Operation.REGISTER_DELIVERY);
        Delivery delivery = tradeRepository.findDeliveryById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nie znaleziono dostawy o ID: " + deliveryId));
        if (delivery.getStatus() == DeliveryStatus.DELIVERED)
            throw new IllegalStateException("Dostawa jest już potwierdzona jako dostarczona.");

        delivery.setStatus(DeliveryStatus.DELIVERED);
        tradeRepository.update(delivery);
        for (OperationLine line : delivery.getLines()) {
            stockRepository.increase(line.getProduct(), line.getQuantity());
        }
        maybeMarkOrderFulfilled(delivery.getOrderId());
    }

    public Sale recordSale(List<OperationLine> lines, User currentUser) {
        requireAuthorized(currentUser, Operation.MAKE_SALE);
        verifyStockSufficient(lines);
        Sale sale = new Sale(UUID.randomUUID(), Instant.now(), lines);
        tradeRepository.save(sale);
        for (OperationLine line : lines) {
            stockRepository.decrease(line.getProduct(), line.getQuantity());
        }
        refreshProductCatalog(lines);
        return sale;
    }

    public Return recordReturn(UUID orderId, List<OperationLine> lines, User currentUser) {
        requireAuthorized(currentUser, Operation.MAKE_RETURN);
        Order order = tradeRepository.findOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nie znaleziono zamówienia o ID: " + orderId));
        Map<Product, Integer> ordered = sumByProduct(order.getLines());
        for (OperationLine line : lines) {
            if (!ordered.containsKey(line.getProduct()))
                throw new IllegalStateException(
                        "Produkt '" + line.getProduct().getName()
                                + "' nie znajduje się w zamówieniu " + orderId + ".");
        }
        verifyStockSufficient(lines);
        Return ret = new Return(UUID.randomUUID(), Instant.now(), lines, orderId);
        tradeRepository.save(ret);
        for (OperationLine line : lines) {
            stockRepository.decrease(line.getProduct(), line.getQuantity());
        }
        refreshProductCatalog(lines);
        return ret;
    }

    // ---------- helpers ----------

    private void requireAuthorized(User user, Operation op) {
        if (!authService.isAuthorized(user, op))
            throw new SecurityException("Brak uprawnień do operacji: " + op);
    }

    private void refreshProductCatalog(List<OperationLine> lines) {
        for (OperationLine line : lines) {
            productRepository.save(line.getProduct());
        }
    }

    private void verifyStockSufficient(List<OperationLine> lines) {
        Map<Product, Integer> requested = sumByProduct(lines);
        for (Map.Entry<Product, Integer> e : requested.entrySet()) {
            int available = stockRepository.getQuantity(e.getKey());
            if (available < e.getValue())
                throw new IllegalStateException(
                        "Niewystarczający stan magazynowy dla produktu '" + e.getKey().getName()
                                + "': dostępne " + available + ", żądane " + e.getValue() + ".");
        }
    }

    private void maybeMarkOrderFulfilled(UUID orderId) {
        Order order = tradeRepository.findOrderById(orderId).orElse(null);
        if (order == null) return;
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.FULFILLED)
            return;
        Map<Product, Integer> ordered = sumByProduct(order.getLines());
        Map<Product, Integer> delivered = sumByProduct(allDeliveredLinesFor(orderId));
        boolean fullyDelivered = ordered.entrySet().stream()
                .allMatch(e -> delivered.getOrDefault(e.getKey(), 0) >= e.getValue());
        if (fullyDelivered) {
            order.setStatus(OrderStatus.FULFILLED);
            tradeRepository.update(order);
        }
    }

    private List<OperationLine> allDeliveryLinesFor(UUID orderId) {
        return tradeRepository.findDeliveries().stream()
                .filter(d -> d.getOrderId().equals(orderId))
                .flatMap(d -> d.getLines().stream())
                .collect(Collectors.toList());
    }

    private List<OperationLine> allDeliveredLinesFor(UUID orderId) {
        return tradeRepository.findDeliveries().stream()
                .filter(d -> d.getOrderId().equals(orderId) && d.getStatus() == DeliveryStatus.DELIVERED)
                .flatMap(d -> d.getLines().stream())
                .collect(Collectors.toList());
    }

    private static Map<Product, Integer> sumByProduct(List<OperationLine> lines) {
        Map<Product, Integer> map = new HashMap<>();
        for (OperationLine line : lines) {
            map.merge(line.getProduct(), line.getQuantity(), Integer::sum);
        }
        return map;
    }
}
