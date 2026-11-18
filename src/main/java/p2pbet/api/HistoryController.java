package p2pbet.api;
import p2pbet.core.entities.Bet;
import p2pbet.core.entities.GameRoom;
import p2pbet.business.HistoryService;
import p2pbet.business.AdminService;
import p2pbet.core.entities.exception.UnauthorizedActionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final HistoryService historyService;
    private final AdminService adminService;
    public HistoryController(HistoryService historyService, AdminService adminService) {
        this.historyService = historyService;
        this.adminService = adminService;
    }
    private void validateAccess(String requesterId, int targetUserId) {
        if (requesterId == null || requesterId.trim().isEmpty()) {
            throw new UnauthorizedActionException("Missing X-User-Id header.");
        }
        try {
            int reqId = Integer.parseInt(requesterId);
            if (reqId != targetUserId && !adminService.isAdmin(reqId)) {
                throw new UnauthorizedActionException("Access Denied: You can only view your own history.");
            }
        } catch (NumberFormatException e) {
            throw new UnauthorizedActionException("Invalid X-User-Id format.");
        }
    }
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUnifiedHistory(@PathVariable int userId, @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        try {
            validateAccess(requesterId, userId);
            Map<String, Object> history = new HashMap<>();
            history.put("bets", historyService.getUserBetHistory(userId));
            history.put("games", historyService.getUserGameHistory(userId));
            return ResponseEntity.ok(history);
        } catch (UnauthorizedActionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
}
