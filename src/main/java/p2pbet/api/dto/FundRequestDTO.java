package p2pbet.api.dto;
public class FundRequestDTO {
    private int userId;
    private double amount;
    public FundRequestDTO() {}
    public FundRequestDTO(int userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }
    public int getUserId() { return userId; }
    public double getAmount() { return amount; }
}
