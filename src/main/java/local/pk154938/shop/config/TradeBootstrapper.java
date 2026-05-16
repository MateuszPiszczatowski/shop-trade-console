package local.pk154938.shop.config;

import local.pk154938.shop.application.repository.ProductRepository;
import local.pk154938.shop.application.repository.StockRepository;
import local.pk154938.shop.application.repository.TradeRepository;
import local.pk154938.shop.domain.trade.Delivery;
import local.pk154938.shop.domain.trade.DeliveryStatus;
import local.pk154938.shop.domain.trade.ItemOperation;
import local.pk154938.shop.domain.trade.OperationLine;
import local.pk154938.shop.domain.trade.Return;
import local.pk154938.shop.domain.trade.Sale;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Replays all persisted trade operations to rebuild the in-memory state:
 *
 * <ul>
 *   <li>{@link ProductRepository} ends up holding the most recent price snapshot
 *       of every product that ever appeared in any operation;</li>
 *   <li>{@link StockRepository} reflects the net effect of confirmed deliveries
 *       (+), sales (−) and returns (−). Orders and not-yet-delivered dispatches
 *       have no stock effect.</li>
 * </ul>
 *
 * <p>Operations are processed in chronological order (oldest first) so that
 * the resulting catalog reflects the last-seen prices.
 */
public class TradeBootstrapper {
    private final TradeRepository tradeRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    public TradeBootstrapper(TradeRepository tradeRepository,
                             ProductRepository productRepository,
                             StockRepository stockRepository) {
        this.tradeRepository = tradeRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
    }

    public void bootstrap() {
        List<ItemOperation> chronological = tradeRepository.findAll().stream()
                .sorted(Comparator.comparing(ItemOperation::getTimestamp))
                .collect(Collectors.toList());

        for (ItemOperation op : chronological) {
            refreshCatalog(op.getLines());
            applyStockEffect(op);
        }
    }

    private void refreshCatalog(List<OperationLine> lines) {
        for (OperationLine line : lines) {
            productRepository.save(line.getProduct());
        }
    }

    private void applyStockEffect(ItemOperation op) {
        if (op instanceof Delivery) {
            Delivery delivery = (Delivery) op;
            if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
                for (OperationLine line : delivery.getLines()) {
                    stockRepository.increase(line.getProduct(), line.getQuantity());
                }
            }
        } else if (op instanceof Sale) {
            for (OperationLine line : op.getLines()) {
                stockRepository.decrease(line.getProduct(), line.getQuantity());
            }
        } else if (op instanceof Return) {
            for (OperationLine line : op.getLines()) {
                stockRepository.decrease(line.getProduct(), line.getQuantity());
            }
        }
        // Order has no stock effect.
    }
}
