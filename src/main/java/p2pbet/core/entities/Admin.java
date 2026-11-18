package p2pbet.core.entities;
public class Admin {
    private int id;
    private String username;
    private String passwordHash;
    public Admin(int id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }
    public int getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getPasswordHash() {
        return passwordHash;
    }
    public boolean validatePassword(String rawPassword) {
        String hash = Integer.toHexString(rawPassword.hashCode());
        return this.passwordHash.equals(hash);
    }
}
