package local.pk154938.shop.ui.menu;

import local.pk154938.shop.domain.trade.Delivery;
import local.pk154938.shop.domain.trade.ItemOperation;
import local.pk154938.shop.domain.trade.OperationLine;
import local.pk154938.shop.domain.trade.Order;
import local.pk154938.shop.domain.trade.Product;
import local.pk154938.shop.domain.trade.Return;
import local.pk154938.shop.domain.trade.Sale;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Shared rendering helpers for trade menus.
 */
public final class Formatters {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private Formatters() {}

    public static String time(Instant instant) {
        return TIMESTAMP.format(instant);
    }

    public static String linesSummary(List<OperationLine> lines) {
        int totalQty = lines.stream().mapToInt(OperationLine::getQuantity).sum();
        return lines.size() + " pozycji / " + totalQty + " szt.";
    }

    public static String renderOrder(Order o) {
        return time(o.getTimestamp()) + "  |  " + o.getId()
                + "  |  " + o.getSupplierName()
                + "  |  " + o.getStatus().displayName()
                + "  |  " + linesSummary(o.getLines());
    }

    public static String renderDelivery(Delivery d) {
        return time(d.getTimestamp()) + "  |  " + d.getId()
                + "  |  zamówienie " + d.getOrderId()
                + "  |  " + d.getStatus().displayName()
                + "  |  " + linesSummary(d.getLines());
    }

    public static String renderSale(Sale s) {
        return time(s.getTimestamp()) + "  |  " + s.getId()
                + "  |  " + linesSummary(s.getLines());
    }

    public static String renderReturn(Return r) {
        return time(r.getTimestamp()) + "  |  " + r.getId()
                + "  |  zamówienie " + r.getOrderId()
                + "  |  " + linesSummary(r.getLines());
    }

    // ---------- full detail views (multi-line) ----------

    public static String orderDetail(Order o) {
        StringBuilder sb = new StringBuilder("\n=== Zamówienie ===\n");
        appendCommon(sb, o);
        sb.append("Dostawca: ").append(o.getSupplierName()).append('\n');
        sb.append("Status: ").append(o.getStatus().displayName()).append('\n');
        appendLines(sb, o.getLines());
        return sb.toString();
    }

    public static String deliveryDetail(Delivery d) {
        StringBuilder sb = new StringBuilder("\n=== Dostawa ===\n");
        appendCommon(sb, d);
        sb.append("Zamówienie: ").append(d.getOrderId()).append('\n');
        sb.append("Status: ").append(d.getStatus().displayName()).append('\n');
        appendLines(sb, d.getLines());
        return sb.toString();
    }

    public static String saleDetail(Sale s) {
        StringBuilder sb = new StringBuilder("\n=== Sprzedaż ===\n");
        appendCommon(sb, s);
        appendLines(sb, s.getLines());
        return sb.toString();
    }

    public static String returnDetail(Return r) {
        StringBuilder sb = new StringBuilder("\n=== Zwrot ===\n");
        appendCommon(sb, r);
        sb.append("Zamówienie: ").append(r.getOrderId()).append('\n');
        appendLines(sb, r.getLines());
        return sb.toString();
    }

    private static void appendCommon(StringBuilder sb, ItemOperation op) {
        sb.append("ID: ").append(op.getId()).append('\n');
        sb.append("Czas: ").append(time(op.getTimestamp())).append('\n');
    }

    private static void appendLines(StringBuilder sb, List<OperationLine> lines) {
        sb.append("Pozycje:\n");
        for (OperationLine line : lines) {
            Product p = line.getProduct();
            sb.append("  - ").append(p.getName()).append(" × ").append(line.getQuantity())
                    .append("  | netto kupna: ").append(p.getPriceNetPurchase())
                    .append("  | netto sprzedaży: ").append(p.getPriceNetSale())
                    .append("  | VAT: ").append(p.getVatRate())
                    .append("  | brutto sprzedaży: ").append(p.priceGrossSale()).append('\n');
        }
    }
}
