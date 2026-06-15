package local.pk154938.shop.ui.menu;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Paged interactive lister. Displays items 10 per page; the user picks one
 * by number, types {@code N} to advance to the next page, or hits Enter to
 * cancel/return.
 */
public final class PaginatedSelector {

    private static final int PAGE_SIZE = 10;

    private PaginatedSelector() {}

    public static <T> Optional<T> selectOne(String header, List<T> items, Function<T, String> renderer) {
        if (items.isEmpty()) {
            System.out.println("Brak elementów do wyboru.");
            return Optional.empty();
        }
        int maxPage = (items.size() - 1) / PAGE_SIZE;
        int page = 0;
        while (true) {
            renderPage(header, items, renderer, page, maxPage);
            System.out.print("Wybór (1-" + items.size() + "), N = następna strona, puste = anuluj: ");
            System.out.flush();
            String input = ConsoleIo.readLine();
            if (input == null || input.isBlank()) return Optional.empty();
            if (input.equalsIgnoreCase("N")) {
                if (page < maxPage) page++;
                else System.out.println("To już ostatnia strona.");
                continue;
            }
            try {
                int idx = Integer.parseInt(input.trim());
                if (idx >= 1 && idx <= items.size()) return Optional.of(items.get(idx - 1));
                System.out.println("Niepoprawny numer (poza zakresem).");
            } catch (NumberFormatException e) {
                System.out.println("Niepoprawne wejście.");
            }
        }
    }

    public static <T> void display(String header, List<T> items, Function<T, String> renderer) {
        if (items.isEmpty()) {
            System.out.println("Brak elementów.");
            return;
        }
        int maxPage = (items.size() - 1) / PAGE_SIZE;
        int page = 0;
        while (true) {
            renderPage(header, items, renderer, page, maxPage);
            System.out.print("N = następna strona, puste = powrót: ");
            System.out.flush();
            String input = ConsoleIo.readLine();
            if (input == null || input.isBlank()) return;
            if (input.equalsIgnoreCase("N")) {
                if (page < maxPage) page++;
                else System.out.println("To już ostatnia strona.");
            }
        }
    }

    private static <T> void renderPage(String header, List<T> items, Function<T, String> renderer,
                                       int page, int maxPage) {
        System.out.println();
        System.out.println("--- " + header + " (strona " + (page + 1) + "/" + (maxPage + 1) + ") ---");
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, items.size());
        for (int i = start; i < end; i++) {
            System.out.println((i + 1) + ". " + renderer.apply(items.get(i)));
        }
    }
}
