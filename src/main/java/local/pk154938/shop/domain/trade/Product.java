package local.pk154938.shop.domain.trade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable catalog entry. Identity is based on lower-cased {@link #name},
 * which means two {@code Product} instances with the same name but different
 * prices are considered equal — this is intentional: the catalog tracks a
 * single logical product whose price snapshot is updated by the most recent
 * trade operation.
 */
public final class Product {
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final String name;
    private final BigDecimal priceNetPurchase;
    private final BigDecimal priceNetSale;
    private final BigDecimal vatRate;

    public Product(String name, BigDecimal priceNetPurchase, BigDecimal priceNetSale, BigDecimal vatRate) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Nazwa produktu nie może być pusta.");
        if (priceNetPurchase == null || priceNetPurchase.signum() < 0)
            throw new IllegalArgumentException("Cena netto kupna nie może być ujemna.");
        if (priceNetSale == null || priceNetSale.signum() < 0)
            throw new IllegalArgumentException("Cena netto sprzedaży nie może być ujemna.");
        if (vatRate == null || vatRate.signum() < 0 || vatRate.compareTo(ONE) > 0)
            throw new IllegalArgumentException("Stawka VAT musi mieścić się w zakresie [0, 1].");

        this.name = name.trim();
        this.priceNetPurchase = priceNetPurchase;
        this.priceNetSale = priceNetSale;
        this.vatRate = vatRate;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPriceNetPurchase() {
        return priceNetPurchase;
    }

    public BigDecimal getPriceNetSale() {
        return priceNetSale;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public BigDecimal priceGrossPurchase() {
        return priceNetPurchase.multiply(ONE.add(vatRate)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal priceGrossSale() {
        return priceNetSale.multiply(ONE.add(vatRate)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product other = (Product) o;
        return name.toLowerCase().equals(other.name.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    @Override
    public String toString() {
        return name;
    }
}
