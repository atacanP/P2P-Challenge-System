package p2pbet.api;
import p2pbet.api.dto.EventResultDTO;
import p2pbet.business.EventService;
import p2pbet.business.SettlementEngineFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;
    private final SettlementEngineFacade settlementEngine;
    private final p2pbet.business.AdminService adminService;
    private final p2pbet.business.BetService betService;
    public EventController(EventService eventService, SettlementEngineFacade settlementEngine, p2pbet.business.AdminService adminService, p2pbet.business.BetService betService) {
        this.eventService = eventService;
        this.settlementEngine = settlementEngine;
        this.adminService = adminService;
        this.betService = betService;
    }
    @GetMapping
    public ResponseEntity<java.util.List<p2pbet.core.entities.Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }
    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEventById(@PathVariable int eventId) {
        try {
            return ResponseEntity.ok(eventService.getEventById(eventId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @GetMapping("/available")
    public ResponseEntity<java.util.List<p2pbet.core.entities.Event>> getAvailableEvents() {
        return ResponseEntity.ok(eventService.getAvailableEvents());
    }
    @PostMapping
    public ResponseEntity<String> createEvent(@RequestHeader("X-Admin-Id") int adminId, @RequestBody p2pbet.api.dto.EventCreateDTO dto) {
        try {
            if (!adminService.isAdmin(adminId)) {
                throw new p2pbet.core.entities.exception.UnauthorizedActionException("Admin privileges required.");
            }
            eventService.createEvent(dto.getDescription(), dto.getOptionA(), dto.getOptionB());
            return ResponseEntity.status(HttpStatus.CREATED).body("Event created successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PutMapping("/{eventId}/start")
    public ResponseEntity<String> startEvent(@RequestHeader("X-Admin-Id") int adminId, @PathVariable int eventId) {
        try {
            if (!adminService.isAdmin(adminId)) {
                throw new p2pbet.core.entities.exception.UnauthorizedActionException("Admin privileges required.");
            }
            eventService.startEvent(eventId);
            betService.cancelUnmatchedBetsForEvent(eventId);
            return ResponseEntity.ok("Event started successfully. Unmatched bets cancelled.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PostMapping("/result")
    public ResponseEntity<String> registerEventResult(@RequestHeader("X-Admin-Id") int adminId, @RequestBody EventResultDTO dto) {
        try {
            if (!adminService.isAdmin(adminId)) {
                throw new p2pbet.core.entities.exception.UnauthorizedActionException("Admin privileges required.");
            }
            eventService.registerResult(dto.getEventId(), dto.getFinalResult());
            settlementEngine.triggerSettlement(dto.getEventId(), dto.getFinalResult());
            return ResponseEntity.ok("Event settled successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @ExceptionHandler(p2pbet.core.entities.exception.UnauthorizedActionException.class)
    public ResponseEntity<String> handleUnauthorized(p2pbet.core.entities.exception.UnauthorizedActionException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
}
