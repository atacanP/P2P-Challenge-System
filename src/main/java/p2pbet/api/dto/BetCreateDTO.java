package p2pbet.api.dto;
public class BetCreateDTO {
    private int creatorId;
    private int eventId;
    private double stake;
    private String creatorSelection;
    public BetCreateDTO() {}
    public BetCreateDTO(int creatorId, int eventId, double stake, String creatorSelection) {
        this.creatorId = creatorId;
        this.eventId = eventId;
        this.stake = stake;
        this.creatorSelection = creatorSelection;
    }
    public int getCreatorId() { return creatorId; }
    public int getEventId() { return eventId; }
    public double getStake() { return stake; }
    public String getCreatorSelection() { return creatorSelection; }
}
