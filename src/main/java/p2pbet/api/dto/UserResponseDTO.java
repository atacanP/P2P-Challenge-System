package p2pbet.api.dto;
public class UserResponseDTO {
    private int id;
    private String username;
    private String role; // Simulated role for frontend logic
    public UserResponseDTO(int id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
