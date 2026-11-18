import com.fasterxml.jackson.databind.ObjectMapper;
public class TestJackson {
    public static class AuthRequestDTO {
        private String username;
        private String password;
        public AuthRequestDTO() {}
        public AuthRequestDTO(String username, String password) { this.username = username; this.password = password; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"username\":\"test\", \"password\":\"pw\"}";
        AuthRequestDTO dto = mapper.readValue(json, AuthRequestDTO.class);
        System.out.println("Username: " + dto.getUsername());
        System.out.println("Password: " + dto.getPassword());
    }
}
