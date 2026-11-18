package p2pbet.api.dto;
public class MatchBetDTO {
    private int betId;
    private int acceptorId;
    public MatchBetDTO() {}
    public MatchBetDTO(int betId, int acceptorId) {
        this.betId = betId;
        this.acceptorId = acceptorId;
    }
    public int getBetId() { return betId; }
    public int getAcceptorId() { return acceptorId; }
}
