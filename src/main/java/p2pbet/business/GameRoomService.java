package p2pbet.business;
import p2pbet.core.entities.GameRoom;
import p2pbet.core.entities.GameRoomStatus;
import p2pbet.core.entities.GameType;
import p2pbet.core.entities.Participant;
import p2pbet.dataaccess.IGameRoomRepository;
import p2pbet.business.strategy.game.DiceRouletteStrategy;
import p2pbet.business.strategy.game.IGameStrategy;
import p2pbet.business.strategy.game.JackpotStrategy;
import p2pbet.business.strategy.game.ClosestNumberStrategy;
import p2pbet.business.strategy.game.HighestRollStrategy;
import p2pbet.business.strategy.game.HighCardStrategy;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
public class GameRoomService {
    private final IGameRoomRepository gameRoomRepository;
    private final WalletService walletService;
    private final GameRoomSettlementEngine settlementEngine;
    private final Map<GameType, IGameStrategy> strategyRegistry;
    public GameRoomService(IGameRoomRepository gameRoomRepository, WalletService walletService, GameRoomSettlementEngine settlementEngine, List<IGameStrategy> strategies) {
        this.gameRoomRepository = gameRoomRepository;
        this.walletService = walletService;
        this.settlementEngine = settlementEngine;
        this.strategyRegistry = strategies.stream()
                .collect(Collectors.toMap(IGameStrategy::getSupportedGameType, Function.identity()));
    }
    public GameRoom createRoom(GameType type, double entryFee, int capacity, int creatorId, String creatorChoice) {
        int roomId = Math.abs(java.util.UUID.randomUUID().hashCode() % 1000000);
        while(gameRoomRepository.findById(roomId).isPresent()){
            roomId = Math.abs(java.util.UUID.randomUUID().hashCode() % 1000000);
        }
        if (entryFee <= 0) {
            throw new IllegalArgumentException("Entry fee must be strictly greater than 0.");
        }
        int maxCapacity = switch (type) {
            case DICE_ROULETTE -> 6;
            default -> 12;
        };
        if (capacity <= 1) {
            throw new IllegalArgumentException("Capacity must be at least 2.");
        }
        if (capacity > maxCapacity) {
            throw new IllegalArgumentException("Capacity " + capacity + " exceeds maximum allowed (" + maxCapacity + ") for game type " + type);
        }
        GameRoom room = new GameRoom(roomId, type, entryFee, capacity);
        gameRoomRepository.save(room);
        System.out.println("Game Room created: " + roomId + " | Type: " + type + " | Capacity: " + capacity);
        joinRoom(roomId, creatorId, creatorChoice);
        return gameRoomRepository.findById(roomId).get();
    }
    public void joinRoom(int roomId, int userId, String choice) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        if (room.getStatus() != GameRoomStatus.OPEN) {
            throw new IllegalStateException("Room is not open for joining.");
        }
        if (room.getParticipants().size() >= room.getCapacity()) {
            throw new IllegalStateException(room.getGameType() + " game is full (max " + room.getCapacity() + " players).");
        }
        if (room.getGameType() == GameType.HIGH_CARD || room.getGameType() == GameType.HIGHEST_ROLL || room.getGameType() == GameType.JACKPOT) {
            choice = room.getAvailableChoices().get(0);
        } else {
            List<String> available = room.getAvailableChoices();
            if (!available.contains(choice)) {
                throw new IllegalArgumentException("Invalid or already taken choice: '" + choice + "'");
            }
        }
        walletService.reserveFunds(userId, room.getEntryFee());
        try {
            Participant participant = new Participant(userId, choice);
            room.addParticipant(participant);
            gameRoomRepository.save(room);
            System.out.println("User " + userId + " joined room " + roomId + " with choice: " + choice);
            if (room.getParticipants().size() == room.getCapacity()) {
                System.out.println("Room " + roomId + " reached capacity (" + room.getCapacity() + "). Auto-starting game...");
                startGame(roomId);
            }
        } catch (Exception e) {
            walletService.refundFunds(userId, room.getEntryFee());
            throw e;
        }
    }
    public void leaveRoom(int roomId, int userId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        if (room.getStatus() != GameRoomStatus.OPEN) {
            throw new IllegalStateException("Cannot leave a finished room.");
        }
        room.removeParticipant(userId);
        walletService.refundFunds(userId, room.getEntryFee());
        if (room.getParticipants().isEmpty()) {
            gameRoomRepository.delete(roomId);
            System.out.println("Room " + roomId + " has been deleted as all participants left.");
        } else {
            gameRoomRepository.save(room);
        }
        System.out.println("User " + userId + " left room " + roomId + ". Entry fee refunded.");
    }
    public void cancelRoom(int roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        if (room.getStatus() != GameRoomStatus.OPEN) {
            throw new IllegalStateException("Only open rooms can be cancelled.");
        }
        for (p2pbet.core.entities.Participant p : room.getParticipants()) {
            walletService.refundFunds(p.getUserId(), room.getEntryFee());
        }
        gameRoomRepository.delete(roomId);
        System.out.println("Room " + roomId + " has been cancelled by Admin. Refunds processed.");
    }
    public void startGame(int roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        if (room.getStatus() != GameRoomStatus.OPEN) {
            throw new IllegalStateException("Room is already finished.");
        }
        room.finishRoom();
        gameRoomRepository.save(room); // Save status change before drawing winner
        try {
            IGameStrategy strategy = resolveStrategy(room.getGameType());
            List<Participant> participants = room.getParticipants();
            if (participants.isEmpty()) {
                System.out.println("No participants in room " + roomId + ". Game finished.");
                return;
            }
            Participant winner = strategy.determineWinner(participants);
            if (winner != null) {
                room.setWinnerId(winner.getUserId());
                settlementEngine.executeZeroSumPayout(room, winner);
            } else {
                room.setWinnerId(-1); // -1 indicates tie/refund
                settlementEngine.executeRefunds(room);
            }
            gameRoomRepository.save(room); // Save the winnerId state
            System.out.println("Game " + roomId + " has concluded.");
        } catch (Exception e) {
            System.err.println("Error during game execution for room " + roomId + ": " + e.getMessage());
            settlementEngine.executeRefunds(room);
            throw new RuntimeException("Game execution failed due to an internal error. All participants refunded.", e);
        }
    }
    private IGameStrategy resolveStrategy(GameType type) {
        IGameStrategy strategy = strategyRegistry.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown or unsupported game type: " + type);
        }
        return strategy;
    }
    public List<GameRoom> getAvailableRooms() {
        return gameRoomRepository.findAll().stream()
                .filter(room -> room.getStatus() == GameRoomStatus.OPEN)
                .collect(Collectors.toList());
    }
    public List<GameRoom> getAllRooms() {
        return gameRoomRepository.findAll();
    }
    public GameRoom getRoomById(int roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }
}
