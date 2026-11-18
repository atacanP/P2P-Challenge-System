package p2pbet.api;
import p2pbet.api.dto.AuthRequestDTO;
import p2pbet.business.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import p2pbet.api.dto.UserResponseDTO;
import p2pbet.core.entities.User;
import p2pbet.core.entities.exception.UnauthorizedActionException;
import p2pbet.business.AdminService;
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final AdminService adminService;
    public UserController(UserService userService, AdminService adminService) {
        this.userService = userService;
        this.adminService = adminService;
    }
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequestDTO dto) {
        try {
            userService.register(dto.getUsername(), dto.getPassword());
            return ResponseEntity.status(HttpStatus.CREATED).body("Registration successful.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthRequestDTO dto) {
        try {
            int token = userService.login(dto.getUsername(), dto.getPassword());
            return ResponseEntity.ok(String.valueOf(token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(
            @PathVariable int userId,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminId) {
        try {
            boolean isAdmin = adminId != null && !adminId.trim().isEmpty() && adminService.isAdmin(Integer.parseInt(adminId));
            boolean isSelf = requesterId != null && !requesterId.trim().isEmpty() && Integer.parseInt(requesterId) == userId;
            if (!isAdmin && !isSelf) {
                throw new UnauthorizedActionException("Access Denied: You can only view your own profile.");
            }
            User user = userService.getUserById(userId);
            UserResponseDTO response = new UserResponseDTO(user.getId(), user.getUsername(), isAdmin && !isSelf ? "USER (Viewed by Admin)" : "USER");
            return ResponseEntity.ok(response);
        } catch (UnauthorizedActionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody java.util.Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required.");
            }
            String token = userService.requestPasswordReset(email);
            return ResponseEntity.ok("Simulated Email Sent! Your reset token is: " + token);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody java.util.Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            if (token == null || token.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Token and newPassword are required.");
            }
            userService.resetPassword(token, newPassword);
            return ResponseEntity.ok("Password reset successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
