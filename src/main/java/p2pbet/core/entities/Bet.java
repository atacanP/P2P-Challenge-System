package p2pbet.core.entities;
import p2pbet.core.entities.state.BetState;
import p2pbet.core.entities.state.BetStatus;
import p2pbet.core.entities.state.PendingState;
import p2pbet.core.entities.state.MatchedState;
import p2pbet.core.entities.state.SettledState;
import p2pbet.core.entities.state.CancelledState;
public class Bet {
    private int id;
    private int creatorId;
    private int acceptorId;
    private int eventId;
    private double stake;
    private String creatorSelection;
    private int version;
    private transient BetState state;
    private BetStatus status;
    private String createdAt;
    public Bet(int id, int creatorId, int eventId, double stake, String creatorSelection) {
        this.id = id;
        this.creatorId = creatorId;
        this.eventId = eventId;
        this.stake = stake;
        this.creatorSelection = creatorSelection;
        this.version = 1;
        this.status = BetStatus.PENDING;
        this.state = new PendingState();
        this.createdAt = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    public Bet(int id, int creatorId, int acceptorId, int eventId, double stake, String creatorSelection, int version, BetStatus status, String createdAt) {
        this.id = id;
        this.creatorId = creatorId;
        this.acceptorId = acceptorId;
        this.eventId = eventId;
        this.stake = stake;
        this.creatorSelection = creatorSelection;
        this.version = version;
        this.status = status;
        this.createdAt = createdAt;
        switch (status) {
            case PENDING: this.state = new PendingState(); break;
            case MATCHED: this.state = new MatchedState(); break;
            case SETTLED: this.state = new SettledState(); break;
            case CANCELLED: this.state = new CancelledState(); break;
        }
    }
    public void accept(int acceptorId, int expectedVersion) {
        this.state.accept(this, acceptorId, expectedVersion);
    }
    public void cancel() {
        this.state.cancel(this);
    }
    public void settle() {
        this.state.settle(this);
    }
    public int getId() { return id; }
    public int getCreatorId() { return creatorId; }
    public int getAcceptorId() { return acceptorId; }
    public void setAcceptorId(int acceptorId) { this.acceptorId = acceptorId; }
    public int getEventId() { return eventId; }
    public double getStake() { return stake; }
    public String getCreatorSelection() { return creatorSelection; }
    public int getVersion() { return version; }
    public void incrementVersion() { this.version++; }
    public boolean isMatchable(int candidateId) {
        return this.creatorId != candidateId && this.status == BetStatus.PENDING;
    }
    public BetStatus getStatus() { return status; }
    public void setState(BetState newState) { 
        this.state = newState; 
        this.status = newState.getStatus();
    }
    public String getCreatedAt() { return createdAt; }
    @Override
    public String toString() {
        return "Bet{" +
                "id=" + id +
                ", creatorId=" + creatorId +
                ", acceptorId=" + acceptorId +
                ", eventId=" + eventId +
                ", stake=" + stake +
                ", creatorSelection='" + creatorSelection + '\'' +
                ", version=" + version +
                ", status=" + status +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
