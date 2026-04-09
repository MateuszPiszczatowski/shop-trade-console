package local.pk154938.shop.domain.user;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public abstract class User {
    private final UUID id;
    private final String username;
    private final String hashedPassword;
    private final String salt;
    private final Set<Role> roles;

    public User(String username, String hashedPassword, String salt, Set<Role> roles){
        this.id = UUID.randomUUID();
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.roles = roles;
        this.salt = salt;
    }
    protected User(UUID id,String username, String hashedPassword, String salt, Set<Role> roles){
        this.id = id;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.roles = roles;
        this.salt = salt;
    }

    public User withUsername(String newUsername){
        return copy(newUsername, this.hashedPassword);
    }

    public User withPassword(String newPassword){
        return copy(this.username, newPassword);
    }

    protected abstract User copy(String username, String password);

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getSalt() {return salt;}

    public Set<Role> getRoles() {return roles;}
}
