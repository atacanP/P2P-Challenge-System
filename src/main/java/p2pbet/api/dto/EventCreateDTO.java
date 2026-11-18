package p2pbet.api.dto;
public class EventCreateDTO {
    private String description;
    private String optionA;
    private String optionB;
    public EventCreateDTO() {}
    public EventCreateDTO(String description, String optionA, String optionB) {
        this.description = description;
        this.optionA = optionA;
        this.optionB = optionB;
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
}
