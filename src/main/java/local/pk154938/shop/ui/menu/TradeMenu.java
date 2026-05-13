package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.repository.ProductRepository;
import local.pk154938.shop.application.repository.StockRepository;
import local.pk154938.shop.application.repository.TradeRepository;
import local.pk154938.shop.application.service.TradeService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.trade.Delivery;
import local.pk154938.shop.domain.trade.ItemOperation;
import local.pk154938.shop.domain.trade.OperationLine;
import local.pk154938.shop.domain.trade.Order;
import local.pk154938.shop.domain.trade.Product;
import local.pk154938.shop.domain.trade.Return;
import local.pk154938.shop.domain.trade.Sale;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TradeMenu extends BaseMenu {
    private final TradeService tradeService;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final TradeRepository tradeRepository;

    public TradeMenu(TradeService tradeService,
                     ProductRepository productRepository,
                     StockRepository stockRepository,
                     TradeRepository tradeRepository,
                     Session session,
                     AuthorizationService authorizationService) {
        super("OPERACJE HANDLOWE", session, authorizationService);
        this.tradeService = tradeService;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.tradeRepository = tradeRepository;
    }

    @Override
    protected void addOptions() {
        addOption("Zamówienia", this::enterOrders,
                Operation.PLACE_SUPPLIER_ORDER, Operation.VIEW_TRADE_INFO);
        addOption("Dostawy", this::enterDeliveries,
                Operation.REGISTER_DELIVERY, Operation.VIEW_TRADE_INFO);
        addOption("Sprzedaż", this::enterSales,
                Operation.MAKE_SALE, Operation.VIEW_TRADE_INFO);
        addOption("Zwroty", this::enterReturns,
                Operation.MAKE_RETURN, Operation.VIEW_TRADE_INFO);
        addOption("Wyświetl stan magazynu", this::viewStock, Operation.VIEW_TRADE_INFO);
        addOption("Szczegóły operacji", this::showOperationDetails, Operation.VIEW_TRADE_INFO);
    }

    private void enterOrders() {
        new OrderSubmenu(tradeService, tradeRepository, session, authorizationService).show();
    }

    private void enterDeliveries() {
        new DeliverySubmenu(tradeService, tradeRepository, session, authorizationService).show();
    }

    private void enterSales() {
        new SaleSubmenu(tradeService, productRepository, stockRepository, tradeRepository,
                session, authorizationService).show();
    }

    private void enterReturns() {
        new ReturnSubmenu(tradeService, stockRepository, tradeRepository,
                session, authorizationService).show();
    }

    private void viewStock() {
        Map<Product, Integer> stock = stockRepository.getAll();
        List<Map.Entry<Product, Integer>> entries = new ArrayList<>(stock.entrySet());
        PaginatedSelector.display("Stan magazynu", entries, e -> {
            Product p = e.getKey();
            return p.getName() + "  |  " + e.getValue() + " szt."
                    + "  |  netto sprzedaży: " + p.getPriceNetSale()
                    + "  |  brutto sprzedaży: " + p.priceGrossSale();
        });
    }

    private void showOperationDetails() {
        try {
            System.out.println("Wybierz typ operacji: 1. Zamówienie | 2. Dostawa | 3. Sprzedaż | 4. Zwrot");
            String choice = InputReader.readNonBlankString("Wybór: ");
            UUID id = InputReader.readUuid("Podaj UUID operacji: ");
            switch (choice) {
                case "1": displayOrder(id); break;
                case "2": displayDelivery(id); break;
                case "3": displaySale(id); break;
                case "4": displayReturn(id); break;
                default: System.out.println("Niepoprawny typ.");
            }
        } catch (InputReader.CancelledException e) {
            System.out.println("Anulowano.");
        } catch (IllegalArgumentException e) {
            System.out.println("BŁĄD: " + e.getMessage());
        }
    }

    private void displayOrder(UUID id) {
        Optional<Order> opt = tradeRepository.findOrderById(id);
        if (opt.isEmpty()) { System.out.println("Nie znaleziono zamówienia."); return; }
        Order o = opt.get();
        System.out.println();
        System.out.println("=== Zamówienie ===");
        printCommon(o);
        System.out.println("Dostawca: " + o.getSupplierName());
        System.out.println("Status: " + o.getStatus().displayName());
        printLines(o.getLines());
    }

    private void displayDelivery(UUID id) {
        Optional<Delivery> opt = tradeRepository.findDeliveryById(id);
        if (opt.isEmpty()) { System.out.println("Nie znaleziono dostawy."); return; }
        Delivery d = opt.get();
        System.out.println();
        System.out.println("=== Dostawa ===");
        printCommon(d);
        System.out.println("Zamówienie: " + d.getOrderId());
        System.out.println("Status: " + d.getStatus().displayName());
        printLines(d.getLines());
    }

    private void displaySale(UUID id) {
        Optional<Sale> opt = tradeRepository.findSaleById(id);
        if (opt.isEmpty()) { System.out.println("Nie znaleziono sprzedaży."); return; }
        Sale s = opt.get();
        System.out.println();
        System.out.println("=== Sprzedaż ===");
        printCommon(s);
        printLines(s.getLines());
    }

    private void displayReturn(UUID id) {
        Optional<Return> opt = tradeRepository.findReturnById(id);
        if (opt.isEmpty()) { System.out.println("Nie znaleziono zwrotu."); return; }
        Return r = opt.get();
        System.out.println();
        System.out.println("=== Zwrot ===");
        printCommon(r);
        System.out.println("Zamówienie: " + r.getOrderId());
        printLines(r.getLines());
    }

    private static void printCommon(ItemOperation op) {
        System.out.println("ID: " + op.getId());
        System.out.println("Czas: " + Formatters.time(op.getTimestamp()));
    }

    private static void printLines(List<OperationLine> lines) {
        System.out.println("Pozycje:");
        for (OperationLine line : lines) {
            Product p = line.getProduct();
            System.out.println("  - " + p.getName() + " × " + line.getQuantity()
                    + "  | netto kupna: " + p.getPriceNetPurchase()
                    + "  | netto sprzedaży: " + p.getPriceNetSale()
                    + "  | VAT: " + p.getVatRate()
                    + "  | brutto sprzedaży: " + p.priceGrossSale());
        }
    }
}
