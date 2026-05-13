package local.pk154938.shop.application.repository;

import local.pk154938.shop.domain.trade.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    /**
     * Saves a product. If a product with the same name (case-insensitive) already
     * exists, it is overwritten — this is the snapshot-replacement behavior used
     * to refresh prices when a newer operation references the same product.
     */
    void save(Product product);

    Optional<Product> findByName(String name);

    List<Product> findAll();
}
