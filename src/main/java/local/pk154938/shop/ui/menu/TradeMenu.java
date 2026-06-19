package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.repository.ProductRepository;
import local.pk154938.shop.application.repository.StockRepository;
import local.pk154938.shop.application.repository.TradeRepository;
import local.pk154938.shop.application.service.TradeService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.trade.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    }

    private void enterOrders() {
        new OrderSubmenu(tradeService, tradeRepository, productRepository,
                session, authorizationService).show();
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
            // Price/VAT come from the catalog (single source of truth, latest
            // snapshot wins) rather than from the stock key, which only carries
            // a quantity. Fall back to the key if the catalog has no entry.
            Product p = productRepository.findByName(e.getKey().getName()).orElse(e.getKey());
            return p.getName() + "  |  " + e.getValue() + " szt."
                    + "  |  netto sprzedaży: " + p.getPriceNetSale()
                    + "  |  brutto sprzedaży: " + p.priceGrossSale();
        });
    }
}
