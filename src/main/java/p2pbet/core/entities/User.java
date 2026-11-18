package p2pbet.core.entities;
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String email;
    private String resetToken;
    public User(int id, String username, String passwordHash, String email, String resetToken) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.resetToken = resetToken;
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
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getResetToken() {
        return resetToken;
    }
    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }
    public void setPassword(String rawPassword) {
        this.passwordHash = Integer.toHexString(rawPassword.hashCode());
    }
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
