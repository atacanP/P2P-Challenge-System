package p2pbet.api;
import p2pbet.api.dto.AuthRequestDTO;
import p2pbet.business.AdminService;
import p2pbet.business.WalletService;
import p2pbet.core.entities.exception.UnauthorizedActionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import p2pbet.api.dto.UserResponseDTO;
import p2pbet.business.UserService;
import p2pbet.business.BetService;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
@RestController
@RequestMapping("/api/admins")
public class AdminController {
    private final AdminService adminService;
    private final WalletService walletService;
    private final UserService userService;
    private final BetService betService;
    public AdminController(AdminService adminService, WalletService walletService, UserService userService, BetService betService) {
        this.adminService = adminService;
        this.walletService = walletService;
        this.userService = userService;
        this.betService = betService;
    }
    @PostMapping("/register")
    public ResponseEntity<String> registerAdmin(@RequestHeader("X-Admin-Secret") String secretKey, @RequestBody AuthRequestDTO dto) {
        try {
            adminService.registerAdmin(dto.getUsername(), dto.getPassword(), secretKey);
            return ResponseEntity.status(HttpStatus.CREATED).body("Admin registration successful.");
        } catch (UnauthorizedActionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthRequestDTO dto) {
        try {
            int token = adminService.login(dto.getUsername(), dto.getPassword());
            return ResponseEntity.ok(String.valueOf(token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
    @PutMapping("/wallets/{userId}")
    public ResponseEntity<String> updateWalletBalance(@RequestHeader("X-Admin-Id") int adminId, @PathVariable int userId, @RequestBody Map<String, Double> body) {
        try {
            if (!adminService.isAdmin(adminId)) {
                throw new UnauthorizedActionException("Admin privileges required.");
            }
            Double newBalance = body.get("newBalance");
            if (newBalance == null) {
                return ResponseEntity.badRequest().body("newBalance is required.");
            }
            walletService.adminUpdateBalance(adminId, userId, newBalance);
            return ResponseEntity.ok("Balance updated successfully.");
        } catch (UnauthorizedActionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader("X-Admin-Id") int adminId) {
        try {
            if (!adminService.isAdmin(adminId)) {
                throw new UnauthorizedActionException("Admin privileges required.");
            }
            List<UserResponseDTO> users = userService.getAllUsers().stream()
                    .map(u -> new UserResponseDTO(u.getId(), u.getUsername(), "USER"))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(users);
        } catch (UnauthorizedActionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @GetMapping("/bets")
    public ResponseEntity<?> getAllBets(@RequestHeader("X-Admin-Id") int adminId) {
        try {
            if (!adminService.isAdmin(adminId)) {
                throw new UnauthorizedActionException("Admin privileges required.");
            }
            return ResponseEntity.ok(betService.getAllBets());
        } catch (UnauthorizedActionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
