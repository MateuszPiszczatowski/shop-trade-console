package local.pk154938.shop.infrastructure.persistence;

import local.pk154938.shop.application.repository.ProductRepository;
import local.pk154938.shop.domain.trade.Product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryProductRepository implements ProductRepository {
    private final Map<String, Product> products = new HashMap<>();

    @Override
    public void save(Product product) {
        products.put(key(product.getName()), product);
    }

    @Override
    public Optional<Product> findByName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(products.get(key(name)));
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(products.values());
    }

    private static String key(String name) {
        return name.toLowerCase();
    }
}
