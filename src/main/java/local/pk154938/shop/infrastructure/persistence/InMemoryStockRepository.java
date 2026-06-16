package local.pk154938.shop.infrastructure.persistence;

import local.pk154938.shop.application.repository.StockRepository;
import local.pk154938.shop.domain.trade.Product;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryStockRepository implements StockRepository {
    private final Map<Product, Integer> stock = new HashMap<>();

    @Override
    public void increase(Product product, int amount) {
        requirePositive(amount);
        // Re-key on the product object just supplied so getAll() doesn't keep an
        // outdated price/VAT snapshot (Product equality is name-based, and a plain
        // merge would retain the first-seen key). The authoritative price for
        // display still comes from the catalog (ProductRepository).
        int next = stock.getOrDefault(product, 0) + amount;
        stock.remove(product);
        stock.put(product, next);
    }

    @Override
    public void decrease(Product product, int amount) {
        requirePositive(amount);
        int current = stock.getOrDefault(product, 0);
        int next = current - amount;
        if (next < 0)
            throw new IllegalStateException(
                    "Niewystarczający stan magazynowy dla produktu '" + product.getName()
                            + "': dostępne " + current + ", żądane " + amount + ".");
        if (next == 0) {
            stock.remove(product);
        } else {
            stock.put(product, next);
        }
    }

    @Override
    public int getQuantity(Product product) {
        return stock.getOrDefault(product, 0);
    }

    @Override
    public Map<Product, Integer> getAll() {
        // Defensive copy preserving insertion order for predictable display.
        return new LinkedHashMap<>(stock);
    }

    private static void requirePositive(int amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Ilość musi być dodatnia.");
    }
}
