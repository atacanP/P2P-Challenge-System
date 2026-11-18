package p2pbet.api.dto;
public class GameRoomJoinDTO {
    private int userId;
    private String choice;
    public GameRoomJoinDTO() {}
    public GameRoomJoinDTO(int userId, String choice) {
        this.userId = userId;
        this.choice = choice;
    }
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public String getChoice() {
        return choice;
    }
    public void setChoice(String choice) {
        this.choice = choice;
    }
}
