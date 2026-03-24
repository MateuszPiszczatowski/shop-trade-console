package local.pk154938.shop.domain.user;

public class User {
    private final String username;
    private final String hashedPassword;

    public User(String username, String hashedPassword){
        this.username = username;
        this.hashedPassword = hashedPassword;
    }

    public String getUsername() {
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }
}
