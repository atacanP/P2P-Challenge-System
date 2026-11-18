package p2pbet.api;
import p2pbet.core.entities.GameRoom;
import p2pbet.api.dto.GameRoomCreateDTO;
import p2pbet.api.dto.GameRoomJoinDTO;
import p2pbet.business.GameRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/gamerooms")
public class GameRoomController {
    private final GameRoomService gameRoomService;
    public GameRoomController(GameRoomService gameRoomService) {
        this.gameRoomService = gameRoomService;
    }
    @PostMapping("/create")
    public ResponseEntity<String> createRoom(@RequestBody GameRoomCreateDTO request) {
        try {
            GameRoom room = gameRoomService.createRoom(
                request.getGameType(), 
                request.getEntryFee(),
                request.getCapacity(),
                request.getCreatorId(),
                request.getCreatorChoice()
            );
            return ResponseEntity.ok("Game Room created successfully: " + room.getRoomId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PutMapping("/{roomId}/join")
    public ResponseEntity<String> joinRoom(@PathVariable int roomId, @RequestBody GameRoomJoinDTO request) {
        try {
            gameRoomService.joinRoom(roomId, request.getUserId(), request.getChoice());
            return ResponseEntity.ok("User joined room successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (p2pbet.core.entities.exception.InsufficientFundsException e) {
            return ResponseEntity.badRequest().body("Not enough funds to join room: " + e.getMessage());
        }
    }
    @PutMapping("/{roomId}/cancel")
    public ResponseEntity<String> cancelRoom(@PathVariable int roomId) {
        try {
            gameRoomService.cancelRoom(roomId);
            return ResponseEntity.ok("Game room cancelled successfully. Participants refunded.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/available")
    public ResponseEntity<List<GameRoom>> getAvailableRooms() {
        return ResponseEntity.ok(gameRoomService.getAvailableRooms());
    }
    @GetMapping
    public ResponseEntity<List<GameRoom>> getAllRooms() {
        return ResponseEntity.ok(gameRoomService.getAllRooms());
    }
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomById(@PathVariable int roomId) {
        try {
            return ResponseEntity.ok(gameRoomService.getRoomById(roomId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @GetMapping("/{roomId}/choices")
    public ResponseEntity<?> getAvailableChoices(@PathVariable int roomId) {
        try {
            GameRoom room = gameRoomService.getRoomById(roomId);
            return ResponseEntity.ok(room.getAvailableChoices());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @PutMapping("/{roomId}/leave")
    public ResponseEntity<String> leaveRoom(@PathVariable int roomId, @RequestParam int userId) {
        try {
            gameRoomService.leaveRoom(roomId, userId);
            return ResponseEntity.ok("Left room successfully. Entry fee refunded.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
