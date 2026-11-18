package p2pbet.core.entities.state;
import p2pbet.core.entities.Bet;
public class CancelledState implements BetState {
    @Override
    public void accept(Bet bet, int acceptorId, int expectedVersion) {
        throw new IllegalStateException("Bet is cancelled.");
    }
    @Override
    public void cancel(Bet bet) {
        throw new IllegalStateException("Bet is already cancelled.");
    }
    @Override
    public void settle(Bet bet) {
        throw new IllegalStateException("Cannot settle a cancelled bet.");
    }
    @Override
    public BetStatus getStatus() {
        return BetStatus.CANCELLED;
    }
}
