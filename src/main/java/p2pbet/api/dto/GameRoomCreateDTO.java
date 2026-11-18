package p2pbet.api.dto;
import p2pbet.core.entities.GameType;
public class GameRoomCreateDTO {
    private GameType gameType;
    private double entryFee;
    private int capacity;
    private int creatorId;
    private String creatorChoice;
    public GameRoomCreateDTO() {}
    public GameRoomCreateDTO(GameType gameType, double entryFee, int capacity, int creatorId, String creatorChoice) {
        this.gameType = gameType;
        this.entryFee = entryFee;
        this.capacity = capacity;
        this.creatorId = creatorId;
        this.creatorChoice = creatorChoice;
    }
    public GameType getGameType() {
        return gameType;
    }
    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }
    public double getEntryFee() {
        return entryFee;
    }
    public void setEntryFee(double entryFee) {
        this.entryFee = entryFee;
    }
    public int getCapacity() {
        return capacity;
    }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    public int getCreatorId() {
        return creatorId;
    }
    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }
    public String getCreatorChoice() {
        return creatorChoice;
    }
    public void setCreatorChoice(String creatorChoice) {
        this.creatorChoice = creatorChoice;
    }
}
