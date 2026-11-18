package p2pbet.business;
import p2pbet.core.entities.User;
import p2pbet.dataaccess.IUserRepository;
import java.util.UUID;
import java.util.Optional;
import org.springframework.stereotype.Service;
@Service
public class UserService {
    private final IUserRepository userRepository;
    private final WalletService walletService;
    public UserService(IUserRepository userRepository, WalletService walletService) {
        this.userRepository = userRepository;
        this.walletService = walletService;
    }
    public User register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }
        String fakeHash = Integer.toHexString(password.hashCode());
        int generatedId = Math.abs(UUID.randomUUID().hashCode() % 1000000);
        User user = new User(generatedId, username, fakeHash, username + "@example.com", "");
        userRepository.save(user);
        walletService.createWallet(user.getId(), 0.0);
        System.out.println("User registered and wallet created: " + username);
        return user;
    }
    public int login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        User user = userOpt.get();
        if (!user.validatePassword(password)) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        System.out.println("User logged in successfully: " + username);
        return user.getId();
    }
    public String requestPasswordReset(String email) {
        p2pbet.dataaccess.csv.UserRepository repo = (p2pbet.dataaccess.csv.UserRepository) userRepository;
        Optional<User> userOpt = repo.findByEmail(email);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("Email not found.");
        }
        User user = userOpt.get();
        String token = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setResetToken(token);
        userRepository.save(user);
        System.out.println("Password reset token generated for " + email + ": " + token);
        return token;
    }
    public void resetPassword(String token, String newPassword) {
        p2pbet.dataaccess.csv.UserRepository repo = (p2pbet.dataaccess.csv.UserRepository) userRepository;
        Optional<User> userOpt = repo.findByResetToken(token);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("Invalid reset token.");
        }
        User user = userOpt.get();
        user.setPassword(newPassword);
        user.setResetToken("");
        userRepository.save(user);
        System.out.println("Password reset successfully for user: " + user.getUsername());
    }
    public User getUserById(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
