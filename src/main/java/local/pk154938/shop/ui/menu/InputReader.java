package local.pk154938.shop.ui.menu;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Console input helpers used by trade menus. An empty input is treated as
 * a user-initiated cancellation — readers that require a value throw
 * {@link CancelledException} so the surrounding flow can short-circuit
 * with a single try/catch.
 */
public final class InputReader {

    private InputReader() {}

    public static class CancelledException extends RuntimeException {
        public CancelledException() { super("Operacja anulowana."); }
    }

    public static String readNonBlankString(String prompt) {
        System.out.print(prompt);

        System.out.flush();
        String s = System.console().readLine();
        if (s == null || s.isBlank()) throw new CancelledException();
        return s.trim();
    }

    public static BigDecimal readNonNegativeBigDecimal(String prompt) {
        String s = readNonBlankString(prompt).replace(',', '.');
        BigDecimal value;
        try {
            value = new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Niepoprawna liczba: " + s);
        }
        if (value.signum() < 0)
            throw new IllegalArgumentException("Wartość nie może być ujemna.");
        return value;
    }

    public static int readPositiveInt(String prompt) {
        String s = readNonBlankString(prompt);
        int v;
        try {
            v = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Niepoprawna liczba całkowita: " + s);
        }
        if (v <= 0)
            throw new IllegalArgumentException("Wartość musi być dodatnia.");
        return v;
    }

    public static UUID readUuid(String prompt) {
        String s = readNonBlankString(prompt);
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Niepoprawny format UUID: " + s);
        }
    }
}
