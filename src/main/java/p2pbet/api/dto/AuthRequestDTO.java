package p2pbet.api.dto;
public class AuthRequestDTO {
    private String username;
    private String password;
    public AuthRequestDTO() {}
    public AuthRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
