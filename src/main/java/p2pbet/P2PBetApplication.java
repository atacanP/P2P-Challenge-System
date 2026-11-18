package p2pbet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class P2PBetApplication {
    public static void main(String[] args) {
        System.out.println("=== Starting P2P Betting System API ===");
        SpringApplication.run(P2PBetApplication.class, args);
        System.out.println("=== P2P Betting System API is now Live on Port 8080 ===");
        System.out.println("You can now test it via Postman.");
    }
}
