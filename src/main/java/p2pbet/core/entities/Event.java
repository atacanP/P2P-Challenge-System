package p2pbet.core.entities;
public class Event {
    private int id;
    private String description;
    private String optionA;
    private String optionB;
    private String finalResult;
    private EventStatus status;
    private String createdAt;
    public enum EventStatus {
        UPCOMING,
        IN_PROGRESS,
        FINISHED
    }
    public Event(int id, String description, String optionA, String optionB, EventStatus status) {
        this.id = id;
        this.description = description;
        this.optionA = optionA;
        this.optionB = optionB;
        this.status = status;
        this.createdAt = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    public Event(int id, String description, String optionA, String optionB, String finalResult, EventStatus status, String createdAt) {
        this.id = id;
        this.description = description;
        this.optionA = optionA;
        this.optionB = optionB;
        this.finalResult = finalResult;
        this.status = status;
        this.createdAt = createdAt;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public int getId() {
        return id;
    }
    public String getDescription() {
        return description;
    }
    public String getOptionA() {
        return optionA;
    }
    public String getOptionB() {
        return optionB;
    }
    public String getFinalResult() {
        return finalResult;
    }
    public EventStatus getStatus() {
        return status;
    }
    public void finishEvent(String finalResult) {
        this.finalResult = finalResult;
        this.status = EventStatus.FINISHED;
    }
    public void startEvent() {
        if (this.status != EventStatus.UPCOMING) {
            throw new IllegalStateException("Event can only be started if it is UPCOMING.");
        }
        this.status = EventStatus.IN_PROGRESS;
    }
    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", optionA='" + optionA + '\'' +
                ", optionB='" + optionB + '\'' +
                ", finalResult='" + finalResult + '\'' +
                ", status=" + status +
                '}';
    }
}
