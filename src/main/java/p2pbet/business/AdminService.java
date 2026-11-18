package p2pbet.business;
import p2pbet.core.entities.Admin;
import p2pbet.dataaccess.IAdminRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;
@Service
public class AdminService {
    private final IAdminRepository adminRepository;
    public AdminService(IAdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }
    public Admin registerAdmin(String username, String password, String secretKey) {
        if (!"SUPER_SECRET_KEY".equals(secretKey)) {
            throw new p2pbet.core.entities.exception.UnauthorizedActionException("Invalid admin secret key.");
        }
        if (adminRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Admin username already exists.");
        }
        String fakeHash = Integer.toHexString(password.hashCode());
        int generatedId = Math.abs(UUID.randomUUID().hashCode() % 1000000);
        Admin admin = new Admin(generatedId, username, fakeHash);
        adminRepository.save(admin);
        System.out.println("Admin registered: " + username);
        return admin;
    }
    public int login(String username, String password) {
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (!adminOpt.isPresent()) {
            throw new IllegalArgumentException("Invalid admin username or password.");
        }
        Admin admin = adminOpt.get();
        if (!admin.validatePassword(password)) {
            throw new IllegalArgumentException("Invalid admin username or password.");
        }
        System.out.println("Admin logged in successfully: " + username);
        return admin.getId();
    }
    public boolean isAdmin(int adminId) {
        return adminRepository.findById(adminId).isPresent();
    }
}
