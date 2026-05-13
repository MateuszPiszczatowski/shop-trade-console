package local.pk154938.shop.ui.menu;

import local.pk154938.shop.domain.trade.Delivery;
import local.pk154938.shop.domain.trade.OperationLine;
import local.pk154938.shop.domain.trade.Order;
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
}
