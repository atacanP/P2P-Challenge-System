package p2pbet.api.dto;
public class EventResultDTO {
    private int eventId;
    private String finalResult;
    public EventResultDTO() {}
    public EventResultDTO(int eventId, String finalResult) {
        this.eventId = eventId;
        this.finalResult = finalResult;
    }
    public int getEventId() { return eventId; }
    public String getFinalResult() { return finalResult; }
}
