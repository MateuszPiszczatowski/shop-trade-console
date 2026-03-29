package local.pk154938.shop.domain.user;

import java.util.Set;

public class User {
    private final String username;
    private final String hashedPassword;
    private Set<Role> roles;

    public User(String username, String hashedPassword, Set<Role> roles){
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.roles = roles;
    }

    public String getUsername() {
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public Set<Role> getRoles() {return roles;}
}
