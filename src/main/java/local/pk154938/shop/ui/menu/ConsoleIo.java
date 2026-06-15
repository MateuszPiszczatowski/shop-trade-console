package local.pk154938.shop.ui.menu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Console I/O that also works where {@link System#console()} is null
 * (IDE Run/Debug, piped stdin). When a real Console exists it is used, so
 * passwords stay masked; otherwise input falls back to stdin — unmasked.
 */
public final class ConsoleIo {

    private static final BufferedReader STDIN =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    private ConsoleIo() {}

    /** Mirrors System.console().readLine() — returns null at end of input. */
    public static String readLine() {
        if (System.console() != null) return System.console().readLine();
        try {
            return STDIN.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Błąd odczytu wejścia.", e);
        }
    }

    /** Mirrors System.console().readPassword(). Not masked in the fallback path. */
    public static char[] readPassword() {
        if (System.console() != null) return System.console().readPassword();
        String line = readLine();
        return line == null ? null : line.toCharArray();
    }
}
