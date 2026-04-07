package local.pk154938.shop.ui.menu;

import local.pk154938.shop.application.auth.Operation;

public class MenuOption {
    private final String label;
    private final Runnable action;
    private final Operation operation;

    public MenuOption(String label, Runnable action, Operation operation) {
        this.label = label;
        this.action = action;
        this.operation = operation;
    }

    public String getLabel() { return label; }
    public Runnable getAction() { return action; }
    public Operation getOperation() { return operation; }
}
