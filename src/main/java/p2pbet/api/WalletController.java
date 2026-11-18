package p2pbet.api;
import p2pbet.api.dto.FundRequestDTO;
import p2pbet.business.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/wallets")
public class WalletController {
    private final WalletService walletService;
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }
    @PostMapping("/deposit")
    public ResponseEntity<String> depositFunds(@RequestHeader("X-User-Id") int requesterId, @RequestBody FundRequestDTO dto) {
        try {
            if (requesterId != dto.getUserId()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access Denied: You can only perform transactions on your own wallet.");
            }
            walletService.deposit(dto.getUserId(), dto.getAmount());
            return ResponseEntity.ok("Funds deposited successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PostMapping("/withdraw")
    public ResponseEntity<String> withdrawFunds(@RequestHeader("X-User-Id") int requesterId, @RequestBody FundRequestDTO dto) {
        try {
            if (requesterId != dto.getUserId()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access Denied: You can only perform transactions on your own wallet.");
            }
            walletService.withdraw(dto.getUserId(), dto.getAmount());
            return ResponseEntity.ok("Funds withdrawn successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @GetMapping("/{userId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable int userId) {
        try {
            double balance = walletService.getBalance(userId);
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @GetMapping("/{userId}/details")
    public ResponseEntity<?> getWalletDetails(@PathVariable int userId) {
        try {
            return ResponseEntity.ok(walletService.getWallet(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
