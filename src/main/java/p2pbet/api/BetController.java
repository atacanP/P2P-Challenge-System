package p2pbet.api;
import p2pbet.core.entities.Bet;
import p2pbet.api.dto.BetCreateDTO;
import p2pbet.api.dto.MatchBetDTO;
import p2pbet.business.BetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/bets")
public class BetController {
    private final BetService betService;
    public BetController(BetService betService) {
        this.betService = betService;
    }
    @PostMapping
    public ResponseEntity<?> createBet(@RequestBody BetCreateDTO dto) {
        try {
            if (dto.getStake() <= 0) {
                return ResponseEntity.badRequest().body("Stake must be greater than 0");
            }
            Bet bet = betService.createOffer(dto.getCreatorId(), dto.getEventId(), dto.getStake(), dto.getCreatorSelection());
            return ResponseEntity.status(HttpStatus.CREATED).body(bet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PutMapping("/match")
    public ResponseEntity<String> acceptBet(@RequestBody MatchBetDTO dto) {
        try {
            betService.acceptOffer(dto.getBetId(), dto.getAcceptorId());
            return ResponseEntity.ok("Bet matched successfully.");
        } catch (p2pbet.core.entities.exception.OptimisticLockException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Bet already taken");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PutMapping("/{betId}/cancel")
    public ResponseEntity<String> cancelBet(@PathVariable int betId, @RequestParam int requesterId) {
        try {
            betService.cancelOffer(betId, requesterId);
            return ResponseEntity.ok("Bet cancelled successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/available")
    public ResponseEntity<List<Bet>> getAvailableBets() {
        return ResponseEntity.ok(betService.getAvailableBets());
    }
}
