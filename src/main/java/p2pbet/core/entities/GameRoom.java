package p2pbet.core.entities;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
public class GameRoom {
    private int roomId;
    private GameType gameType;
    private double entryFee;
    private int capacity;
    private double poolAmount;
    private GameRoomStatus status;
    private List<Participant> participants;
    private Integer winnerId;
    private String createdAt;
    public GameRoom(int roomId, GameType gameType, double entryFee, int capacity) {
        this.roomId = roomId;
        this.gameType = gameType;
        this.entryFee = entryFee;
        this.capacity = capacity;
        this.poolAmount = 0.0;
        this.status = GameRoomStatus.OPEN;
        this.participants = new ArrayList<>();
        this.winnerId = null;
        this.createdAt = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    public GameRoom(int roomId, GameType gameType, double entryFee, int capacity, double poolAmount, GameRoomStatus status, List<Participant> participants, Integer winnerId, String createdAt) {
        this.roomId = roomId;
        this.gameType = gameType;
        this.entryFee = entryFee;
        this.capacity = capacity;
        this.poolAmount = poolAmount;
        this.status = status;
        this.participants = new ArrayList<>(participants);
        this.winnerId = winnerId;
        this.createdAt = createdAt;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public void addParticipant(Participant participant) {
        if (this.status != GameRoomStatus.OPEN) {
            throw new IllegalStateException("Game room is not open.");
        }
        boolean alreadyJoined = participants.stream()
                .anyMatch(p -> p.getUserId() == participant.getUserId());
        if (alreadyJoined) {
            throw new IllegalArgumentException("User has already joined this room.");
        }
        this.participants.add(participant);
        this.poolAmount += this.entryFee;
    }
    public void removeParticipant(int userId) {
        if (this.status != GameRoomStatus.OPEN) {
            throw new IllegalStateException("Cannot leave a room that is not open.");
        }
        boolean removed = this.participants.removeIf(p -> p.getUserId() == userId);
        if (!removed) {
            throw new IllegalArgumentException("User " + userId + " is not in this room.");
        }
        this.poolAmount -= this.entryFee;
    }
    public void finishRoom() {
        this.status = GameRoomStatus.FINISHED;
    }
    public int getRoomId() { return roomId; }
    public GameType getGameType() { return gameType; }
    public double getEntryFee() { return entryFee; }
    public int getCapacity() { return capacity; }
    public double getPoolAmount() { return poolAmount; }
    public GameRoomStatus getStatus() { return status; }
    public List<Participant> getParticipants() { return new ArrayList<>(participants); }
    public Integer getWinnerId() { return winnerId; }
    public void setWinnerId(Integer winnerId) { this.winnerId = winnerId; }
    public List<String> getAllowedChoices() {
        List<String> choices = new ArrayList<>();
        if (gameType == GameType.DICE_ROULETTE) {
            for (int i = 1; i <= Math.min(capacity, 6); i++) {
                choices.add(String.valueOf(i));
            }
        } else if (gameType == GameType.CLOSEST_NUMBER) {
            for (int i = 1; i <= 1000; i++) {
                choices.add(String.valueOf(i));
            }
        } else {
            for (int i = 1; i <= capacity; i++) {
                choices.add("Slot " + i);
            }
        }
        return choices;
    }
    public List<String> getAvailableChoices() {
        List<String> allowed = getAllowedChoices();
        List<String> taken = participants.stream()
                .map(Participant::getChoice)
                .collect(Collectors.toList());
        allowed.removeAll(taken);
        return allowed;
    }
    public String getParticipantsAsString() {
        if (participants.isEmpty()) return "NONE";
        return participants.stream()
                .map(p -> p.getUserId() + ":" + p.getChoice())
                .collect(Collectors.joining("|"));
    }
}
