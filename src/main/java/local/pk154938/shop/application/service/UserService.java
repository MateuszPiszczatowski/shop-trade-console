package local.pk154938.shop.application.service;

import local.pk154938.shop.application.auth.AuthorizationService;
import local.pk154938.shop.application.auth.Operation;
import local.pk154938.shop.application.repository.UserRepository;
import local.pk154938.shop.domain.user.*;
import local.pk154938.shop.util.SecurityUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class UserService {
    private final UserRepository userRepository;
    private final AuthorizationService authService;
    public UserService(UserRepository userRepository, AuthorizationService authService){
        this.userRepository = userRepository;
        this.authService = authService;
    }
    public Optional<User> login(String username, String rawPassword){
        return userRepository.findByUsername(username).filter(user -> {
            String attemptHash = SecurityUtils.hashPassword(rawPassword, user.getSalt());
            return attemptHash.equals(user.getHashedPassword());
        });
    }

    public void removeUser(String username) {
        userRepository.delete(username);
    }

    public void createAndAddUser(String username, String password, Role roleToCreate, User currentUser) {
        Operation op = mapRoleToOperation(roleToCreate);
        if (!authService.isAuthorized(currentUser, op)) {
            throw new IllegalStateException("Brak uprawnień! Twoja ranga nie pozwala na tworzenie roli: " + roleToCreate);
        }

        User newUser;
        String salt = SecurityUtils.generateSalt();
        String hashedPassword = SecurityUtils.hashPassword(password, salt);

        switch (roleToCreate) {
            case ADMIN:    newUser = new Admin(username, hashedPassword, salt, Set.of(Role.ADMIN)); break;
            case MANAGER:  newUser = new Manager(username, hashedPassword, salt, Set.of(Role.MANAGER)); break;
            case EMPLOYEE: newUser = new Employee(username, hashedPassword, salt, Set.of(Role.EMPLOYEE)); break;
            default: throw new IllegalArgumentException("Nieznana rola!");
        }

        userRepository.save(newUser);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private Operation mapRoleToOperation(Role role) {
        switch (role) {
            case ADMIN: return Operation.ADD_ADMIN;
            case MANAGER: return Operation.ADD_MANAGER;
            case EMPLOYEE: return Operation.ADD_EMPLOYEE;
            default: throw new IllegalStateException("Nieobsługiwana rola");
        }
    }
}
