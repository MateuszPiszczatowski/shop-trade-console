package local.pk154938.shop.config;

import local.pk154938.shop.application.repository.UserRepository;
import local.pk154938.shop.domain.user.Admin;
import local.pk154938.shop.domain.user.Role;
import local.pk154938.shop.util.SecurityUtils;

import java.util.Set;

public class DataSeeder {
    private final UserRepository userRepository;

    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void seedAdminIfMissing() {
        // Check if any admin already exists
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(user -> user.getRoles().contains(Role.ADMIN));

        if (!adminExists) {
            String adminUser = System.getenv("SHOP_DEFAULT_ADMIN_USER");
            if (adminUser == null || adminUser.isBlank()) {
                adminUser = "admin"; // Fallback default
            }

            String adminPass = System.getenv("SHOP_DEFAULT_ADMIN_PASS");
            if (adminPass == null || adminPass.isBlank()) {
                adminPass = "admin"; // Fallback default
            }

            String salt = SecurityUtils.generateSalt();
            String hashedPassword = SecurityUtils.hashPassword(adminPass, salt);

            Admin defaultAdmin = new Admin(adminUser, hashedPassword, salt, Set.of(Role.ADMIN));
            userRepository.save(defaultAdmin);
            System.out.println("[SYSTEM] Utworzono domyślne konto administratora: " + adminUser);
        }
    }
}