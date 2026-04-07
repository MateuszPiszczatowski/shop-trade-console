package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.session.Session;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseMenu {
    protected final String title;
    protected final List<MenuOption> options = new ArrayList<>();
    protected final Session session;
    protected final AuthorizationService authorizationService;

    public BaseMenu(String title, Session session, AuthorizationService authorizationService) {
        this.title = title;
        this.session = session;
        this.authorizationService = authorizationService;
    }

    public final void prepareOptions(){
        options.clear();
    }

    protected abstract void addOptions();

    protected final void addOption(String label, Runnable action, Operation op){
        if (op == Operation.UNRESTRICTED || authorizationService.isAuthorized(session.getCurrentUser(), op)) {
            options.add(new MenuOption(label, action));
        }
    }

    public final void show() {
        boolean running = true;

        while (running) {
            prepareOptions();
            addOptions();
            if(options.isEmpty()){
                running=false;
                break;
            }
            display();
            running = handleInput();
        }
    }

    private void display(){
        System.out.println("\n=== " + title + " ===");

        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i).getLabel());
        }
        System.out.println("0. Powrót/Wyjście");
    }

    private boolean handleInput(){
        String input = System.console().readLine("Wybór: ");

        if ("0".equals(input)) {
            return false;
        } else {
            try {
                int choice = Integer.parseInt(input);
                if (choice > 0 && choice <= options.size()) {
                    options.get(choice - 1).getAction().run();
                } else {
                    System.out.println("Niepoprawny numer.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Błąd, aby wybrać opcję z menu należy wprowadzić liczbę.");
            }
            return true;
        }
    }
}
