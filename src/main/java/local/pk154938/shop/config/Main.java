package local.pk154938.shop.config;
import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.repository.UserRepository;
import local.pk154938.shop.application.service.UserService;
import local.pk154938.shop.application.session.Session;
import local.pk154938.shop.domain.user.Admin;
import local.pk154938.shop.domain.user.Role;
import local.pk154938.shop.domain.user.User;
import local.pk154938.shop.infrastructure.persistence.InMemoryUserRepository;
import local.pk154938.shop.ui.menu.MainMenu;
import local.pk154938.shop.util.SecurityUtils;

import java.util.Set;

public class Main {
    public static void main(String[] args) {
        Session session = new Session();
        UserRepository userRepository = new InMemoryUserRepository();
        
        // Seed the database with a default admin if one doesn't exist
        DataSeeder dataSeeder = new DataSeeder(userRepository);
        dataSeeder.seedAdminIfMissing();
        
        AuthorizationService authService = new AuthorizationService();
        UserService userService = new UserService(userRepository, authService);
        MainMenu menu = new MainMenu(userService, session, authService);
        menu.show();
    }
}